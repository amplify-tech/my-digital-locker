package com.example.locker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import dev.bandb.graphview.AbstractGraphAdapter
import dev.bandb.graphview.graph.Graph
import dev.bandb.graphview.graph.Node
import dev.bandb.graphview.layouts.tree.BuchheimWalkerConfiguration
import dev.bandb.graphview.layouts.tree.BuchheimWalkerLayoutManager
import dev.bandb.graphview.layouts.tree.TreeEdgeDecoration
import java.util.*

class TreeView : AppCompatActivity() {
    class NodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView
        init {
            textView = itemView.findViewById(R.id.nodeText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tree_view)

        val extras = intent.extras
        val index = extras!!.getInt("index")

        val doc: Document? = DataHelper.getDocList()[index]

        //==================================================================
        //      setup tree view
        //==================================================================
        val recycler = findViewById<View>(R.id.treeRecycler) as RecyclerView

        // 1. Set a layout manager of the ones described above that the RecyclerView will use.
        val configuration = BuchheimWalkerConfiguration.Builder()
            .setSiblingSeparation(100)
            .setLevelSeparation(100)
            .setSubtreeSeparation(100)
            .setOrientation(BuchheimWalkerConfiguration.ORIENTATION_TOP_BOTTOM)
            .build()
        recycler.layoutManager = BuchheimWalkerLayoutManager(this, configuration)

        // 2. Attach item decorations to draw edges
        recycler.addItemDecoration(TreeEdgeDecoration())

        // 3. Build your graph
        val graph = Graph()

        val len = doc!!.snapSize
        val head = doc!!.head
        val nodes = ArrayList<Node>()
        for (i in 0 until len) {
            nodes.add(Node("v$i"))
            val p = doc.getParent(i)
            if (p != -1) {
                graph.addEdge(nodes[p], nodes[i])
            }
        }
        // 4. You will need a simple Adapter/ViewHolder.
        // 4.1 Your Adapter class should extend from `AbstractGraphAdapter`
        var adapter = object : AbstractGraphAdapter<NodeViewHolder>() {

            // 4.2 ViewHolder should extend from `RecyclerView.ViewHolder`
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.node, parent, false)
                return NodeViewHolder(view)
            }

            override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
                holder.textView.text = getNodeData(position).toString()
                if(position == head){
                    holder.textView.setBackgroundColor(resources.getColor(R.color.green))
                }
                holder.textView.setOnClickListener(View.OnClickListener {
                    val i = Intent(this@TreeView, View_doc::class.java)
                    i.putExtra("index", index)
                    i.putExtra("version", position)
                    startActivity(i)
                })
            }
        }.apply {
            // 4.3 Submit the graph
            this.submitGraph(graph)
            recycler.adapter = this
        }
    }


}