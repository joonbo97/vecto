package com.vecto_example.vecto.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.NoticeRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.launch

class NewNoticeViewModel(private val repository: NoticeRepository): ViewModel() {
    private val _noticeListResponse = MutableLiveData<VectoService.NoticeResponse>()
    val noticeListResponse: LiveData<VectoService.NoticeResponse> = _noticeListResponse

    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    fun getNewNotice() {

        viewModelScope.launch {
            val result = repository.getNewNotice()

            result.onSuccess {
                _noticeListResponse.postValue(it)
            }.onFailure {

            }

        }
    }
}