package com.example.locker;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "main_activity";
    public static String hash, salt;
    public static final int MIN_PASS_SIZE = 6;

    private ProgressBar progressBar;
    private LinearLayout setPassLayout, verifyPassLayout, loginLayout;
    private TextView msgSetPass, msgVerPass, errMsgSetPass, errMsgVerPass, msgLogin;
    private EditText setPassMain, setPassRe, verPass;
    private Button setPassBtn, verifyBtn, loginBtn;

    // See: https://developer.android.com/training/basics/intents/result
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    onSignInResult(result);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar)findViewById(R.id.mainProgressBar);
        setPassLayout = (LinearLayout) findViewById(R.id.setPasswordLayout);
        verifyPassLayout = (LinearLayout) findViewById(R.id.verifyPasswordLayout);
        loginLayout = (LinearLayout) findViewById(R.id.loginLayout);

        msgLogin = (TextView) findViewById(R.id.msgLogin);
        msgSetPass = (TextView) findViewById(R.id.msgSetPass);
        msgVerPass = (TextView) findViewById(R.id.msgVerifyPass);
        errMsgSetPass = (TextView) findViewById(R.id.errMsgSetPass);
        errMsgVerPass = (TextView) findViewById(R.id.errMsgVer);

        setPassMain = (EditText) findViewById(R.id.main_password);
        setPassRe = (EditText) findViewById(R.id.retype_password);
        verPass = (EditText) findViewById(R.id.password_verify);

        setPassBtn = (Button) findViewById(R.id.btnSetPass);
        verifyBtn = (Button) findViewById(R.id.btnVerify);
        loginBtn = (Button) findViewById(R.id.btnLogin);

        msgSetPass.setText("Set a strong password, You will need this everytime you want to access your encrypted data");
        msgVerPass.setText("Please Enter your password to decrypt the data");
        msgLogin.setText("Welcome to our LOCKER app\nPress login/register button to login using email and password\nWe are not using Google Sign in, your emails on the device will be shown for convenience, you can choose None Of The Above to use other email");

        setPassBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPassword();
            }
        });

        verifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPassword();
            }
        });
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseSignIn();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        showProgressBar();
        if(FirebaseAuth.getInstance().getCurrentUser() == null){
            showLoginScreen();
        }else{
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            DataHelper.UID = user.getUid();
            DataHelper.EMAIL = user.getEmail();
            securityPasswordAuthentication();
        }
    }

    private void firebaseSignIn(){
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build());

        // Create and launch sign-in intent
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build();
        signInLauncher.launch(signInIntent);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            DataHelper.UID = user.getUid();
            DataHelper.EMAIL = user.getEmail();
            securityPasswordAuthentication();
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            Toast.makeText(this, "Sign in Failed", Toast.LENGTH_SHORT).show();
        }
    }

    // user is authenticated
    // verify their encryption password
    private void securityPasswordAuthentication(){
        showProgressBar();
        setTitle("Locker ("+DataHelper.EMAIL+")");
        DataHelper.firebaseDB.collection(DataHelper.usersCollection)
                .document(DataHelper.UID)
                .collection(DataHelper.credCollection)
                .document(DataHelper.credDocID).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            DocumentSnapshot docSnap = task.getResult();
                            if(docSnap.exists()) {
                                hash = (String) docSnap.get("hash");
                                salt = (String) docSnap.get("salt");
                                showVerPassScreen();
                            }else {
                                showSetPassScreen();
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Toast.makeText(MainActivity.this, "Fetching password hash failed\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
                        showLoginScreen();
                    }
                });
    }

    private void verifyPassword(){
        String pass = verPass.getText().toString();
        try {
            if(DataHelper.verifyHash(pass, salt, hash)){
                Document.PASS_HASH = DataHelper.getOneTimeHash(pass, salt);
                Intent i = new Intent(getApplicationContext(), HomeActivity.class);
                startActivity(i);
            }else{
                errMsgVerPass.setText("Password is incorrect");
            }
        }catch (Exception e){
            Log.d(TAG, "onComplete: "+e.getMessage());
        }
    }

    private void setPassword(){
        String pass1 = setPassMain.getText().toString();
        String pass2 = setPassRe.getText().toString();

        if(pass1.equals(pass2)){
            if(pass1.length() >= MIN_PASS_SIZE){
                try {
                    salt = DataHelper.generateSalt();
                    hash = DataHelper.getHashWithSalt(pass1, salt);
                    Map<String, Object> data = new HashMap<>();
                    data.put("salt", salt);
                    data.put("hash", hash);
                    showProgressBar();
                    DataHelper.firebaseDB.collection(DataHelper.usersCollection)
                            .document(DataHelper.UID)
                            .collection(DataHelper.credCollection)
                            .document(DataHelper.credDocID).set(data)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull @NotNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        showSetPassScreen();
                                        try {
                                            Document.PASS_HASH = DataHelper.getOneTimeHash(pass1, salt);
                                            Intent i = new Intent(getApplicationContext(), HomeActivity.class);
                                            startActivity(i);
                                        }catch (Exception e){
                                            Log.d(TAG, "onComplete: "+e.getMessage());
                                        }
                                    }else{
                                        Toast.makeText(MainActivity.this, "Some Error Occurred", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }catch (Exception e){
                    Log.d(TAG, "setPassword: "+e.getMessage());
                }
            }else{
                errMsgSetPass.setText("length must be >= "+MIN_PASS_SIZE);
            }
        }else{
            errMsgSetPass.setText("Passwords mismatch");
        }
    }

    private void showProgressBar(){
        progressBar.setVisibility(View.VISIBLE);
        setPassLayout.setVisibility(View.GONE);
        verifyPassLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.GONE);
    }
    private void showSetPassScreen(){
        progressBar.setVisibility(View.GONE);
        setPassLayout.setVisibility(View.VISIBLE);
        verifyPassLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.GONE);
    }
    private void showVerPassScreen(){
        progressBar.setVisibility(View.GONE);
        setPassLayout.setVisibility(View.GONE);
        verifyPassLayout.setVisibility(View.VISIBLE);
        loginLayout.setVisibility(View.GONE);
    }
    private void showLoginScreen(){
        setTitle("Locker");
        progressBar.setVisibility(View.GONE);
        setPassLayout.setVisibility(View.GONE);
        verifyPassLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.VISIBLE);
    }
}