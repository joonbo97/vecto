package com.vecto_example.vecto.ui.followinfo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.launch

class FollowInfoViewModel(private val repository: UserRepository): ViewModel() {

    private val _followListResponse = MutableLiveData<VectoService.FollowListResponse>()
    val followListResponse: LiveData<VectoService.FollowListResponse> = _followListResponse

    /*   로딩   */
    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private var tempLoading = false

    //팔로우 요청
    private val _postFollowResult = MutableLiveData<Boolean>()
    val postFollowResult: LiveData<Boolean> = _postFollowResult

    //팔로우 취소
    private val _deleteFollowResult = MutableLiveData<Boolean>()
    val deleteFollowResult: LiveData<Boolean> = _deleteFollowResult

    /*   에러   */
    private val _getFollowError = MutableLiveData<String>()
    val getFollowError: LiveData<String> = _getFollowError

    private val _postFollowError = MutableLiveData<String>()
    val postFollowError: LiveData<String> = _postFollowError

    private val _deleteFollowError = MutableLiveData<String>()
    val deleteFollowError: LiveData<String> = _deleteFollowError

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
        startLoading()

        viewModelScope.launch {
            val followerResponse = repository.getFollowerList(userId)

            followerResponse.onSuccess {
                _followListResponse.value = it

                endLoading()
            }.onFailure {
                _getFollowError.value = it.message

                endLoading()
            }
        }
    }

    fun getFollowingList(userId: String){
        startLoading()

        viewModelScope.launch {
            val followingResponse = repository.getFollowingList(userId)

            followingResponse.onSuccess {
                _followListResponse.value = it

                endLoading()
            }.onFailure {
                _getFollowError.value = it.message

                endLoading()
            }
        }
    }

    fun postFollow(userId: String) {
        tempLoading = true

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
                _postFollowError.value = it.message

                endLoading()
            }
        }
    }

    fun deleteFollow(userId: String) {
        tempLoading = true

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
                _deleteFollowError.value = it.message

                endLoading()
            }
        }
    }

    fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(isLoadingCenter.value == false && !tempLoading)
    }

}