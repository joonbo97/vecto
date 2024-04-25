package com.vecto_example.vecto.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.UserRepository
import java.lang.IllegalArgumentException

class FeedDetailViewModelFactory(private val repository: FeedRepository, private val userRepository: UserRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FeedDetailViewModel::class.java)) {
            return FeedDetailViewModel(repository, userRepository) as T
        }

        throw IllegalArgumentException("ViewModel Factory ERROR")
    }
}