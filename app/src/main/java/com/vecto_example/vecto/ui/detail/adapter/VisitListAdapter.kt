package com.vecto_example.vecto.ui.detail.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.PathData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.databinding.DetailPathItemBinding
import com.vecto_example.vecto.databinding.DetailVisitItemBinding
import com.vecto_example.vecto.utils.ServerResponse

class VisitListAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val visitData = mutableListOf<VisitData>()
    val pathData = mutableListOf<PathData>()
    interface OnDetailItemClickListener {
        fun onVisitItemClick(visitData: VisitData, itemPosition: Int)

        fun onPathItemClick(pathData: PathData)
    }

    var detailItemClickListener: OnDetailItemClickListener? = null

    inner class VisitViewHolder(val binding: DetailVisitItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VisitData) {

            when(adapterPosition / 2){
                0 -> binding.visitItemIcon.setImageResource(R.drawable.detail_number_1)
                1 -> binding.visitItemIcon.setImageResource(R.drawable.detail_number_2)
                2 -> binding.visitItemIcon.setImageResource(R.drawable.detail_number_3)
                3 -> binding.visitItemIcon.setImageResource(R.drawable.detail_number_4)
                4 -> binding.visitItemIcon.setImageResource(R.drawable.detail_number_5)
                5 -> binding.visitItemIcon.setImageResource(R.drawable.detail_number_6)
                6 -> binding.visitItemIcon.setImageResource(R.drawable.detail_number_7)
                7 -> binding.visitItemIcon.setImageResource(R.drawable.detail_number_8)
                8 -> binding.visitItemIcon.setImageResource(R.drawable.detail_number_9)
                else -> binding.visitItemIcon.setImageResource(R.drawable.edit_course_circle_gray)
            }

            binding.visitTitleText.text = item.name

            binding.highlightImage.setOnClickListener {
                detailItemClickListener?.onVisitItemClick(item, adapterPosition / 2)
            }
        }
    }

    inner class PathViewHolder(val binding: DetailPathItemBinding): RecyclerView.ViewHolder(binding.root){
        @SuppressLint("SetTextI18n")
        fun bind(item: PathData) {


            when(visitData[adapterPosition / 2].transportType) {
                ServerResponse.VISIT_TYPE_WALK.code -> {
                    binding.pathTypeIcon.setImageResource(R.drawable.edit_course_path_icon_walk_on)
                    binding.pathDistanceText.text = "도보로 약 ${getDistanceText(visitData[adapterPosition / 2].distance)} 이동"
                }
                ServerResponse.VISIT_TYPE_CAR.code -> {
                    binding.pathTypeIcon.setImageResource(R.drawable.edit_course_path_icon_car_on)
                    binding.pathDistanceText.text = "자동차로 약 ${getDistanceText(visitData[adapterPosition / 2].distance)} 이동"
                }
                ServerResponse.VISIT_TYPE_PUBLIC_TRANSPORT.code -> {
                    binding.pathTypeIcon.setImageResource(R.drawable.edit_course_path_icon_bus_on)
                    binding.pathDistanceText.text = "대중교통으로 약 ${getDistanceText(visitData[adapterPosition / 2].distance)} 이동"
                }
                else -> {
                    binding.pathTypeIcon.setImageResource(R.drawable.edit_course_path_icon_walk_on)
                    binding.pathDistanceText.text = "도보로 약 ${getDistanceText(visitData[adapterPosition / 2].distance)} 이동"
                }
            }

            binding.highlightImage.setOnClickListener {
                detailItemClickListener?.onPathItemClick(item)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(position % 2) {
            0 -> {
                if (holder is VisitViewHolder) {
                    holder.bind(visitData[position / 2])
                }
            }
            1 -> {
                if (holder is PathViewHolder) {
                    holder.bind(pathData[position / 2])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            VISIT -> {
                val binding = DetailVisitItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                VisitViewHolder(binding)
            }
            PATH -> {
                val binding = DetailPathItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PathViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    override fun getItemCount(): Int {
        return visitData.size + pathData.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (position % 2){
            0 -> VISIT
            1 -> PATH
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    companion object {
        const val VISIT = 0
        const val PATH = 1
    }

    private fun getDistanceText(distance: Int): String {
        return if(distance < 1000){
            "${distance}m"
        } else {
            "${distance/1000}.${distance % 1000 / 100}Km"
        }
    }
}