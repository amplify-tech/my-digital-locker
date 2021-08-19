package com.example.locker;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private ArrayList<Document> docs;
    private final Context context;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView title, created, modified;

        public ViewHolder(@NonNull @org.jetbrains.annotations.NotNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            created = itemView.findViewById(R.id.created);
            modified = itemView.findViewById(R.id.modified);
            itemView.setOnClickListener(this);
        }
        // Handles the row being being clicked
        @Override
        public void onClick(View view) {
            int position = getAbsoluteAdapterPosition(); // gets item position
            HomeActivity.lastClickedPosition = position;
            if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                Intent i = new Intent(context, View_doc.class);
                i.putExtra("index", position);
                i.putExtra("version", -1);
                context.startActivity(i);
            }
        }
    }

    public MyAdapter(ArrayList<Document> docs, Context context){
        this.docs = docs;
        this.context = context;
    }

    @NonNull
    @org.jetbrains.annotations.NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @org.jetbrains.annotations.NotNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View docView = inflater.inflate(R.layout.recycler_card, parent, false);
        return new ViewHolder(docView);
    }

    @Override
    public void onBindViewHolder(@NonNull @org.jetbrains.annotations.NotNull MyAdapter.ViewHolder holder, int position) {
        Document doc = docs.get(position);

        // Set item views based on your views and data model
        TextView tvTitle = holder.title;
        tvTitle.setText(doc.getTitle());

        TextView tvModified = holder.modified;
        tvModified.setText("modified: "+doc.getModified().toString());

        TextView tvCreated = holder.created;
        tvCreated.setText("created: "+doc.getCreated().toString());

    }

    @Override
    public int getItemCount() {
        return docs.size();
    }
}
