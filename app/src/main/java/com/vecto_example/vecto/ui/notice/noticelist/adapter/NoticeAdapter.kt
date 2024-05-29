package com.vecto_example.vecto.ui.notice.noticelist.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.databinding.NoticeItemBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.DateTimeUtils

class NoticeAdapter(): RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder>() {
    val noticeList = mutableListOf<VectoService.NoticeListResponse>()

    var shownNoticeId = -1

    interface OnItemClickListener {
        fun onNoticeItemClick(noticeId: Int)
    }

    var itemClickListener: OnItemClickListener? = null

    inner class NoticeViewHolder(val binding: NoticeItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VectoService.NoticeListResponse) {
            binding.noticeTitle.text = item.title

            if((DateTimeUtils.isNewNotice(item.createdAt)) && (shownNoticeId < item.id))
                binding.newNoticeImage.visibility = View.VISIBLE
            else
                binding.newNoticeImage.visibility = View.INVISIBLE

            binding.noticeTimeText.text = DateTimeUtils.getNoticeTime(item.createdAt)

            itemView.setOnClickListener {
                itemClickListener?.onNoticeItemClick(item.id)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        val binding = NoticeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoticeViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return noticeList.size
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        holder.bind(noticeList[position])
    }

    fun setData(noticeData: List<VectoService.NoticeListResponse>) {
        noticeList.addAll(noticeData)

        notifyItemRangeInserted(0, noticeList.size)
    }
}