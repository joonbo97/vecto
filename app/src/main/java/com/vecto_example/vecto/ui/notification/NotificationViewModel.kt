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
    var nextPage: Int = 0
    var lastPage: Boolean = false

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
        if(nextPage == 0)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.postValue(true)
        else                //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.postValue(true)
    }

    private fun endLoading(){
        _isLoadingCenter.postValue(false)
        _isLoadingBottom.postValue(false)
    }

    fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false)
    }
}