package com.vecto_example.vecto.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.NotificationRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import java.lang.IllegalArgumentException

class NotificationViewModelFactory(private val repository: NotificationRepository, private val tokenRepository: TokenRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            return NotificationViewModel(repository, tokenRepository) as T
        }

        throw IllegalArgumentException("NotificationViewModelFactory ERROR")
    }

}