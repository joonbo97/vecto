package com.vecto_example.vecto.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.NoticeRepository
import java.lang.IllegalArgumentException

class NewNoticeViewModelFactory (private val repository: NoticeRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewNoticeViewModel::class.java)) {
            return NewNoticeViewModel(repository) as T
        }

        throw IllegalArgumentException("NewNoticeViewModelFactory ERROR")
    }
}