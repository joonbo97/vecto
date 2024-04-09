package com.vecto_example.vecto.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.launch

class RegisterViewModel (private val repository: UserRepository): ViewModel() {
    private val _registerResult = MutableLiveData<Result<String>>()
    val registerResult: LiveData<Result<String>> = _registerResult

    fun registerRequest(registerRequest: VectoService.RegisterRequest){
        viewModelScope.launch {
            val registerResponse = repository.register(registerRequest)

            _registerResult.value = registerResponse
        }
    }

}