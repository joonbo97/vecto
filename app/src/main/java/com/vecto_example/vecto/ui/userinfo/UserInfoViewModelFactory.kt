package com.vecto_example.vecto.ui.userinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import java.lang.IllegalArgumentException

class UserInfoViewModelFactory(private val feedRepository: FeedRepository, private val userRepository: UserRepository, private val tokenRepository: TokenRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(UserInfoViewModel::class.java)) {
            return UserInfoViewModel(feedRepository, userRepository, tokenRepository) as T
        }

        throw IllegalArgumentException("UserInfoViewModelFactory ERROR")
    }
}