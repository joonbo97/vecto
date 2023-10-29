package com.example.vecto.ui_bottom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vecto.R
import com.example.vecto.data.PathData
import com.example.vecto.data.VisitData
import java.lang.IllegalArgumentException


class MyCourseAdapter(private val context: Context, private val itemClickListener: OnItemClickListener): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var visitdata = mutableListOf<VisitData>()
    var pathdata = mutableListOf<PathData>()
    private var selectedPosition = -1


    inner class VisitViewHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener{
        val imageView: ImageView = view.findViewById(R.id.VisitImageView)
        val title: TextView = view.findViewById(R.id.VisitTitleText)
        val time: TextView = view.findViewById(R.id.VisitTimeText)

        val boximageView: ImageView = view.findViewById(R.id.VisitDiscriptionBoxImage)

        init{
            view.setOnClickListener(this)
        }

        fun bindvisit(item: VisitData)
        {
            if(adapterPosition == 0)
            {
                if(item.name.isNotEmpty())
                {
                    if(visitdata.size == 1) {
                        imageView.setImageResource(R.drawable.course_visit_only_on)
                        title.text = item.name
                        time.text = getStayTime(item.staytime)
                    }
                    else {
                        imageView.setImageResource(R.drawable.course_visit_top_on)
                        title.text = item.name
                        time.text = getStayTime(item.staytime)
                    }
                }
                else
                {
                    if(visitdata.size == 1) {
                        imageView.setImageResource(R.drawable.course_visit_only_off)
                        time.text = getStayTime(item.staytime)
                    }
                    else {
                        imageView.setImageResource(R.drawable.course_visit_top_off)
                        time.text = getStayTime(item.staytime)
                    }
                }
            }
            else if(adapterPosition == visitdata.size + pathdata.size - 1)
            {
                if(item.name.isNotEmpty())
                {
                    imageView.setImageResource(R.drawable.course_visit_bottom_on)
                    title.text = item.name
                    time.text = getStayTime(item.staytime)
                }
                else
                {
                    imageView.setImageResource(R.drawable.course_visit_bottom_off)
                    time.text = getStayTime(item.staytime)
                }
            }
            else
            {
                if(item.name.isNotEmpty())
                {
                    imageView.setImageResource(R.drawable.course_visit_middle_on)
                    title.text = item.name
                    time.text = getStayTime(item.staytime)
                }
                else
                {
                    imageView.setImageResource(R.drawable.course_visit_middle_off)
                    time.text = getStayTime(item.staytime)
                }
            }

            if (adapterPosition == selectedPosition) {
                boximageView.setImageResource(R.drawable.course_visitbox_selet)
            } else {
                // 나머지 아이템은 기본 이미지로 설정
                boximageView.setImageResource(R.drawable.course_visitbox)
            }
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {

                // 선택된 위치 업데이트
                val prevSelectedPosition = selectedPosition
                selectedPosition = position


                notifyItemChanged(prevSelectedPosition)
                notifyItemChanged(selectedPosition)

                val item =
                    if (position % 2 == 0)
                        visitdata[position / 2]
                    else
                        pathdata[position / 2]


                itemClickListener.onItemClick(item)
            }
        }
    }

    inner class PathViewHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener{

        val locationView: ImageView = view.findViewById(R.id.PathImageView)

        init{
            view.setOnClickListener(this)
        }

        fun bindlocation(item: PathData)
        {
            if (adapterPosition == selectedPosition) {
                locationView.setImageResource(R.drawable.course_location_select)
            } else {
                // 나머지 아이템은 기본 이미지로 설정
                locationView.setImageResource(R.drawable.course_location)
            }
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                // 선택된 위치 업데이트
                val prevSelectedPosition = selectedPosition
                selectedPosition = position

                // 이전에 강조된 visitdata의 항목과 현재 선택된 pathdata의 항목을 갱신
                notifyItemChanged(prevSelectedPosition)
                notifyItemChanged(selectedPosition)


                val item =
                    if (position % 2 == 0)
                        visitdata[position / 2]
                    else
                        pathdata[position / 2]
                itemClickListener.onItemClick(item)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            VISIT -> {
                val view = LayoutInflater.from(context).inflate(R.layout.edit_visit, parent, false)
                VisitViewHolder(view)
            }
            PATH -> {
                val view = LayoutInflater.from(context).inflate(R.layout.edit_course, parent, false)
                PathViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    override fun getItemCount(): Int {
        return visitdata.size + pathdata.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(position % 2) {
            0 -> {
                if (holder is VisitViewHolder) {
                    holder.bindvisit(visitdata[position / 2])
                }
            }
            1 -> {
                if (holder is PathViewHolder) {
                    holder.bindlocation(pathdata[position / 2])
                }
            }

        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position % 2){
            0 -> VISIT
            1 -> PATH
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    interface OnItemClickListener {
        fun onItemClick(data: Any)
    }

    private fun getStayTime(minutes: Int): String{
        if(minutes == 0)
            return ""

        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        if(hours == 0)
            return "${remainingMinutes}분"
        return "${hours}시간 ${remainingMinutes}분"
    }

    companion object {
        const val VISIT = 0
        const val PATH = 1
    }
}