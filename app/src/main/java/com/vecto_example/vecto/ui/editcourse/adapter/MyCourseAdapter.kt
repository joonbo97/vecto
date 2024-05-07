package com.vecto_example.vecto.ui.editcourse.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.PathData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.databinding.EditCoursePathItemBinding
import com.vecto_example.vecto.databinding.EditCourseVisitItemBinding
import com.vecto_example.vecto.utils.ServerResponse
import java.lang.IllegalArgumentException


class MyCourseAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var visitdata = mutableListOf<VisitData>()
    var pathdata = mutableListOf<PathData>()

    var selectedPosition = -1

    private var isTypeChangeFinished = true

    interface OnItemClickListener {
        fun onVisitItemClick()

        fun onPathItemClick()

        fun onPathTypeClick(type: String)
    }

    var itemClickListener: OnItemClickListener? = null

    inner class VisitViewHolder(val binding: EditCourseVisitItemBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(item: VisitData)
        {
            val visitPosition = adapterPosition / 2

            when(visitPosition % 4){
                0 -> binding.visitItemIcon.setImageResource(R.drawable.edit_course_circle_mint)
                1 -> binding.visitItemIcon.setImageResource(R.drawable.edit_course_circle_blue)
                2 -> binding.visitItemIcon.setImageResource(R.drawable.edit_course_circle_yellow)
                3 -> binding.visitItemIcon.setImageResource(R.drawable.edit_course_circle_pink)
            }

            if(item.name.isNotEmpty()) {
                binding.visitTitleText.text = item.name
            } else {
                binding.visitItemIcon.setImageResource(R.drawable.edit_course_circle_gray)
                binding.visitTitleText.text = "정확한 장소를 설정하세요."
            }

            if(selectedPosition == adapterPosition) {   //선택 데이터강조
                binding.highlightImage.setImageResource(R.color.edit_course_highlight)
            } else {
                binding.highlightImage.setImageResource(R.color.alpha)
            }

            if(item.staytime == 0)
                binding.stayTimeText.visibility = View.GONE
            else {
                binding.stayTimeText.visibility = View.VISIBLE
                binding.stayTimeText.text = getStayTimeText(item.staytime)
            }

            binding.highlightImage.setOnClickListener {
                setSelectItem(adapterPosition)

                itemClickListener?.onVisitItemClick()    //visitData position 으로 넘겨줌
            }

        }
    }
    inner class PathViewHolder(val binding: EditCoursePathItemBinding): RecyclerView.ViewHolder(binding.root){
        @SuppressLint("SetTextI18n")
        fun bind(item: PathData)
        {
            when(visitdata[adapterPosition / 2].type){
                ServerResponse.VISIT_TYPE_WALK.code -> {
                    binding.pathWalkTypeIcon.setImageResource(R.drawable.edit_course_path_icon_walk_on)
                    binding.pathCarTypeIcon.setImageResource(R.drawable.edit_course_path_icon_car_off)
                    binding.pathBusTypeIcon.setImageResource(R.drawable.edit_course_path_icon_bus_off)

                    binding.pathDistanceText.text = "도보로 약 ${getDistanceText(visitdata[adapterPosition / 2].distance)} 이동"
                }

                ServerResponse.VISIT_TYPE_CAR.code -> {
                    binding.pathWalkTypeIcon.setImageResource(R.drawable.edit_course_path_icon_walk_off)
                    binding.pathCarTypeIcon.setImageResource(R.drawable.edit_course_path_icon_car_on)
                    binding.pathBusTypeIcon.setImageResource(R.drawable.edit_course_path_icon_bus_off)

                    binding.pathDistanceText.text = "자동차로 약 ${getDistanceText(visitdata[adapterPosition / 2].distance)} 이동"
                }

                ServerResponse.VISIT_TYPE_PUBLIC_TRANSPORT.code -> {
                    binding.pathWalkTypeIcon.setImageResource(R.drawable.edit_course_path_icon_walk_off)
                    binding.pathCarTypeIcon.setImageResource(R.drawable.edit_course_path_icon_car_off)
                    binding.pathBusTypeIcon.setImageResource(R.drawable.edit_course_path_icon_bus_on)

                    binding.pathDistanceText.text = "대중교통으로 약 ${getDistanceText(visitdata[adapterPosition / 2].distance)} 이동"
                }

                else -> {
                    binding.pathWalkTypeIcon.setImageResource(R.drawable.edit_course_path_icon_walk_on)
                    binding.pathCarTypeIcon.setImageResource(R.drawable.edit_course_path_icon_car_off)
                    binding.pathBusTypeIcon.setImageResource(R.drawable.edit_course_path_icon_bus_off)

                    binding.pathDistanceText.text = "도보로 약 ${getDistanceText(visitdata[adapterPosition / 2].distance)} 이동"
                }
            }

            if(selectedPosition == adapterPosition) {   //선택 데이터강조
                binding.highlightImage.setImageResource(R.color.edit_course_highlight)
            } else {
                binding.highlightImage.setImageResource(R.color.alpha)
            }

            binding.highlightImage.setOnClickListener {
                setSelectItem(adapterPosition)

                itemClickListener?.onPathItemClick()    //pathData position 으로 넘겨줌
            }

            binding.pathWalkTypeBox.setOnClickListener {
                pathTypeBoxClick(ServerResponse.VISIT_TYPE_WALK.code, adapterPosition)
            }

            binding.pathCarTypeBox.setOnClickListener {
                pathTypeBoxClick(ServerResponse.VISIT_TYPE_CAR.code, adapterPosition)
            }

            binding.pathBusTypeBox.setOnClickListener {
                pathTypeBoxClick(ServerResponse.VISIT_TYPE_PUBLIC_TRANSPORT.code, adapterPosition)
            }

        }
    }

    private fun pathTypeBoxClick(type: String, adapterPosition: Int) {
        if(!isTypeChangeFinished)   //이전 작업이 있으면
            return

        isTypeChangeFinished = false

        if(selectedPosition == adapterPosition){    //강조된 Path 의 Type 선택한 경우
            itemClickListener?.onPathTypeClick(type)
        } else {
            setSelectItem(adapterPosition)

            itemClickListener?.onPathItemClick()
            itemClickListener?.onPathTypeClick(type)
        }
    }

    fun successChangeType(type: String, position: Int){   //이미 /2 된 position 받음
        isTypeChangeFinished = true

        visitdata[position].type = type

        notifyItemChanged(selectedPosition)

        Log.d("MyCourseAdapter", "isTypeChangeFinished: $isTypeChangeFinished")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            VISIT -> {
                val binding = EditCourseVisitItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                VisitViewHolder(binding)
            }
            PATH -> {
                val binding = EditCoursePathItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PathViewHolder(binding)
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
                    holder.bind(visitdata[position / 2])
                }
            }
            1 -> {
                if (holder is PathViewHolder) {
                    holder.bind(pathdata[position / 2])
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

    private fun getStayTimeText(minutes: Int): String{
        if(minutes == 0)
            return ""

        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        if(hours == 0)
            return "${remainingMinutes}분"
        return "약 ${hours}시간 ${remainingMinutes}분 방문"
    }

    private fun getDistanceText(distance: Int): String {
        return if(distance < 1000){
            "${distance}m"
        } else {
            "${distance/1000}.${distance % 1000 / 100}Km"
        }
    }

    private fun setSelectItem(adapterPosition: Int){
        val lastSelectedPosition = selectedPosition

        selectedPosition = adapterPosition
        notifyItemChanged(selectedPosition)

        if(lastSelectedPosition != -1 && lastSelectedPosition != selectedPosition)
            notifyItemChanged(lastSelectedPosition)
    }

    fun clearSelect(){
        if(selectedPosition == -1)
            return

        val lastSelectedPosition = selectedPosition

        selectedPosition = -1
        notifyItemChanged(lastSelectedPosition)
    }

    fun updateVisitData(newVisitData: VisitData, position: Int){
        if(newVisitData.lat_set != visitdata[position / 2].lat_set || newVisitData.lng_set != visitdata[position / 2].lng_set) //위치도 바뀐 경우
            adjustPathData(newVisitData, position / 2)

        visitdata[position / 2] = newVisitData

        notifyItemChanged(position)
    }

    private fun adjustPathData(visitData: VisitData, visitPosition: Int){
        when(visitPosition){
            0 -> {  //처음 인 경우
                changeAfterPath(visitData, visitPosition)
            }
            visitdata.lastIndex -> {    //마지막 인 경우
                changeBeforePath(visitData, visitPosition - 1)
            }
            else -> {
                changeBeforePath(visitData, visitPosition - 1)
                changeAfterPath(visitData, visitPosition)
            }
        }
    }

    private fun changeBeforePath(visitData: VisitData, pathPosition: Int){
        pathdata[pathPosition].coordinates[pathdata[pathPosition].coordinates.lastIndex] = LocationData(
            pathdata[pathPosition].coordinates[pathdata[pathPosition].coordinates.lastIndex].datetime, visitData.lat_set, visitData.lng_set)
    }

    private fun changeAfterPath(visitData: VisitData, pathPosition: Int){
        pathdata[pathPosition].coordinates[0] = LocationData(
            pathdata[pathPosition].coordinates[0].datetime, visitData.lat_set, visitData.lng_set)
    }

    companion object {
        const val VISIT = 0
        const val PATH = 1
    }
}