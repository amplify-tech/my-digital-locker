package com.example.locker;

import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Key;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static android.content.ContentValues.TAG;
import static com.google.common.primitives.Bytes.concat;

public class Document{
//    private static int x;
    private static final String ALGORITHM = "AES/CBC/PKCS5PADDING";
    private static final String CREATED = "created";
    private static final String MODIFIED = "modified";
    private static final String HEAD = "head";
    private static final String SNAPS = "snaps";
    private static final String TITLE = "title";

    private static final String DATA = "data";
    private static final String PARENT = "parent";
    private static final String DATE = "date";

    public static byte[] PASS_HASH = null;
//    private final String rootCollection;
    private String id;
    private String title;
    private int head;
    private Date created, modified;
    private ArrayList<JSONObject> snaps;
    private final CollectionReference dbDocsRef = DataHelper.firebaseDB.collection(DataHelper.usersCollection)
            .document(DataHelper.UID).collection(DataHelper.docCollection);

    // constructor to create new document
    Document(String title){
        this.title = title;
        this.head = 0;
        this.created = Calendar.getInstance().getTime();
        this.modified = this.created;
        snaps = new ArrayList<JSONObject>();
        try {
            JSONObject snap = new JSONObject();
            snap.put(DATA, "The Initial value of the document");
            snap.put(PARENT, -1);
            snap.put(DATE, DataHelper.dateFormat.format(created));
            snaps.add(snap);
        }catch (JSONException e){
            Log.d("", "Document: "+e.getMessage());
        }
        // Store in cloud fireStore
        Map<String, Object> data = new HashMap<>();
        try {
            data.put(TITLE, encrypt(this.title));
            data.put(HEAD, encrypt(String.valueOf(this.head)));
            data.put(CREATED, encrypt(String.valueOf(this.created.getTime())));
            data.put(MODIFIED, encrypt(String.valueOf(this.modified.getTime())));
            data.put(SNAPS, JSONArrayTOStringArray(snaps));
        }catch (Exception e){
            Log.d(TAG, "Document: "+e.getMessage());
        }
        dbDocsRef.add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        id = documentReference.getId();
                        Log.d(TAG, "onSuccess: added to the fireStore database");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Log.d(TAG, "onFailure: Error adding document to database");
                    }
                });
    }

    Document(Map<String,Object> data, String id){
        this.id = id;
        try {
            this.title = decrypt((String)data.get(TITLE));
            this.head = Integer.parseInt(decrypt((String)data.get(HEAD)));
            this.created = new Date(Long.parseLong(decrypt((String)data.get(CREATED))));
            this.modified = new Date(Long.parseLong(decrypt((String)data.get(MODIFIED))));
            ArrayList<String> arr = (ArrayList<String>) data.get(SNAPS);
            this.snaps = StringArrayTOJSONArray(arr);
        }catch (Exception e){
            Log.d(TAG, "Document: "+e.getMessage());
        }
    }

//    getters
    public String getID(){ return id; }
    public int getHead(){ return head; }
    public String getTitle(){ return title; }
    public int getSnapSize(){ return snaps.size(); }
    public String getCreated(){ return DataHelper.dateFormatSmall.format(created); }
    public String getModified(){ return DataHelper.dateFormatSmall.format(modified); }

    public String getData(int index){
        try {
            return snaps.get(index).getString(DATA);
        }catch (JSONException e){
            Log.d("", "getData: "+e.getMessage());
        }
        return "Exception Occurred";
    }

    public int getParent(int index){
        try {
            return snaps.get(index).getInt(PARENT);
        }catch (JSONException e){
            Log.d("", "getData: "+e.getMessage());
        }
        return -1;
    }
    public String getSnapDate(int index){
        try {
            return snaps.get(index).getString(DATE);
        }catch (JSONException e){
            Log.d("", "getData: "+e.getMessage());
        }
        return null;
    }

