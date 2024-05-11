package com.vecto_example.vecto.ui.comment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.CommentRepository
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.launch

class CommentViewModel(private val repository: CommentRepository): ViewModel() {

    var nextPage: Int = 0
    var lastPage: Boolean = false

    var firstFlag = true

    private val _commentErrorLiveData = MutableLiveData<Result<VectoService.CommentListResponse>>()
    val commentErrorLiveData: LiveData<Result<VectoService.CommentListResponse>> = _commentErrorLiveData

    private val _commentInfoLiveData = MutableLiveData<VectoService.CommentListResponse>()
    val commentInfoLiveData: LiveData<VectoService.CommentListResponse> = _commentInfoLiveData

    var allCommentInfo = mutableListOf<VectoService.CommentResponse>()

    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    private var tempLoading = false

    private val _addCommentResult = MutableLiveData<Result<String>>()
    val addCommentResult: LiveData<Result<String>> = _addCommentResult

    private val _sendCommentLikeResult = MutableLiveData<Result<String>>()
    val sendCommentLikeResult: LiveData<Result<String>> = _sendCommentLikeResult

    private val _cancelCommentLikeResult = MutableLiveData<Result<String>>()
    val cancelCommentLikeResult: LiveData<Result<String>> = _cancelCommentLikeResult

    private val _updateCommentResult = MutableLiveData<Result<String>>()
    val updateCommentResult: LiveData<Result<String>> = _updateCommentResult

    private val _deleteCommentResult = MutableLiveData<Result<String>>()
    val deleteCommentResult: LiveData<Result<String>> = _deleteCommentResult

    fun getCommentList(feedId: Int){
        if(!lastPage)
            startLoading()

        viewModelScope.launch {
            if(lastPage)
                return@launch

            val commentListResponse: Result<VectoService.CommentListResponse>

            if(Auth.loginFlag.value == true)
                commentListResponse = repository.getPersonalCommentList(feedId, nextPage)
            else
                commentListResponse = repository.getCommentList(feedId, nextPage)

            commentListResponse.onSuccess {

                if(firstFlag){
                    allCommentInfo = it.comments.toMutableList()
                } else {
                    allCommentInfo.addAll(it.comments)
                }

                _commentInfoLiveData.postValue(it)

                nextPage = it.nextPage
                lastPage = it.lastPage
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

    fun sendCommentLike(commentId: Int) {
        tempLoading = true
        viewModelScope.launch {
            val sendCommentLikeResponse = repository.sendCommentLike(commentId)

            _sendCommentLikeResult.value = sendCommentLikeResponse

            endLoading()
        }
    }

    fun cancelCommentLike(commentId: Int) {
        tempLoading = true
        viewModelScope.launch {
            val cancelCommentLikeResponse = repository.cancelCommentLike(commentId)

            _cancelCommentLikeResult.value = cancelCommentLikeResponse

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

    fun deleteComment(commentInt: Int) {
        startCenterLoading()
        viewModelScope.launch {
            val deleteCommentResponse = repository.deleteComment(commentInt)

            _deleteCommentResult.value = deleteCommentResponse

            endLoading()
        }

    }

    fun initSetting(){
        Log.d("COMMENT_VIEWMODEL", "initSetting")

        nextPage = 0
        lastPage = false

        firstFlag = true
    }

    fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false && !tempLoading)
    }

    private fun startLoading(){
        Log.d("STARTLOADING", "START")

        if(firstFlag)   //처음 실행하는 경우 center 로딩
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
        tempLoading = false
    }

}