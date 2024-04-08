package com.vecto_example.vecto.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: UserRepository): ViewModel() {

    val loginResult = MutableLiveData<Result<Boolean>>()


    fun loginRequest(loginRequest: VectoService.LoginRequest, provider: String){
        viewModelScope.launch {

            val loginResponse = repository.login(loginRequest)

            loginResult.value = loginResponse
        }
    }

    //fun fetchUserFeedResults(userId: String){
    //        startLoading()
    //
    //        Log.d("FETCHUSERFEED", "LOADING")
    //
    //        viewModelScope.launch {
    //            try {
    //                if(!lastPage) { //마지막 page가 아닐 경우에만 실행
    //                    feedIdsLiveData.value?.let { allFeedIds.addAll(it.feedIds) }
    //                    feedInfoLiveData.value?.let { allFeedInfo.addAll(it) }
    //
    //                    val feedListResponse = repository.getUserFeedList(userId, nextPage)
    //                    val feedIds = feedListResponse.feedIds  //요청한 pageNo에 해당하는 Feed Ids
    //
    //                    val feedInfo = feedIds.map {
    //                        async { repository.getFeedInfo(it) }
    //                    }.awaitAll()    //모든 feed info 요청이 완료될 때까지 기다림
    //
    //                    _feedInfoLiveData.postValue(feedInfo)   //LiveData 값 변경
    //                    _feedIdsLiveData.postValue(feedListResponse)
    //
    //                    nextPage = feedListResponse.nextPage    //페이지 정보값 변경
    //                    lastPage = feedListResponse.lastPage
    //                }
    //            } catch (e: Exception) {
    //                throw Exception("fetchUserFeedResults Failed")
    //            } finally {
    //                endLoading()
    //            }
    //        }
    //    }
}