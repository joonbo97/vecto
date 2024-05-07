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
import com.vecto_example.vecto.databinding.WritecourseItemBinding

class MyWriteCourseAdapter(): RecyclerView.Adapter<MyWriteCourseAdapter.ViewHolder>() {
    var myVisit = mutableListOf<VisitData>()
    var cnt = 0
    var idx_first = -1
    var idx_second = -1
    private val selectedItems = mutableListOf<VisitData>()
    var onItemsSelectedListener: ((List<VisitData>) -> Unit)? = null

    init {
        selectedItems.clear() // 초기 상태에서 선택된 아이템 목록을 비워줍니다.
    }

    inner class ViewHolder(val binding: WritecourseItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VisitData) {

            when(adapterPosition % 4){
                0 -> binding.visitItemIcon.setImageResource(R.drawable.edit_course_circle_mint)
                1 -> binding.visitItemIcon.setImageResource(R.drawable.edit_course_circle_blue)
                2 -> binding.visitItemIcon.setImageResource(R.drawable.edit_course_circle_yellow)
                3 -> binding.visitItemIcon.setImageResource(R.drawable.edit_course_circle_pink)
            }

            binding.visitTitleText.text = item.name

            if(idx_first != -1) {
                if (adapterPosition in idx_first..idx_second) {   //선택 데이터강조
                    binding.highlightImage.setImageResource(R.color.edit_course_highlight)
                } else {
                    binding.highlightImage.setImageResource(R.color.alpha)
                }
            }

            binding.highlightImage.setOnClickListener {
                if(cnt == 0)//처음 터치한 경우
                {
                    if(myVisit.size == 1) {
                        selectedItems.add(myVisit[adapterPosition])
                        onItemsSelectedListener?.invoke(selectedItems)
                    }
                    else {
                        idx_first = adapterPosition
                        idx_second = adapterPosition

                        notifyItemChanged(idx_first)
                    }
                    cnt++
                }
                else if(cnt == 1 && idx_first <= adapterPosition)//두번째 터치인 경우, 이후의 아이템을 선택한 경우
                {
                    idx_second = adapterPosition

                    notifyItemRangeChanged(idx_first, idx_second)

                    for(i in idx_first until idx_second + 1)
                        selectedItems.add(myVisit[i])
                    onItemsSelectedListener?.invoke(selectedItems)
                }
            }

        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val visitData = myVisit[position]
        holder.bind(visitData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = WritecourseItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return myVisit.size
    }

}