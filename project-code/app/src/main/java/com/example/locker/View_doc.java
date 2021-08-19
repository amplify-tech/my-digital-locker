package com.example.locker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.DocumentType;

import java.util.ArrayList;

import dev.bandb.graphview.graph.Node;

public class View_doc extends AppCompatActivity {

    private Button saveBtn, editBtn, cancelBtn, prevVerBtn, viewTreeBtn, makeHeadBtn, gotoCurBtn;
    private ScrollView dataScrollText, dataScrollEdit;
    private TextView version, date, dataText;
    private int curVerIndex;
    private Document doc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_doc);
        TextView title = (TextView)findViewById(R.id.titleEdit);
        EditText dataEdit = (EditText) findViewById(R.id.etData);
        dataText = (TextView)findViewById(R.id.tvData);
        date = (TextView)findViewById(R.id.date);
        version = (TextView)findViewById(R.id.version);
        dataScrollText = (ScrollView)findViewById(R.id.dataScrollText);
        dataScrollEdit = (ScrollView)findViewById(R.id.dataScrollEdit);

        saveBtn = (Button) findViewById(R.id.btnSave);
        editBtn = (Button) findViewById(R.id.btnEdit);
        cancelBtn = (Button) findViewById(R.id.btnCancel);
        prevVerBtn = (Button) findViewById(R.id.btnPrevVer);
        viewTreeBtn = (Button) findViewById(R.id.btnViewTree);
        makeHeadBtn = (Button) findViewById(R.id.btnMarkHead);
        gotoCurBtn = (Button) findViewById(R.id.btnGotoCur);

        // set to View only mode
        setViewOnlyMode();

        Bundle extras = getIntent().getExtras();
        int index = extras.getInt("index");
        int version = extras.getInt("version");

        doc = DataHelper.getDocList().get(index);
        if(version == -1){
            version = doc.getHead();
        }

        title.setText(doc.getTitle());
        // other fields will be set by setVersion function
        curVerIndex = setVersion(version);

        // ===============================================
        // click listener for title to change title
        // ===============================================
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = View.inflate(View_doc.this, R.layout.title_alert, null);

                final EditText newTitle = (EditText) view.findViewById(R.id.newTitle);
                newTitle.setText(doc.getTitle());

                AlertDialog.Builder builder = new AlertDialog.Builder(View_doc.this);
                builder.setMessage("New Title")
                        .setView(view)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                doc.setTitle(newTitle.getText().toString());
                                title.setText(doc.getTitle());
                                doc.markModified();
                            }
                        })
                        .setNegativeButton("Cancel", null);
                // Create the AlertDialog object and return it
                builder.create().show();
            }
        });

        // ===============================================
        // set click listeners to buttons
        // ===============================================
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataEdit.setText(dataText.getText());
                setEditMode();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String curVal = dataText.getText().toString();
                String newVal = dataEdit.getText().toString();
                if(curVal.equals(newVal)){
                    Toast.makeText(View_doc.this, "No change detected", Toast.LENGTH_SHORT).show();
                }else{
                    if(doc.save(newVal, curVerIndex)){
                        curVerIndex = setVersion(doc.getHead());
                        Toast.makeText(View_doc.this, "Saved", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(View_doc.this, "Some error while saving", Toast.LENGTH_SHORT).show();
                    }
                }
                setViewOnlyMode();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setViewOnlyMode();
            }
        });

        prevVerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int parent = doc.getParent(curVerIndex);
                if(parent == -1){
                    Toast.makeText(View_doc.this, "This is the initial version", Toast.LENGTH_SHORT).show();
                }else{
                    curVerIndex = setVersion(parent);
                }
            }
        });

        makeHeadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(View_doc.this);
                builder.setMessage("You are setting this version as the current version")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                doc.setHead(curVerIndex);
                                setVersion(curVerIndex);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {}
                        });
                // Create the AlertDialog object and return it
                builder.create().show();
            }
        });

        gotoCurBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                curVerIndex = setVersion(doc.getHead());
            }
        });

        viewTreeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(View_doc.this, TreeView.class);
                i.putExtra("index", index);
                startActivity(i);
            }
        });
    }

    private void setViewOnlyMode(){
        saveBtn.setVisibility(View.GONE);
        cancelBtn.setVisibility(View.GONE);
//        editBtn.setVisibility(View.VISIBLE);
        editBtn.setEnabled(true);
        prevVerBtn.setEnabled(true);
        gotoCurBtn.setEnabled(true);
        makeHeadBtn.setEnabled(true);
        viewTreeBtn.setEnabled(true);

        dataScrollEdit.setVisibility(View.INVISIBLE);
        dataScrollText.setVisibility(View.VISIBLE);
    }

    private void setEditMode(){
        saveBtn.setVisibility(View.VISIBLE);
        cancelBtn.setVisibility(View.VISIBLE);
//        editBtn.setVisibility(View.VISIBLE);
        editBtn.setEnabled(false);
        prevVerBtn.setEnabled(false);
        gotoCurBtn.setEnabled(false);
        makeHeadBtn.setEnabled(false);
        viewTreeBtn.setEnabled(false);

        dataScrollEdit.setVisibility(View.VISIBLE);
        dataScrollText.setVisibility(View.INVISIBLE);
    }

    // will set current version to given version index value
    // will set all fields as per this version
    private int setVersion(int newVer){
        if(newVer == doc.getHead()){
            version.setBackgroundColor(getResources().getColor(R.color.green));
            version.setText("Version "+newVer+" : Current");
        }else{
            version.setBackgroundColor(getResources().getColor(R.color.gray));
            version.setText("Version "+newVer);
        }
        dataText.setText(doc.getData(newVer));
        date.setText(doc.getSnapDate(newVer));
        return newVer;
    }
}