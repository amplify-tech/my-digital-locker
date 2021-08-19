package com.example.locker;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.security.auth.callback.Callback;

import static android.content.ContentValues.TAG;

/*
collection = [
	{
		title: "doc 1",
		versions: [
			{data, parent, date}
		],
		head: 0
	},
	{
		title: "doc 2",
        ...
	},
	...
]
*/

public class DataHelper {
    public static final int FETCHING=2, DONE=3;
    public static String UID, EMAIL;
    public static final String usersCollection = "users";
    public static final String docCollection = "documents";
    public static final String credCollection = "credentials";
    public static final String credDocID = "pass_hash_salt";

    private static final ArrayList<Document> docs = new ArrayList<Document>();
    public static final FirebaseFirestore firebaseDB = FirebaseFirestore.getInstance();
    public static final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    public static final DateFormat dateFormatSmall = new SimpleDateFormat("dd-MM-yy HH:mm");

    public interface MyCallback{
        void execute();
    }

    DataHelper(){}

    public static int init(MyCallback callback){
        // callback is required to stop progressBar once data is fetched
        if(docs.size() == 0){
            // calling firebase api to get list of all documents
            firebaseDB.collection(usersCollection).document(DataHelper.UID)
                    .collection("documents").get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()){
                                for(QueryDocumentSnapshot doc : task.getResult()){
                                    docs.add(new Document(doc.getData(), doc.getId()));
                                }
                            }else{
                                Log.d(TAG, "onComplete: fetching data failed");
                            }
                            callback.execute();
                        }
                    });
            return FETCHING;
        }
        return DONE;
    }

    // return the position of the new document
    public static int addDocument(Document newDoc){
        docs.add(newDoc);
        return docs.size()-1;
    }
    public static void clearDocs(){
        docs.clear();
    }

    public static ArrayList<Document> getDocList(){
        return docs;
    }


    // methods for generating salt and hashing
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[32];
        random.nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public static String getHashWithSalt(String pass, String salt) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(md.digest((pass+salt).getBytes()));
        return Base64.encodeToString(hashedBytes, Base64.DEFAULT);
    }

    public static byte[] getOneTimeHash(String pass, String salt) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest((pass+salt).getBytes());
    }

    public static boolean verifyHash(String pass, String salt, String expectedHash) throws NoSuchAlgorithmException{
        return expectedHash.equals(getHashWithSalt(pass, salt));
    }

}
