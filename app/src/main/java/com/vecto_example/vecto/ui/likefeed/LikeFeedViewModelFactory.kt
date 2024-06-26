package com.vecto_example.vecto.ui.likefeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository

class LikeFeedViewModelFactory(private val feedRepository: FeedRepository, private val userRepository: UserRepository, private val tokenRepository: TokenRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(LikeFeedViewModel::class.java)) {
            return LikeFeedViewModel(feedRepository, userRepository, tokenRepository) as T
        }

        throw IllegalArgumentException("LikeFeedViewModelFactory Error")
    }
}