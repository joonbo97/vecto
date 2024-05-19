package com.vecto_example.vecto.ui.followinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository

class FollowInfoViewModelFactory(private val userRepository: UserRepository, private val tokenRepository: TokenRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FollowInfoViewModel::class.java)) {
            return FollowInfoViewModel(userRepository, tokenRepository) as T
        }

        throw IllegalArgumentException("FollowInfoViewModelFactory ERROR")
    }
}