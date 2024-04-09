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
            try {
                if(!lastPage) { //마지막 page가 아닐 경우에만 실행

                    val feedListResponse = repository.getFeedList(nextPage)
                    val feedIds = feedListResponse.feedIds  //요청한 pageNo에 해당하는 Feed Ids

                    val feedInfo = feedIds.map {
                        async { repository.getFeedInfo(it) }
                    }.awaitAll()    //모든 feed info 요청이 완료될 때까지 기다림

                    // 기존 데이터에 새로운 데이터 추가
                    val updatedFeedInfo = _feedInfoLiveData.value?.toMutableList() ?: mutableListOf()
                    updatedFeedInfo.addAll(feedInfo)

                    newFeedInfo.clear()
                    newFeedInfo.addAll(feedInfo)

                    _feedInfoLiveData.postValue(updatedFeedInfo)

                    // 기존 Feed ID 리스트에 새로운 ID 추가
                    val updatedFeedIds = _feedIdsLiveData.value?.feedIds?.toMutableList() ?: mutableListOf()
                    updatedFeedIds.addAll(feedIds)

                    newFeedIds.clear()
                    newFeedIds.addAll(feedIds)

                    _feedIdsLiveData.postValue(feedListResponse.copy(feedIds = updatedFeedIds))

                    nextPage = feedListResponse.nextPage    //페이지 정보값 변경
                    lastPage = feedListResponse.lastPage
                }
            } catch (e: Exception) {
                throw Exception("fetchPostResults Failed")
            } finally {
                endLoading()
            }
        }
    }

    fun fetchPersonalFeedResults(){
        startLoading()

        viewModelScope.launch {
            try {
                if(!lastPage){  //마지막 page가 아닐 경우에만 실행
                    val feedListResponse = repository.getPersonalFeedList(followPage, nextPage)
                    val feedIds = feedListResponse.feedIds

                    val feedInfo = feedIds.map {
                        async { repository.getFeedInfo(it) }
                    }.awaitAll()

                    // 기존 데이터에 새로운 데이터 추가
                    val updatedFeedInfo = _feedInfoLiveData.value?.toMutableList() ?: mutableListOf()
                    updatedFeedInfo.addAll(feedInfo)

                    newFeedInfo.clear()
                    newFeedInfo.addAll(feedInfo)

                    _feedInfoLiveData.postValue(updatedFeedInfo)

                    // 기존 Feed ID 리스트에 새로운 ID 추가
                    val updatedFeedIds = _feedIdsLiveData.value?.feedIds?.toMutableList() ?: mutableListOf()
                    updatedFeedIds.addAll(feedIds)

                    newFeedIds.clear()
                    newFeedIds.addAll(feedIds)

                    _feedIdsLiveData.postValue(feedListResponse.copy(feedIds = updatedFeedIds))

                    nextPage = feedListResponse.nextPage    //페이지 정보값 변경
                    lastPage = feedListResponse.lastPage
                    followPage = feedListResponse.followPage
                }

            } catch (e: Exception) {
                throw Exception("fetchPostResults Failed")
            } finally {
                endLoading()
            }
        }
    }

    fun fetchSearchFeedResults(query: String){
        startLoading()

        viewModelScope.launch {
            try{
                if(!lastPage){
                    val feedListResponse = repository.getSearchFeedList(query, nextPage)
                    val feedIds = feedListResponse.feedIds

                    val feedInfo = feedIds.map {
                        async { repository.getFeedInfo(it) }
                    }.awaitAll()

                    // 기존 데이터에 새로운 데이터 추가
                    val updatedFeedInfo = _feedInfoLiveData.value?.toMutableList() ?: mutableListOf()
                    updatedFeedInfo.addAll(feedInfo)

                    newFeedInfo.clear()
                    newFeedInfo.addAll(feedInfo)

                    _feedInfoLiveData.postValue(updatedFeedInfo)

                    // 기존 Feed ID 리스트에 새로운 ID 추가
                    val updatedFeedIds = _feedIdsLiveData.value?.feedIds?.toMutableList() ?: mutableListOf()
                    updatedFeedIds.addAll(feedIds)

                    newFeedIds.clear()
                    newFeedIds.addAll(feedIds)

                    _feedIdsLiveData.postValue(feedListResponse.copy(feedIds = updatedFeedIds))

                    nextPage = feedListResponse.nextPage
                    lastPage = feedListResponse.lastPage
                }
            } catch (e: Exception) {
                throw Exception("fetchPostResults Failed")
            } finally {
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