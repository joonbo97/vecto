package com.vecto_example.vecto.ui.comment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.CommentRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.launch

class CommentViewModel(private val repository: CommentRepository, private val tokenRepository: TokenRepository): ViewModel() {
    private val _reissueResponse = MutableLiveData<String>()
    val reissueResponse: LiveData<String> = _reissueResponse

    var accessToken: String? = null
    var refreshToken: String? = null

    var nextPage: Int = 0
    var lastPage: Boolean = false

    var firstFlag = true

    var sendCommentLikeId = -1
    var cancelCommnetLikeId = -1
    var deleteCommentId = -1

    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

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

    enum class Function{
        GetCommentList, AddComment, SendCommentLike, CancelCommentLike, UpdateComment, DeleteComment
    }


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
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.GetCommentList.name)
                }

                _commentErrorLiveData.value = commentListResponse
            }

            endLoading()
        }
    }

    fun addComment(feedId: Int, content: String) {
        startCenterLoading()
        viewModelScope.launch {
            val addCommentResponse = repository.addComment(feedId, content)

            addCommentResponse.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.AddComment.name)
                }
            }

            _addCommentResult.value = addCommentResponse

            endLoading()
        }
    }

    fun sendCommentLike(commentId: Int) {
        tempLoading = true
        sendCommentLikeId = commentId

        viewModelScope.launch {
            val sendCommentLikeResponse = repository.sendCommentLike(commentId)

            sendCommentLikeResponse.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.SendCommentLike.name)
                }
            }

            _sendCommentLikeResult.value = sendCommentLikeResponse

            endLoading()
        }
    }

    fun cancelCommentLike(commentId: Int) {
        tempLoading = true
        cancelCommnetLikeId = commentId

        viewModelScope.launch {
            val cancelCommentLikeResponse = repository.cancelCommentLike(commentId)

            cancelCommentLikeResponse.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.CancelCommentLike.name)
                }
            }

            _cancelCommentLikeResult.value = cancelCommentLikeResponse

            endLoading()
        }
    }

    fun updateComment(commentUpdateRequest: VectoService.CommentUpdateRequest) {
        startCenterLoading()
        viewModelScope.launch {
            val updateCommentResponse = repository.updateComment(commentUpdateRequest)

            updateCommentResponse.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.UpdateComment.name)
                }
            }

            _updateCommentResult.value = updateCommentResponse

            endLoading()
        }
    }

    fun deleteComment(commentInt: Int) {
        deleteCommentId = commentInt

        startCenterLoading()
        viewModelScope.launch {
            val deleteCommentResponse = repository.deleteComment(commentInt)

            deleteCommentResponse.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.DeleteComment.name)
                }
            }

            _deleteCommentResult.value = deleteCommentResponse

            endLoading()
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