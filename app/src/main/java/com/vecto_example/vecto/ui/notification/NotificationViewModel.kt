package com.vecto_example.vecto.ui.notification

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.NotificationRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.launch

class NotificationViewModel (private val repository: NotificationRepository, private val tokenRepository: TokenRepository): ViewModel() {
    private val _reissueResponse = MutableLiveData<String>()
    val reissueResponse: LiveData<String> = _reissueResponse

    var accessToken: String? = null
    var refreshToken: String? = null

    private var nextPage: Int = 0
    private var lastPage: Boolean = false

    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    val allNotifications = mutableListOf<VectoService.Notification>()

    private val _notificationsLiveData = MutableLiveData<VectoService.NotificationResponse>()
    val notificationsLiveData: LiveData<VectoService.NotificationResponse> = _notificationsLiveData

    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    enum class Function {
        GetNewNotificationFlag, GetNotificationResults
    }

    val newNotificationFlag = MutableLiveData<Result<Boolean>>()
    fun getNewNotificationFlag() {

        viewModelScope.launch {
            val result = repository.getNewNotificationFlag()

            result.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.GetNewNotificationFlag.name)
                }
            }

            newNotificationFlag.value = result
        }
    }

    fun getNotificationResults(){
        startLoading()
        Log.d("getNotification", "LOADING")

        viewModelScope.launch {
            if(!lastPage) {
                val notificationResponse = repository.getNotification(nextPage)

                notificationResponse.onSuccess {
                    _notificationsLiveData.postValue(it)
                    allNotifications.addAll(it.notifications)

                    nextPage = it.nextPage
                    lastPage = it.lastPage

                    endLoading()
                }.onFailure {
                    when(it.message){
                        ServerResponse.ACCESS_TOKEN_INVALID_ERROR.name -> {
                            reissueToken(Function.GetNotificationResults.name)
                        }
                        ServerResponse.ERROR.name -> {
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

    }

    private fun reissueToken(function: String){
        viewModelScope.launch {
            val reissueResponse = tokenRepository.reissueToken()

            reissueResponse.onSuccess { //Access Token이 만료되어서 갱신됨
                accessToken = it.accessToken
                refreshToken = it.refreshToken
                _reissueResponse.postValue(function)
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
                endLoading()
            }
        }
    }

    private fun startLoading(){
        Log.d("NotificationViewModel", "START_LOADING")

        if(nextPage == 0)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.value = true
        else                //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.value = true
    }

    private fun endLoading(){
        Log.d("NotificationViewModel", "END_LOADING")

        _isLoadingCenter.value = false
        _isLoadingBottom.value = false
    }

    fun checkLoading(): Boolean{
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false)
    }
}