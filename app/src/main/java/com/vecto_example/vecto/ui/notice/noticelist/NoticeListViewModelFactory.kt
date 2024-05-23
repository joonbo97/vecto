package com.vecto_example.vecto.ui.notice.noticelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.NoticeRepository
import com.vecto_example.vecto.ui.notification.NotificationViewModel
import java.lang.IllegalArgumentException

class NoticeListViewModelFactory(private val repository: NoticeRepository): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoticeListViewModel::class.java)) {
            return NoticeListViewModel(repository) as T
        }

        throw IllegalArgumentException("NoticeListViewModelFactory ERROR")
    }
}