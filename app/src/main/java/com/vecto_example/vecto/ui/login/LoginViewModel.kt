package com.vecto_example.vecto.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.launch

class LoginViewModel(private val userRepository: UserRepository, private val tokenRepository: TokenRepository): ViewModel() {
    lateinit var loginRequestData: VectoService.LoginRequest

    private val _loginResult = MutableLiveData<VectoService.UserToken>()
    val loginResult: LiveData<VectoService.UserToken> = _loginResult

    private val _registerResult = MutableLiveData<String>()
    val registerResult: LiveData<String> = _registerResult

    private val _userInfoResult = MutableLiveData<VectoService.UserInfoResponse>()
    val userInfoResult: LiveData<VectoService.UserInfoResponse> = _userInfoResult

    private val _isLoginFinished = MutableLiveData(false)
    val isLoginFinished: LiveData<Boolean> = _isLoginFinished

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    private val _reissueResponse = MutableLiveData<String>()
    val reissueResponse: LiveData<String> = _reissueResponse

    var userId: String? = null
    var nickname: String = ""
    var accessToken: String? = null
    var refreshToken: String? = null

    fun loginRequest(loginRequest: VectoService.LoginRequest){
        startLoading()

        loginRequestData = loginRequest

        viewModelScope.launch {
            val loginResponse = userRepository.login(loginRequest)

            loginResponse.onSuccess {
                _loginResult.postValue(it)
                endLoading()
            }.onFailure {
                when(it.message){
                    ServerResponse.FAIL.code -> {
                        if(loginRequest.userPw == null){    //KAKAO
                            registerRequest(VectoService.RegisterRequest(loginRequestData.userId, null, "kakao", nickname, null, null))
                        } else {    //VECTO
                            _errorMessage.postValue(R.string.login_wrong)
                            endLoading()
                        }
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        endLoading()
                    }
                }
            }
        }

    }

    private fun registerRequest(registerRequest: VectoService.RegisterRequest){

        viewModelScope.launch {
            val registerResponse = userRepository.register(registerRequest)

            registerResponse.onSuccess {
                _registerResult.postValue(it)
            }.onFailure {
                when(it.message){
                    ServerResponse.FAIL.code -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                    }
                }
            }

            endLoading()
        }
    }

    fun getUserInfo(){
        startLoading()

        viewModelScope.launch {
            val userInfoResponse = userRepository.getUserInfo(userId!!)

            userInfoResponse.onSuccess {
                _userInfoResult.postValue(it)

                endLoading()
            }.onFailure {
                when(it.message){
                    ServerResponse.FAIL_GET_USERINFO.code -> {
                        _errorMessage.postValue(R.string.login_none)
                        endLoading()
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        endLoading()
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                        endLoading()
                    }
                }
            }
        }
    }

    fun reissueToken(){
        startLoading()

        viewModelScope.launch {
            val reissueResponse = tokenRepository.reissueToken()

            reissueResponse.onSuccess { //Access Token이 만료되어서 갱신됨
                accessToken = it.accessToken
                refreshToken = it.refreshToken
                _reissueResponse.postValue(it.accessToken)
            }.onFailure {
                when(it.message){
                    //아직 유효한 경우
                    ServerResponse.ACCESS_TOKEN_VALID_ERROR.code -> {
                        getUserInfo()
                    }
                    //Refresh Token 만료
                    ServerResponse.REFRESH_TOKEN_INVALID_ERROR.code -> {
                        _errorMessage.postValue(R.string.expired_login)
                        endLoading()
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                        endLoading()
                    }
                }
            }
        }
    }

    fun loginFinish(){
        _isLoginFinished.value = true
    }

    fun startLoading(){
        _isLoading.postValue(true)
    }

    fun endLoading(){
        _isLoading.postValue(false)
    }
}