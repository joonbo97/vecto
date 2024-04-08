package com.vecto_example.vecto.ui.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CommentViewModelFactory(private val repository: CommentRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommentViewModel::class.java)) {
            return CommentViewModel(repository) as T
        }

        throw IllegalArgumentException("CommentViewModelFactory ERROR")
    }
}