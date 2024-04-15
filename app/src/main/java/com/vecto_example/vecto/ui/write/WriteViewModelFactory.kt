package com.vecto_example.vecto.ui.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WriteViewModelFactory(private val repository: WriteRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(WriteViewModel::class.java)) {
            return WriteViewModel(repository) as T
        }

        throw IllegalArgumentException("WriteViewModelFactory ERROR")
    }
}