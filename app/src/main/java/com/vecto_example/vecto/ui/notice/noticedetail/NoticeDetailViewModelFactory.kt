package com.vecto_example.vecto.ui.notice.noticedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.NoticeRepository
import java.lang.IllegalArgumentException

class NoticeDetailViewModelFactory(private val repository: NoticeRepository): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoticeDetailViewModel::class.java)) {
            return NoticeDetailViewModel(repository) as T
        }

        throw IllegalArgumentException("NoticeDetailViewModelFactory ERROR")
    }
}