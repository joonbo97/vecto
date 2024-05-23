package com.vecto_example.vecto.ui.notice.noticelist

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.NoticeRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.launch

class NoticeListViewModel(private val repository: NoticeRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _noticeListResponse = MutableLiveData<List<VectoService.NoticeListResponse>>()
    val noticeListResponse: LiveData<List<VectoService.NoticeListResponse>> = _noticeListResponse

    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    fun getNoticeList() {
        startLoading()

        viewModelScope.launch {
            val result = repository.getNoticeList()

            result.onSuccess {
                _noticeListResponse.postValue(it)
            }.onFailure {
                when(it.message){
                    ServerResponse.ERROR.code -> _errorMessage.postValue(R.string.APIErrorToastMessage)
                    else -> _errorMessage.postValue(R.string.APIFailToastMessage)
                }
            }

            endLoading()
        }
    }

    private fun startLoading(){
        Log.d("NoticeListViewModel", "START_LOADING")

        _isLoading.postValue(true)
    }

    private fun endLoading(){
        Log.d("NoticeListViewModel", "END_LOADING")

        _isLoading.postValue(false)
    }
}