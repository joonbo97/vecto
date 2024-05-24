package com.vecto_example.vecto.ui.delete_account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.user.UserApiClient
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class DeleteAccountViewModel(private val userRepository: UserRepository, private val tokenRepository: TokenRepository): ViewModel() {
    private val _reissueResponse = MutableSharedFlow<VectoService.TokenUpdateEvent>(replay = 0)
    val reissueResponse = _reissueResponse.asSharedFlow()

    //탈퇴
    private val _deleteAccount = MutableLiveData<String>()
    val deleteAccount: LiveData<String> = _deleteAccount

    //에러
    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    enum class Function {
        AccountCancellation
    }

    fun accountCancellation(){
        if(Auth.provider == "kakao"){
            UserApiClient.instance.unlink { error ->
                if (error != null) {
                    _errorMessage.postValue(R.string.delete_kakao_error)
                }else {
                    deleteAccount()
                }
            }
        } else {
            deleteAccount()
        }
    }

    fun deleteAccount(){
        viewModelScope.launch {
            val deleteResponse = userRepository.deleteAccount()

            deleteResponse.onSuccess {
                _deleteAccount.postValue(it)
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.AccountCancellation.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                }
            }
        }
    }

    private fun reissueToken(function: String){
        viewModelScope.launch {
            val reissueResponse = tokenRepository.reissueToken()

            reissueResponse.onSuccess { //Access Token이 만료되어서 갱신됨
                _reissueResponse.emit(VectoService.TokenUpdateEvent(function, it))
            }.onFailure {
                when(it.message){
                    //아직 유효한 경우
                    ServerResponse.ACCESS_TOKEN_VALID_ERROR.code -> {}
                    //Refresh Token 만료
                    ServerResponse.REFRESH_TOKEN_INVALID_ERROR.code -> {
                        _errorMessage.postValue(R.string.expired_login)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                }
            }
        }
    }
}