//    setters
    public void setHead(int newHead){
        head = newHead;
        syncMetaData();
    }
    public void setTitle(String newTitle){
        this.title = newTitle;
        syncMetaData();
    }
    public void markModified(){
        this.modified = Calendar.getInstance().getTime();
        syncMetaData();
    }

//    save new snapshot, update head, update modified date
    boolean save(String newVal, int parent){
        JSONObject newSnap = new JSONObject();
        try {
            Date curDate = Calendar.getInstance().getTime();
            newSnap.put(DATA, newVal);
            newSnap.put(PARENT, parent);
            newSnap.put(DATE, DataHelper.dateFormat.format(curDate));
            modified = curDate;
            head = snaps.size();
            snaps.add(newSnap);
            syncAllData();
            return true;
        }catch (JSONException e){
            Log.d("", "getSnapshots: "+e.getMessage());
            return false;
        }
    }

    void syncAllData(){
        syncMetaData();
        syncSnapshots();
    }
    // sync snapshots
    void syncSnapshots(){
        dbDocsRef.document(id).update(SNAPS, JSONArrayTOStringArray(snaps))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<Void> task) {
                        Log.d(TAG, "onComplete: snapshots synced");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Log.d(TAG, "onFailure: snapshots sync failed");
                    }
                });
    }

    // sync title,head,modified
    void syncMetaData(){
        Map<String, Object> data = new HashMap<>();
        try{
            data.put(TITLE, encrypt(this.title));
            data.put(HEAD, encrypt(String.valueOf(this.head)));
            data.put(MODIFIED, encrypt(String.valueOf(this.modified.getTime())));
            data.put(CREATED, encrypt(String.valueOf(this.created.getTime())));
        }catch (Exception e){
            Log.d(TAG, "syncMetaData: "+e.getMessage());
        }

        dbDocsRef.document(id).update(data)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<Void> task) {
                        Log.d(TAG, "onComplete: metadata synced");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Log.d(TAG, "onFailure: metadata sync failed");
                    }
                });
    }

    ArrayList<String> JSONArrayTOStringArray(ArrayList<JSONObject> array){
        ArrayList<String> newarr = new ArrayList<>();
        try {
            for (int i = 0; i < array.size(); i++) {
                newarr.add(encrypt(array.get(i).toString()));
            }
        }catch (Exception e){
            Log.d(TAG, "JSONArrayTOStringArray: "+e.getMessage());
        }
        return newarr;
    }

    ArrayList<JSONObject> StringArrayTOJSONArray(ArrayList<String> array){
        ArrayList<JSONObject> newarr = new ArrayList<>();
        try {
            for(int i=0; i<array.size(); i++){
                newarr.add(new JSONObject(decrypt(array.get(i))));
            }
        }catch (Exception e){
            Log.d(TAG, "StringArrayTOJSONArray: "+e.getMessage());
        }
        return newarr;
    }

    // ========================================================
    //          Encryption and decryption functions
    // ========================================================
    // see this article to know about IV
    // https://doridori.github.io/Android-Security-Beware-of-the-default-IV/
    public static String encrypt(String value) throws Exception
    {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(Document.ALGORITHM);
        byte[] iv = new byte[cipher.getBlockSize()];
        new SecureRandom().nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte [] encryptedByteValue = cipher.doFinal(value.getBytes("utf-8"));
        return Base64.encodeToString(concat(iv, encryptedByteValue), Base64.DEFAULT);
    }

    public static String decrypt(String value) throws Exception
    {
        byte[] decryptedValue64 = Base64.decode(value, Base64.DEFAULT);
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(Document.ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(decryptedValue64, 0, cipher.getBlockSize()));
        byte [] decryptedByteValue = cipher.doFinal(decryptedValue64, cipher.getBlockSize(), decryptedValue64.length-cipher.getBlockSize());
        return new String(decryptedByteValue,"utf-8");
    }

    private static Key generateKey() throws Exception
    {
        if (Document.PASS_HASH == null){
            Log.d(TAG, "generateKey: PASS_HASH is null");
        }
        return new SecretKeySpec(Document.PASS_HASH,"AES");
    }
}