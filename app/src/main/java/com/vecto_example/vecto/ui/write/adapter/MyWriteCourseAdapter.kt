package com.vecto_example.vecto.ui.write.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.VisitData

class MyWriteCourseAdapter(private val context: Context): RecyclerView.Adapter<MyWriteCourseAdapter.ViewHolder>() {
    var myVisit = mutableListOf<VisitData>()
    var cnt = 0
    var idx_first = 0
    var idx_second = 0
    private val selectedItems = mutableListOf<VisitData>()
    var onItemsSelectedListener: ((List<VisitData>) -> Unit)? = null

    init {
        selectedItems.clear() // 초기 상태에서 선택된 아이템 목록을 비워줍니다.
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val itemImage: ImageView = view.findViewById(R.id.itemImage)
        val itemText: TextView = view.findViewById(R.id.VisitTitleText)
    }





    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(myVisit.size == 1)
            holder.itemImage.setImageResource(R.drawable.write_course_only)
        else
        {
            if(holder.adapterPosition == 0)//처음
            {
                holder.itemImage.setImageResource(R.drawable.write_course_top)
            }
            else if(holder.adapterPosition == myVisit.size - 1)//마지막
            {
                holder.itemImage.setImageResource(R.drawable.write_course_bottom)
            }
            else//중간
            {
                holder.itemImage.setImageResource(R.drawable.write_course_middle)
            }
        }

        holder.itemText.text = myVisit[position].name

        holder.itemView.setOnClickListener {
            if(cnt == 0)//처음 터치한 경우
            {
                if(myVisit.size == 1) {
                    holder.itemImage.setImageResource(R.drawable.write_course_only_all)

                    selectedItems.add(myVisit[position])
                    onItemsSelectedListener?.invoke(selectedItems)
                }
                else {
                    if (holder.adapterPosition == 0)//처음
                    {
                        holder.itemImage.setImageResource(R.drawable.write_course_top_top)
                    }
                    else if (holder.adapterPosition == myVisit.size - 1)//마지막
                    {
                        holder.itemImage.setImageResource(R.drawable.write_course_bottom_top)
                    }
                    else//중간
                    {
                        holder.itemImage.setImageResource(R.drawable.write_course_middle_top)
                    }

                    idx_first = position
                }
                cnt++
            }
            else if(cnt == 1 && idx_first <= position)//두번째 터치인 경우, 이후의 아이템을 선택한 경우
            {
                if (holder.adapterPosition == 0)//처음
                {
                    holder.itemImage.setImageResource(R.drawable.write_course_top_bottom)
                }
                else if (holder.adapterPosition == myVisit.size - 1)//마지막
                {
                    holder.itemImage.setImageResource(R.drawable.write_course_bottom_bottom)
                }
                else//중간
                {
                    holder.itemImage.setImageResource(R.drawable.write_course_middle_bottom)
                }


                idx_second = position
                for(i in idx_first until idx_second + 1)
                    selectedItems.add(myVisit[i])
                onItemsSelectedListener?.invoke(selectedItems)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.writecourse_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return myVisit.size
    }

}