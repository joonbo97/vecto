package com.vecto_example.vecto.ui.userinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.FeedRepository
import java.lang.IllegalArgumentException

class UserInfoViewModelFactory(private val repository: FeedRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(UserInfoViewModel::class.java)) {
            return UserInfoViewModel(repository) as T
        }

        throw IllegalArgumentException("UserInfoViewModelFactory ERROR")
    }
}