package com.vecto_example.vecto.ui.followinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.UserRepository

class FollowInfoViewModelFactory(private val userRepository: UserRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FollowInfoViewModel::class.java)) {
            return FollowInfoViewModel(userRepository) as T
        }

        throw IllegalArgumentException("FollowInfoViewModelFactory ERROR")
    }
}