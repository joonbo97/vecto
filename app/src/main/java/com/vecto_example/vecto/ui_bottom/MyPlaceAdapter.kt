package com.vecto_example.vecto.ui_bottom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R

class MyPlaceAdapter(private val context: Context, private val onItemClick: (String) -> Unit): RecyclerView.Adapter<MyPlaceAdapter.ViewHolder>() {
    val nameList = mutableListOf<String>()


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.PlaceNameText)
    }

    override fun onBindViewHolder(holder: MyPlaceAdapter.ViewHolder, position: Int) {
        holder.nameText.text = nameList[position]

        holder.nameText.setOnClickListener{
            onItemClick(nameList[position])
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPlaceAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.place_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return nameList.size
    }
}