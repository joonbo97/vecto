package com.vecto_example.vecto.ui.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.launch

class FeedDetailViewModel(private val repository: FeedRepository) : ViewModel() {
    var nextPage: Int = 0
    var lastPage: Boolean = false
    var followPage: Boolean = true
    var originLoginFlag: Boolean? = false

    val allFeedInfo = mutableListOf<VectoService.FeedInfo>()

    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    private val _feedInfoLiveData = MutableLiveData<VectoService.FeedPageResponse>()
    val feedInfoLiveData: LiveData<VectoService.FeedPageResponse> = _feedInfoLiveData

    private val _feedErrorLiveData = MutableLiveData<String>()
    val feedErrorLiveData: LiveData<String> = _feedErrorLiveData

    private fun startLoading(){
        Log.d("STARTLOADING", "START")

        if(nextPage == 0)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.value = true
        else                //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.value = true
    }

    private fun endLoading(){
        Log.d("ENDLOADING", "END")

        _isLoadingCenter.value = false
        _isLoadingBottom.value = false
    }

    fun fetchFeedResults(){
        startLoading()

        viewModelScope.launch {
            val feedListResponse = repository.getFeedList(nextPage)

            feedListResponse.onSuccess { feedPageResponse ->
                if(!lastPage) {
                    allFeedInfo.addAll(feedPageResponse.feeds)
                    _feedInfoLiveData.postValue(feedPageResponse)

                    nextPage = feedPageResponse.nextPage    //페이지 정보값 변경
                    lastPage = feedPageResponse.lastPage
                    followPage = feedPageResponse.followPage
                }
                endLoading()
            }.onFailure {
                _feedErrorLiveData.value = it.message
                endLoading()
            }
        }
    }

    fun fetchPersonalFeedResults(){
        startLoading()

        viewModelScope.launch {
            val feedListResponse = repository.getPersonalFeedList(followPage, nextPage)

            feedListResponse.onSuccess { feedPageResponse ->
                if(!lastPage) {
                    allFeedInfo.addAll(feedPageResponse.feeds)
                    _feedInfoLiveData.postValue(feedPageResponse)

                    nextPage = feedPageResponse.nextPage    //페이지 정보값 변경
                    lastPage = feedPageResponse.lastPage
                    followPage = feedPageResponse.followPage
                }
                endLoading()
            }.onFailure {
                _feedErrorLiveData.value = it.message
                endLoading()
            }
        }
    }

    fun fetchSearchFeedResults(query: String){
        startLoading()

        viewModelScope.launch {
            val feedListResponse = repository.getSearchFeedList(query, nextPage)

            feedListResponse.onSuccess { feedPageResponse ->
                if(!lastPage) {
                    allFeedInfo.addAll(feedPageResponse.feeds)
                    _feedInfoLiveData.postValue(feedPageResponse)

                    nextPage = feedPageResponse.nextPage    //페이지 정보값 변경
                    lastPage = feedPageResponse.lastPage
                }
                endLoading()
            }.onFailure {
                _feedErrorLiveData.value = it.message
                endLoading()
            }
        }
    }


    fun fetchLikeFeedResults() {
        startLoading()

        viewModelScope.launch {
            val feedListResponse = repository.postLikeFeedList(nextPage)

            feedListResponse.onSuccess { feedPageResponse ->
                if(!lastPage) {
                    allFeedInfo.addAll(feedPageResponse.feeds)
                    _feedInfoLiveData.postValue(feedPageResponse)

                    nextPage = feedPageResponse.nextPage    //페이지 정보값 변경
                    lastPage = feedPageResponse.lastPage
                }
                endLoading()
            }.onFailure {
                _feedErrorLiveData.value = it.message
                endLoading()
            }
        }
    }

    fun fetchUserFeedResults(userId: String) {
        startLoading()

        viewModelScope.launch {
            val feedListResponse = repository.getUserFeedList(userId, nextPage)

            feedListResponse.onSuccess { feedPageResponse ->
                if(!lastPage) {
                    allFeedInfo.addAll(feedPageResponse.feeds)
                    _feedInfoLiveData.postValue(feedPageResponse)

                    nextPage = feedPageResponse.nextPage    //페이지 정보값 변경
                    lastPage = feedPageResponse.lastPage
                }
                endLoading()
            }.onFailure {
                _feedErrorLiveData.value = it.message
                endLoading()
            }
        }
    }

    fun initSetting(){
        nextPage = 0
        lastPage = false
        followPage = true

        allFeedInfo.clear()
    }
}