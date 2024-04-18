package com.vecto_example.vecto.ui.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.Collections.addAll

class SearchViewModel(private val repository: FeedRepository) : ViewModel() {
    var nextPage: Int = 0
    var lastPage: Boolean = false
    var followPage: Boolean = true
    var originLoginFlag: Boolean? = null

    var newFeedIds = mutableListOf<Int>()
    var newFeedInfo = mutableListOf<VectoService.FeedInfoResponse>()

    private val _isLoadingCenter = MutableLiveData(false)
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData(false)
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    private val _feedIdsLiveData = MutableLiveData<VectoService.FeedPageResponse>()
    val feedIdsLiveData: LiveData<VectoService.FeedPageResponse> = _feedIdsLiveData

    private val _feedInfoLiveData = MutableLiveData<List<VectoService.FeedInfoResponse>>()
    val feedInfoLiveData: LiveData<List<VectoService.FeedInfoResponse>> = _feedInfoLiveData

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
                    val feedInfo = mutableListOf<VectoService.FeedInfoResponse>()
                    val successfulFeedIds = mutableListOf<Int>()

                    feedPageResponse.feedIds.forEach { feedId ->
                        val job = async {
                            try {
                                repository.getFeedInfo(feedId).also {
                                    successfulFeedIds.add(feedId)
                                }
                            } catch (e: Exception) {
                                Log.e("fetchFeedResults", "Failed to fetch feed info for ID $feedId", e)
                                null // 실패한 경우 null 반환
                            }
                        }
                        job.await()?.let {
                            feedInfo.add(it) // null이 아닌 결과만 추가
                        }
                    }
                    // 기존 데이터에 새로운 데이터 추가
                    val updatedFeedInfo = _feedInfoLiveData.value?.toMutableList() ?: mutableListOf()
                    updatedFeedInfo.addAll(feedInfo)

                    newFeedInfo.clear()
                    newFeedInfo.addAll(feedInfo)

                    _feedInfoLiveData.postValue(updatedFeedInfo)

                    // 기존 Feed ID 리스트에 새로운 ID 추가
                    val updatedFeedIds = _feedIdsLiveData.value?.feedIds?.toMutableList() ?: mutableListOf()
                    updatedFeedIds.addAll(feedPageResponse.feedIds)

                    _feedIdsLiveData.postValue(feedPageResponse.copy(feedIds = (_feedIdsLiveData.value?.feedIds ?: mutableListOf()).apply {
                        addAll(successfulFeedIds)
                    }))

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
                    val feedInfo = mutableListOf<VectoService.FeedInfoResponse>()
                    val successfulFeedIds = mutableListOf<Int>()

                    feedPageResponse.feedIds.forEach { feedId ->
                        val job = async {
                            try {
                                repository.getFeedInfo(feedId).also {
                                    successfulFeedIds.add(feedId)
                                }
                            } catch (e: Exception) {
                                Log.e("fetchPersonalFeedResults", "Failed to fetch feed info for ID $feedId", e)
                                null // 실패한 경우 null 반환
                            }
                        }
                        job.await()?.let {
                            feedInfo.add(it) // null이 아닌 결과만 추가
                        }
                    }
                    // 기존 데이터에 새로운 데이터 추가
                    val updatedFeedInfo = _feedInfoLiveData.value?.toMutableList() ?: mutableListOf()
                    updatedFeedInfo.addAll(feedInfo)

                    newFeedInfo.clear()
                    newFeedInfo.addAll(feedInfo)

                    _feedInfoLiveData.postValue(updatedFeedInfo)

                    // 기존 Feed ID 리스트에 새로운 ID 추가
                    val updatedFeedIds = _feedIdsLiveData.value?.feedIds?.toMutableList() ?: mutableListOf()
                    updatedFeedIds.addAll(feedPageResponse.feedIds)

                    _feedIdsLiveData.postValue(feedPageResponse.copy(feedIds = (_feedIdsLiveData.value?.feedIds ?: mutableListOf()).apply {
                        addAll(successfulFeedIds)
                    }))

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
                    val feedInfo = mutableListOf<VectoService.FeedInfoResponse>()
                    val successfulFeedIds = mutableListOf<Int>()

                    feedPageResponse.feedIds.forEach { feedId ->
                        val job = async {
                            try {
                                repository.getFeedInfo(feedId).also {
                                    successfulFeedIds.add(feedId)
                                }
                            } catch (e: Exception) {
                                Log.e("fetchSearchFeedResults", "Failed to fetch feed info for ID $feedId", e)
                                null // 실패한 경우 null 반환
                            }
                        }
                        job.await()?.let {
                            feedInfo.add(it) // null이 아닌 결과만 추가
                        }
                    }
                    // 기존 데이터에 새로운 데이터 추가
                    val updatedFeedInfo = _feedInfoLiveData.value?.toMutableList() ?: mutableListOf()
                    updatedFeedInfo.addAll(feedInfo)

                    newFeedInfo.clear()
                    newFeedInfo.addAll(feedInfo)

                    _feedInfoLiveData.postValue(updatedFeedInfo)

                    // 기존 Feed ID 리스트에 새로운 ID 추가
                    val updatedFeedIds = _feedIdsLiveData.value?.feedIds?.toMutableList() ?: mutableListOf()
                    updatedFeedIds.addAll(feedPageResponse.feedIds)

                    _feedIdsLiveData.postValue(feedPageResponse.copy(feedIds = (_feedIdsLiveData.value?.feedIds ?: mutableListOf()).apply {
                        addAll(successfulFeedIds)
                    }))

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

    fun initSetting(){
        Log.d("SEARCH_VIEWMODEL", "initSetting")

        nextPage = 0
        lastPage = false
        followPage = true

        _feedInfoLiveData.postValue(emptyList())
        _feedIdsLiveData.postValue(VectoService.FeedPageResponse(0, emptyList(), false, true))

        newFeedIds.clear()
        newFeedInfo.clear()
    }

    fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false)
    }
}