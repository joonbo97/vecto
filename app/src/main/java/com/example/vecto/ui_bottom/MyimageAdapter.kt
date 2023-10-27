package com.example.vecto.ui_bottom

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.vecto.R

class MyimageAdapter (private val context: Context): RecyclerView.Adapter<MyimageAdapter.ViewHolder>(){
    val imageUri = mutableListOf<Uri>()

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val imageView: ImageView = view.findViewById(R.id.ImageItem)
        val deleteButton: ImageView = view.findViewById(R.id.DeleteButton)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imageUri.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = imageUri[position]
        holder.imageView.setImageURI(uri)

        holder.deleteButton.setOnClickListener {
            removeItem(position)
        }
    }

    private fun removeItem(position: Int) {
        if (position >= 0 && position < imageUri.size) {
            imageUri.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}