package com.example.locker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static android.content.ContentValues.TAG;

public class HomeActivity extends AppCompatActivity {
    public static int lastClickedPosition;
    private RecyclerView recyclerView;
    private MyAdapter adapter;
    private ProgressBar progress;
    private LinearLayout recyclerL, changePassL;
    private Button changePassBtn, cancelBtn;
    private TextView errMsg;
    private EditText oldPass, newPass1, newPass2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        progress = (ProgressBar)findViewById(R.id.progressBar);
        recyclerView = (RecyclerView)findViewById(R.id.recyclerView);
        recyclerL = (LinearLayout) findViewById(R.id.recyclerLayout);
        changePassL = (LinearLayout) findViewById(R.id.changePassLayout);

        changePassBtn = (Button) findViewById(R.id.changePassBtn);
        cancelBtn = (Button) findViewById(R.id.cancelBtn);
        oldPass = (EditText) findViewById(R.id.oldPass);
        newPass1 = (EditText) findViewById(R.id.newPass1);
        newPass2 = (EditText) findViewById(R.id.newPass2);
        errMsg = (TextView) findViewById(R.id.tvErrMsg);

        setTitle("Locker ("+DataHelper.EMAIL+")");

        lastClickedPosition = RecyclerView.NO_POSITION;

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecyclerScreen();
            }
        });
        changePassBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        // enable the progress bar
        showProgressBar();

        if( DataHelper.init(new DataHelper.MyCallback() {
                @Override
                public void execute() {
                    adapter.notifyDataSetChanged();
                    showRecyclerScreen();
                }
            }) == DataHelper.DONE){

            showRecyclerScreen();
        }

        ArrayList<Document> docList = DataHelper.getDocList();

        // create adapter
        adapter = new MyAdapter(docList, HomeActivity.this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(HomeActivity.this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(lastClickedPosition != RecyclerView.NO_POSITION){
            adapter.notifyItemChanged(lastClickedPosition);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    private void changePassword(){
        String oldPassword = oldPass.getText().toString();
        String newPassword1 = newPass1.getText().toString();
        String newPassword2 = newPass2.getText().toString();
        try {
            if(DataHelper.verifyHash(oldPassword, MainActivity.salt, MainActivity.hash)){
//                Document.PASS_HASH = DataHelper.getOneTimeHash(pass, salt);
                if(newPassword1.equals(newPassword2)){
                    if(newPassword1.length() >= MainActivity.MIN_PASS_SIZE){
                        try {
                            MainActivity.salt = DataHelper.generateSalt();
                            MainActivity.hash = DataHelper.getHashWithSalt(newPassword1, MainActivity.salt);
                            Map<String, Object> data = new HashMap<>();
                            data.put("salt", MainActivity.salt);
                            data.put("hash", MainActivity.hash);

                            showProgressBar();
                            DataHelper.firebaseDB.collection(DataHelper.usersCollection)
                                    .document(DataHelper.UID)
                                    .collection(DataHelper.credCollection)
                                    .document(DataHelper.credDocID).set(data)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull @NotNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                try {
                                                    Document.PASS_HASH = DataHelper.getOneTimeHash(newPassword1, MainActivity.salt);
                                                    ArrayList<Document> docList = DataHelper.getDocList();
                                                    for(Document doc : docList){
                                                        doc.syncAllData();
                                                    }
                                                    showRecyclerScreen();
                                                    Toast.makeText(HomeActivity.this, "Password changed Successfully", Toast.LENGTH_SHORT).show();
                                                }catch (Exception e){
                                                    Log.d(TAG, "onComplete: "+e.getMessage());
                                                }
                                            }
                                        }
                                    });
                        }catch (Exception e){
                            Log.d(TAG, "changePassword: "+e.getMessage());
                        }
                    }else{
                        errMsg.setText("length must be >= "+MainActivity.MIN_PASS_SIZE);
                    }
                }else{
                    errMsg.setText("Passwords mismatch");
                }
            }else{
                errMsg.setText("Current Password is incorrect");
            }
        }catch (Exception e){
            Log.d(TAG, "changePassword: "+e.getMessage());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.addBtn:
                View view = View.inflate(this, R.layout.title_alert, null);

                final EditText newTitle = (EditText) view.findViewById(R.id.newTitle);
                newTitle.setHint("Title");

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Title")
                        .setView(view)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Document doc = new Document(newTitle.getText().toString());
                                adapter.notifyItemInserted(DataHelper.addDocument(doc));
                            }
                        })
                        .setNegativeButton("Cancel", null);
                // Create the AlertDialog object and return it
                builder.create().show();
                return true;
            case R.id.logoutBtn:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(HomeActivity.this, "Logged Out", Toast.LENGTH_SHORT).show();
                                DataHelper.clearDocs();
                                finish();
                            }
                        });
                return true;
            case R.id.changePassBtn:
                showChangePassScreen();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showProgressBar(){
        progress.setVisibility(View.VISIBLE);
        recyclerL.setVisibility(View.GONE);
        changePassL.setVisibility(View.GONE);

    }
    private void showRecyclerScreen(){
        progress.setVisibility(View.GONE);
        recyclerL.setVisibility(View.VISIBLE);
        changePassL.setVisibility(View.GONE);
    }
    private void showChangePassScreen(){
        progress.setVisibility(View.GONE);
        recyclerL.setVisibility(View.GONE);
        changePassL.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}