package com.vecto_example.vecto.utils

import androidx.recyclerview.widget.DiffUtil
import com.vecto_example.vecto.retrofit.VectoService

class DiffUtilCallback (private val oldList: List<VectoService.FeedInfo>,
                        private val newList: List<VectoService.FeedInfo>) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].feedId == newList[newItemPosition].feedId

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition] == newList[newItemPosition]

    companion object {
        val diffUtil = object: DiffUtil.ItemCallback<VectoService.FeedInfo>() {
            override fun areItemsTheSame(oldItem: VectoService.FeedInfo, newItem: VectoService.FeedInfo): Boolean {
                return oldItem.feedId == newItem.feedId
            }

            override fun areContentsTheSame(oldItem: VectoService.FeedInfo, newItem: VectoService.FeedInfo): Boolean {
                return oldItem == newItem
            }
        }
    }
}
