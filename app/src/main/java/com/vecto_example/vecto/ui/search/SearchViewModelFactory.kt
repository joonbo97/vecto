package com.vecto_example.vecto.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import java.lang.IllegalArgumentException

class SearchViewModelFactory(private val repository: FeedRepository, private val userRepository: UserRepository, private val tokenRepository: TokenRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(repository, userRepository, tokenRepository) as T
        }

        throw IllegalArgumentException("SearchViewModelFactory ERROR")
    }
}