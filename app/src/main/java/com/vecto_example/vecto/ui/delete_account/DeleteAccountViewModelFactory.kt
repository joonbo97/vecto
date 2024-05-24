package com.vecto_example.vecto.ui.delete_account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository

class DeleteAccountViewModelFactory(private val userRepository: UserRepository, private val tokenRepository: TokenRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(DeleteAccountViewModel::class.java)) {
            return DeleteAccountViewModel(userRepository, tokenRepository) as T
        }

        throw IllegalArgumentException("DeleteAccountViewModelFactory ERROR")
    }
}