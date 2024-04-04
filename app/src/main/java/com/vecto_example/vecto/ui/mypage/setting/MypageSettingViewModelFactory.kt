package com.vecto_example.vecto.ui.mypage.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.UserRepository
import java.lang.IllegalArgumentException

class MypageSettingViewModelFactory (private val repository: UserRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MypageSettingViewModel::class.java)) {
            return MypageSettingViewModel(repository) as T
        }

        throw IllegalArgumentException("MypageSettingViewModelFactory ERROR")
    }
}