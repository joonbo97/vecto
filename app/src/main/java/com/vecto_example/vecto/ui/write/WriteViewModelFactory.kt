package com.vecto_example.vecto.ui.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.TokenRepository

class WriteViewModelFactory(private val writeRepository: WriteRepository, private val tokenRepository: TokenRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(WriteViewModel::class.java)) {
            return WriteViewModel(writeRepository, tokenRepository) as T
        }

        throw IllegalArgumentException("WriteViewModelFactory ERROR")
    }
}