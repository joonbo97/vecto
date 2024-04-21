package com.vecto_example.vecto.ui.notification

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.NotificationRepository
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.launch

class NotificationViewModel (private val repository: NotificationRepository): ViewModel() {
    private var nextPage: Int = 0
    private var lastPage: Boolean = false

    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    val allNotifications = mutableListOf<VectoService.Notification>()

    private val _notificationsLiveData = MutableLiveData<VectoService.NotificationResponse>()
    val notificationsLiveData: LiveData<VectoService.NotificationResponse> = _notificationsLiveData

    val newNotificationFlag = MutableLiveData<Result<Boolean>>()
    fun getNewNotificationFlag() {

        viewModelScope.launch {
            val result = repository.getNewNotificationFlag()
            newNotificationFlag.value = result
        }
    }

    fun getNotificationResults(){
        startLoading()
        Log.d("getNotification", "LOADING")

        viewModelScope.launch {
            try {
                if(!lastPage) {
                    val notificationResponse = repository.getNotification(nextPage)

                    _notificationsLiveData.postValue(notificationResponse)
                    allNotifications.addAll(notificationResponse.notifications)

                    nextPage = notificationResponse.nextPage
                    lastPage = notificationResponse.lastPage
                }
            } catch (e: Exception) {
                Log.e("NotificationError", "Failed to load notifications", e)
            } finally {
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