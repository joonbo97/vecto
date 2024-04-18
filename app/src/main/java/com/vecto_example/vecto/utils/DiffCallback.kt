package com.vecto_example.vecto.utils

import androidx.recyclerview.widget.DiffUtil

class DiffCallback (private val oldList: List<Any>, private val newList: List<Any>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // 고유 ID 또는 데이터를 비교
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // 데이터의 내용이 같은지 확인
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}