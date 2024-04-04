package com.vecto_example.vecto.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.UserRepository
import java.lang.IllegalArgumentException

class MypageViewModelFactory(private val repository: UserRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MypageViewModel::class.java)) {
            return MypageViewModel(repository) as T
        }

        throw IllegalArgumentException("MyPage Factory ERROR")
    }
}