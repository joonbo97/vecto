package com.vecto_example.vecto.ui.editcourse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vecto_example.vecto.data.repository.NaverRepository
import com.vecto_example.vecto.data.repository.TMapRepository
import java.lang.IllegalArgumentException

class EditCourseViewModelFactory (private val repository: TMapRepository, private val naverRepository: NaverRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(EditCourseViewModel::class.java)) {
            return EditCourseViewModel(repository, naverRepository) as T
        }

        throw IllegalArgumentException("EditCourseViewModelFactory ERROR")
    }
}