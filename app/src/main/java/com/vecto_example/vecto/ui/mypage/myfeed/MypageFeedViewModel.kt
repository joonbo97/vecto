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
import kotlinx.coroutines.launch

class MypageFeedViewModel(private val repository: FeedRepository) : ViewModel() {
    var nextPage: Int = 0
    private var lastPage: Boolean = false
    private var followPage: Boolean = true

    val allFeedIds = mutableListOf<Int>()
    val allFeedInfo = mutableListOf<VectoService.FeedInfoResponse>()

    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    private var tempLoading = false

    private val _feedIdsLiveData = MutableLiveData<VectoService.FeedPageResponse>()
    val feedIdsLiveData: LiveData<VectoService.FeedPageResponse> = _feedIdsLiveData

    private val _feedInfoLiveData = MutableLiveData<List<VectoService.FeedInfoResponse>>()
    val feedInfoLiveData: LiveData<List<VectoService.FeedInfoResponse>> = _feedInfoLiveData

    private val _feedErrorLiveData = MutableLiveData<String>()
    val feedErrorLiveData: LiveData<String> = _feedErrorLiveData

    /*   좋아요   */
    private val _postFeedLikeResult = MutableLiveData<Result<String>>()
    val postFeedLikeResult: LiveData<Result<String>> = _postFeedLikeResult

    private val _deleteFeedLikeResult = MutableLiveData<Result<String>>()
    val deleteFeedLikeResult: LiveData<Result<String>> = _deleteFeedLikeResult

    /*   게시글 삭제   */
    private val _deleteFeedResult = MutableLiveData<Result<String>>()
    val deleteFeedResult: LiveData<Result<String>> = _deleteFeedResult


    private fun startLoading(){
        Log.d("STARTLOADING", "START")

        if(nextPage == 0)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.value = true
        else                //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.value = true
    }

    private fun startCenterLoading(){
        _isLoadingCenter.value = true
    }

    private fun endLoading(){
        Log.d("ENDLOADING", "END")

        _isLoadingCenter.value = false
        _isLoadingBottom.value = false
        tempLoading = false
    }

    fun fetchUserFeedResults(){
        startLoading()

        viewModelScope.launch {
            val feedListResponse = repository.getUserFeedList(Auth._userId.value.toString(), nextPage)

            feedListResponse.onSuccess { feedPageResponse ->
                if(!lastPage) {
                    feedIdsLiveData.value?.let { allFeedIds.addAll(it.feedIds) }
                    feedInfoLiveData.value?.let { allFeedInfo.addAll(it) }

                    val successfulFeedIds = mutableListOf<Int>()
                    val feedInfo = mutableListOf<VectoService.FeedInfoResponse>()

                    feedPageResponse.feedIds.forEach { feedId ->
                        val job = async {
                            try {
                                repository.getFeedInfo(feedId).also {
                                    successfulFeedIds.add(feedId)
                                }
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
                    _feedIdsLiveData.postValue(feedPageResponse.copy(feedIds = successfulFeedIds))

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

    fun postFeedLike(feedId: Int) {
        tempLoading = true

        viewModelScope.launch {
            val postFeedLikeResponse = repository.postFeedLike(feedId)

            _postFeedLikeResult.value = postFeedLikeResponse

            endLoading()
        }
    }

    fun deleteFeedLike(feedId: Int) {
        tempLoading = true

        viewModelScope.launch {
            val deleteFeedLikeResponse = repository.deleteFeedLike(feedId)

            _deleteFeedLikeResult.value = deleteFeedLikeResponse

            endLoading()
        }
    }

    fun deleteFeed(feedId: Int) {
        startCenterLoading()
        viewModelScope.launch {
            val deleteFeedResponse = repository.deleteFeed(feedId)

            _deleteFeedResult.value = deleteFeedResponse

            endLoading()
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
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false && !tempLoading)
    }
}