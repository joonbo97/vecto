package com.vecto_example.vecto.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: UserRepository): ViewModel() {
    lateinit var loginRequestData: VectoService.LoginRequest

    private val _loginResult = MutableLiveData<Result<String>>()
    val loginResult: LiveData<Result<String>> = _loginResult

    private val _registerResult = MutableLiveData<Result<String>>()
    val registerResult: LiveData<Result<String>> = _registerResult

    private val _userInfoResult = MutableLiveData<Result<VectoService.UserInfoResponse>>()
    val userInfoResult: LiveData<Result<VectoService.UserInfoResponse>> = _userInfoResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loginRequest(loginRequest: VectoService.LoginRequest){
        loginRequestData= loginRequest

        viewModelScope.launch {
            val loginResponse = repository.login(loginRequest)

            _loginResult.value = loginResponse
        }

    }

    fun registerRequest(registerRequest: VectoService.RegisterRequest){
        viewModelScope.launch {
            val registerResponse = repository.register(registerRequest)

            _registerResult.value = registerResponse
        }
    }

    fun getUserInfo(){
        viewModelScope.launch {
            val userInfoResponse = repository.getUserInfo(loginRequestData.userId)

            _userInfoResult.value = userInfoResponse
        }
    }
}