package com.vecto_example.vecto

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.model.data.VisitData

class VisitNumberAdapter(private val context: Context): RecyclerView.Adapter<VisitNumberAdapter.ViewHolder>() {
    val visitdataList = mutableListOf<VisitData>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val numberImage: ImageView = view.findViewById(R.id.NumberImage)
        val titleText: TextView = view.findViewById(R.id.TitleText)


    }

    override fun onBindViewHolder(holder: VisitNumberAdapter.ViewHolder, position: Int) {
        holder.numberImage.setImageResource(setImage(position))
        holder.titleText.text = visitdataList[position].name
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitNumberAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.post_number_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return visitdataList.size
    }

    private fun setImage(position: Int): Int {
        when(position)
        {
            0 -> return R.drawable.course_number_1
            1 -> return R.drawable.course_number_2
            2 -> return R.drawable.course_number_3
            3 -> return R.drawable.course_number_4
            4 -> return R.drawable.course_number_5
            5 -> return R.drawable.course_number_6
            6 -> return R.drawable.course_number_7
            7 -> return R.drawable.course_number_8
            8 -> return R.drawable.course_number_9

            else -> return R.drawable.course_number_1
        }
    }
}