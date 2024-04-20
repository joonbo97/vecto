package com.vecto_example.vecto.ui.register

import android.util.Log
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

    private val _idDuplicateResult = MutableLiveData<Result<String>>()
    val idDuplicateResult: LiveData<Result<String>> = _idDuplicateResult

    private val _sendMailResult = MutableLiveData<Result<String>>()
    val sendMailResult: LiveData<Result<String>> = _sendMailResult

    var idCheckFinished = false
    var isMailSent = false

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun checkIdDuplicate(userId: String) {
        startLoading()

        viewModelScope.launch {
            val response = repository.checkUserId(userId)

            _idDuplicateResult.value = response

            endLoading()
        }
    }

    fun sendMail(email: String) {
        startLoading()

        viewModelScope.launch {
            val response = repository.sendMail(email)

            _sendMailResult.value = response

            endLoading()
        }
    }

    fun registerRequest(registerRequest: VectoService.RegisterRequest) {
        startLoading()

        viewModelScope.launch {
            val registerResponse = repository.register(registerRequest)

            _registerResult.value = registerResponse

            endLoading()
        }
    }

    private fun startLoading(){
        Log.d("RegisterViewModel", "START LOADING")

        _isLoading.value = true
    }

    private fun endLoading(){
        Log.d("RegisterViewModel", "END LOADING")

        _isLoading.value = false
    }
}