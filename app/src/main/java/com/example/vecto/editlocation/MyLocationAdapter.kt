package com.example.vecto.editlocation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vecto.R
import com.example.vecto.data.PathData
import com.example.vecto.data.VisitData

@Suppress("IMPLICIT_CAST_TO_ANY")
class MyLocationAdapter(private val context: Context, private val itemClickListener: OnItemClickListener): RecyclerView.Adapter<MyLocationAdapter.ViewHolder>(){

    var visitdata = mutableListOf<VisitData>()
    var pathdata = mutableListOf<PathData>()


    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener{
        val imageView: ImageView = view.findViewById(R.id.VisitImageView)

        init{
            view.setOnClickListener(this)
        }

        fun bindvisit(item: VisitData)
        {
            if(adapterPosition == 0)
            {
                if(item.name.isNotEmpty())
                {
                    imageView.setImageResource(R.drawable.location_visit_first_green)
                }
                else
                {
                    imageView.setImageResource(R.drawable.location_visit_first_red)
                }
            }
            else if(adapterPosition == visitdata.size + pathdata.size - 1)
            {
                if(item.name.isNotEmpty())
                {
                    imageView.setImageResource(R.drawable.location_visit_last_green)
                }
                else
                {
                    imageView.setImageResource(R.drawable.location_visit_last_red)

                }
            }
            else
            {
                if(item.name.isNotEmpty())
                {
                    imageView.setImageResource(R.drawable.location_visit_middle_green)

                }
                else
                {
                    imageView.setImageResource(R.drawable.location_visit_middle_red)
                }
            }
        }


        fun bindlocation(item: PathData)
        {
            imageView.setImageResource(R.drawable.location_location)
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item =
                    if (position % 2 == 0)
                        visitdata[position / 2]
                    else
                        pathdata[position / 2]
                itemClickListener.onItemClick(item)
            }
        }

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {


        val view = LayoutInflater.from(context).inflate(R.layout.edit_course, parent, false)
        return ViewHolder(view)

    }

    override fun getItemCount(): Int {
        return visitdata.size + pathdata.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when(position % 2) {
            0 -> holder.bindvisit(visitdata[position/2])
            1 -> holder.bindlocation(pathdata[position/2])
        }
    }

    interface OnItemClickListener {
        fun onItemClick(data: Any)
    }


}