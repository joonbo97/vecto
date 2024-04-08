package com.vecto_example.vecto.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.UserRepository

class LoginViewModelFactory(private val repository: UserRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository) as T
        }

        throw IllegalArgumentException("LoginViewModelFactory ERROR")
    }
}