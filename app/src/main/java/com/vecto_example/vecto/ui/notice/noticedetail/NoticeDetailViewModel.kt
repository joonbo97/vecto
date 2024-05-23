package com.vecto_example.vecto.ui.notice.noticedetail

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

class NoticeDetailViewModel(private val repository: NoticeRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _noticeResponse = MutableLiveData<VectoService.NoticeResponse>()
    val noticeResponse: LiveData<VectoService.NoticeResponse> = _noticeResponse

    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    fun getNotice(id: Int) {
        startLoading()

        viewModelScope.launch {
            val result = repository.getNotice(id)

            result.onSuccess {
                _noticeResponse.postValue(it)
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
        Log.d("NoticeDetailViewModel", "START_LOADING")

        _isLoading.postValue(true)
    }

    private fun endLoading(){
        Log.d("NoticeDetailViewModel", "END_LOADING")

        _isLoading.postValue(false)
    }
}