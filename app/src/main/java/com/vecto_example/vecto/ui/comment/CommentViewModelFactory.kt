package com.vecto_example.vecto.ui.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.CommentRepository
import com.vecto_example.vecto.data.repository.TokenRepository

class CommentViewModelFactory(private val repository: CommentRepository, private val tokenRepository: TokenRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommentViewModel::class.java)) {
            return CommentViewModel(repository, tokenRepository) as T
        }

        throw IllegalArgumentException("CommentViewModelFactory ERROR")
    }
}