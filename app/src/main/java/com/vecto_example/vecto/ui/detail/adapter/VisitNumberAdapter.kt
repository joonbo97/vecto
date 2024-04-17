package com.vecto_example.vecto.ui.detail.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.VisitData

class VisitNumberAdapter(private val context: Context): RecyclerView.Adapter<VisitNumberAdapter.ViewHolder>() {
    val visitdataList = mutableListOf<VisitData>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val visitImage: ImageView = view.findViewById(R.id.VisitImage)
        val titleText: TextView = view.findViewById(R.id.TitleText)


    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.visitImage.setImageResource(setImage(position))
        holder.titleText.text = visitdataList[position].name
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.post_number_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return visitdataList.size
    }

    private fun setImage(position: Int): Int {
        return if(visitdataList.size == 1){
            R.drawable.detail_only
        } else {
            when (position) {
                0 -> {
                    R.drawable.detail_top
                }
                visitdataList.lastIndex -> {
                    R.drawable.detail_last
                }
                else -> {
                    R.drawable.detail_mid
                }
            }
        }
    }
}