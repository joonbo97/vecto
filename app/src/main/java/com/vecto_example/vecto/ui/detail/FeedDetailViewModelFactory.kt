package com.vecto_example.vecto.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.FeedRepository
import java.lang.IllegalArgumentException

class FeedDetailViewModelFactory(private val repository: FeedRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FeedDetailViewModel::class.java)) {
            return FeedDetailViewModel(repository) as T
        }

        throw IllegalArgumentException("ViewModel Factory ERROR")
    }
}