package com.vecto_example.vecto.ui.mypage.myfeed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class MypageFeedViewModel(private val repository: FeedRepository) : ViewModel() {
    var nextPage: Int = 0
    var lastPage: Boolean = false
    var followPage: Boolean = true

    val allFeedIds = mutableListOf<Int>()
    val allFeedInfo = mutableListOf<VectoService.FeedInfoResponse>()

    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
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

    fun fetchUserFeedResults(){
        startLoading()

        viewModelScope.launch {
            val feedListResponse = repository.getUserFeedList(Auth._userId.value.toString(), nextPage)

            feedListResponse.onSuccess { feedPageResponse ->
                if(!lastPage) {
                    feedIdsLiveData.value?.let { allFeedIds.addAll(it.feedIds) }
                    feedInfoLiveData.value?.let { allFeedInfo.addAll(it) }

                    val feedInfo = mutableListOf<VectoService.FeedInfoResponse>()

                    feedPageResponse.feedIds.forEach { feedId ->
                        val job = async {
                            try {
                                repository.getFeedInfo(feedId)
                            } catch (e: Exception) {
                                Log.e("fetchUserFeedResults", "Failed to fetch feed info for ID $feedId", e)
                                null // 실패한 경우 null 반환
                            }
                        }
                        job.await()?.let {
                            feedInfo.add(it) // null이 아닌 결과만 추가
                        }
                    }
                    _feedInfoLiveData.postValue(feedInfo)   //LiveData 값 변경
                    _feedIdsLiveData.postValue(feedPageResponse)

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

        allFeedIds.clear()
        allFeedInfo.clear()
    }

    fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false)
    }
}