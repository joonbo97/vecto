package com.vecto_example.vecto.ui.followinfo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class FollowInfoViewModel(private val repository: UserRepository, private val tokenRepository: TokenRepository): ViewModel() {
    private val _reissueResponse = MutableSharedFlow<VectoService.TokenUpdateEvent>(replay = 0)
    val reissueResponse = _reissueResponse.asSharedFlow()

    private val _followListResponse = MutableLiveData<VectoService.FollowListResponse>()
    val followListResponse: LiveData<VectoService.FollowListResponse> = _followListResponse

    /*   로딩   */
    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private var tempLoading = false

    var userId = ""
    var postFollowId = ""
    var deleteFollowId = ""

    //팔로우 요청
    private val _postFollowResult = MutableLiveData<Boolean>()
    val postFollowResult: LiveData<Boolean> = _postFollowResult

    //팔로우 취소
    private val _deleteFollowResult = MutableLiveData<Boolean>()
    val deleteFollowResult: LiveData<Boolean> = _deleteFollowResult

    /*   에러   */
    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    private val _postFollowError = MutableLiveData<String>()
    val postFollowError: LiveData<String> = _postFollowError

    private val _deleteFollowError = MutableLiveData<String>()
    val deleteFollowError: LiveData<String> = _deleteFollowError

    enum class Function {
        GetFollowerList, GetFollowingList, PostFollow, DeleteFollow
    }

    private fun startLoading(){
        Log.d("FollowInfoViewModel", "Start Loading")

        _isLoadingCenter.value = true
    }

    private fun endLoading(){
        Log.d("FollowInfoViewModel", "End Loading")

        _isLoadingCenter.value = false

        tempLoading = false
    }

    fun getFollowerList(userId: String){
        this.userId = userId

        startLoading()

        viewModelScope.launch {
            val followerResponse = repository.getFollowerList(userId)

            followerResponse.onSuccess {
                _followListResponse.value = it

                endLoading()
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.GetFollowerList.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        endLoading()
                    }
                    else -> {
                        _errorMessage.postValue(R.string.get_follower_list_fail)
                        endLoading()
                    }
                }

                endLoading()
            }
        }
    }

    fun getFollowingList(userId: String){
        this.userId = userId
        startLoading()

        viewModelScope.launch {
            val followingResponse = repository.getFollowingList(userId)

            followingResponse.onSuccess {
                _followListResponse.value = it

                endLoading()
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.GetFollowingList.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        endLoading()
                    }
                    else -> {
                        _errorMessage.postValue(R.string.get_following_list_fail)
                        endLoading()
                    }
                }

                endLoading()
            }
        }
    }

    fun postFollow(userId: String) {
        tempLoading = true
        postFollowId = userId

        viewModelScope.launch {
            val followResponse = repository.postFollow(userId)

            followResponse.onSuccess {
                if(it == ServerResponse.SUCCESS_POSTFOLLOW.code){
                    _postFollowResult.value = true
                } else if(it == ServerResponse.SUCCESS_ALREADY_POSTFOLLOW.code) {
                    _postFollowResult.value = false
                }

                endLoading()
            }.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.PostFollow.name)
                    return@launch
                }

                _postFollowError.value = it.message

                endLoading()
            }
        }
    }

    fun deleteFollow(userId: String) {
        tempLoading = true
        deleteFollowId = userId

        viewModelScope.launch {
            val followResponse = repository.deleteFollow(userId)

            followResponse.onSuccess {
                if(it == ServerResponse.SUCCESS_DELETEFOLLOW.code){
                    _deleteFollowResult.value = true
                } else if(it == ServerResponse.SUCCESS_ALREADY_DELETEFOLLOW.code) {
                    _deleteFollowResult.value = false
                }

                endLoading()
            }.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.DeleteFollow.name)
                    return@launch
                }

                _deleteFollowError.value = it.message

                endLoading()
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
                endLoading()
            }
        }
    }

    fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(isLoadingCenter.value == false && !tempLoading)
    }

}