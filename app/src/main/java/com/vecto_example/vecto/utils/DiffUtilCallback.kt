package com.vecto_example.vecto.utils

import androidx.recyclerview.widget.DiffUtil
import com.vecto_example.vecto.retrofit.VectoService

class DiffUtilCallback (private val oldList: List<VectoService.FeedInfoResponse>,
                        private val newList: List<VectoService.FeedInfoResponse>) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].content == newList[newItemPosition].content
    //TODO FeedId로 바꾸기

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition] == newList[newItemPosition]
}
