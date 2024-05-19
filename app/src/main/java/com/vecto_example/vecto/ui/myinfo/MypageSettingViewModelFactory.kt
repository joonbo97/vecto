package com.vecto_example.vecto.ui.myinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import java.lang.IllegalArgumentException

class MypageSettingViewModelFactory (private val userRepository: UserRepository, private val tokenRepository: TokenRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MypageSettingViewModel::class.java)) {
            return MypageSettingViewModel(userRepository, tokenRepository) as T
        }

        throw IllegalArgumentException("MypageSettingViewModelFactory ERROR")
    }
}