package com.vecto_example.vecto.ui.comment

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

class CommentViewModel(private val repository: CommentRepository): ViewModel() {

    var nextPage: Int = 0
    var lastPage: Boolean = false

    private val _commentErrorLiveData = MutableLiveData<Result<VectoService.CommentListResponse>>()
    val commentErrorLiveData: LiveData<Result<VectoService.CommentListResponse>> = _commentErrorLiveData

    private val _commentInfoLiveData = MutableLiveData<VectoService.CommentListResponse>()
    val commentInfoLiveData: LiveData<VectoService.CommentListResponse> = _commentInfoLiveData

    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    private val _addCommentResult = MutableLiveData<Result<String>>()
    val addCommentResult: LiveData<Result<String>> = _addCommentResult

    private val _updateCommentResult = MutableLiveData<Result<String>>()
    val updateCommentResult: LiveData<Result<String>> = _updateCommentResult

    fun fetchCommentResults(feedId: Int){
        startLoading()

        viewModelScope.launch {
            val commentListResponse = repository.getCommentList(feedId, nextPage)

            commentListResponse.onSuccess {
                if(!lastPage){  //마지막 page가 아닐 경우에만 실행
                    _commentInfoLiveData.postValue(it)

                    nextPage = it.nextPage
                    lastPage = it.lastPage
                }
            }.onFailure {
                _commentErrorLiveData.value = commentListResponse
            }

            endLoading()
        }
    }

    fun fetchPersonalCommentResults(feedId: Int) {
        startLoading()

        viewModelScope.launch {
            val commentListResponse = repository.getPersonalCommentList(feedId, nextPage)

            commentListResponse.onSuccess {
                if(!lastPage){  //마지막 page가 아닐 경우에만 실행
                    _commentInfoLiveData.postValue(it)

                    nextPage = it.nextPage
                    lastPage = it.lastPage
                }
            }.onFailure {
                _commentErrorLiveData.value = commentListResponse
            }

            endLoading()
        }
    }

    fun addComment(feedId: Int, content: String) {
        startCenterLoading()
        viewModelScope.launch {
            val addCommentResponse = repository.addComment(feedId, content)

            _addCommentResult.value = addCommentResponse

            endLoading()
        }
    }

    fun updateComment(commentUpdateRequest: VectoService.CommentUpdateRequest) {
        startCenterLoading()
        viewModelScope.launch {
            val updateCommentResponse = repository.updateComment(commentUpdateRequest)

            _updateCommentResult.value = updateCommentResponse

            endLoading()
        }
    }

    fun initSetting(){
        Log.d("COMMENT_VIEWMODEL", "initSetting")

        nextPage = 0
        lastPage = false

        _commentInfoLiveData.postValue(VectoService.CommentListResponse(0, emptyList(), false))
    }

    fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false)
    }

    private fun startLoading(){
        Log.d("STARTLOADING", "START")

        if(nextPage == 0)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.value = true
        else                //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.value = true
    }

    private fun startCenterLoading(){
        Log.d("STARTLOADING", "START")

        _isLoadingCenter.value = true

    }

    private fun endLoading(){
        Log.d("ENDLOADING", "END")

        _isLoadingCenter.value = false
        _isLoadingBottom.value = false
    }

}