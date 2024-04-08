package com.vecto_example.vecto.ui.comment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.launch

class CommentViewModel(private val repository: CommentRepository): ViewModel() {

    var nextPage: Int = 0
    var lastPage: Boolean = false

    private val _commentInfoLiveData = MutableLiveData<VectoService.CommentListResponse>()
    val commentInfoLiveData: LiveData<VectoService.CommentListResponse> = _commentInfoLiveData

    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    fun fetchCommentResults(feedId: Int){
        startLoading()

        viewModelScope.launch {
            try {
                /*if(!lastPage) { //마지막 page가 아닐 경우에만 실행
                    feedIdsLiveData.value?.let { allFeedIds.addAll(it.feedIds) }
                    feedInfoLiveData.value?.let { allFeedInfo.addAll(it) }

                    val feedListResponse = repository.getFeedList(nextPage)
                    val feedIds = feedListResponse.feedIds  //요청한 pageNo에 해당하는 Feed Ids

                    val feedInfo = feedIds.map {
                        async { repository.getFeedInfo(it) }
                    }.awaitAll()    //모든 feed info 요청이 완료될 때까지 기다림

                    _feedInfoLiveData.postValue(feedInfo)   //LiveData 값 변경
                    _feedIdsLiveData.postValue(feedListResponse)

                    nextPage = feedListResponse.nextPage    //페이지 정보값 변경
                    lastPage = feedListResponse.lastPage
                }*/

                val commentListResponse = repository.getCommentList(feedId)

                _commentInfoLiveData.postValue(commentListResponse)

            } catch (e: Exception) {
                Log.e("fetchCommentResultsError", "Failed to load comment", e)
            } finally {
                endLoading()
            }
        }
    }

    fun fetchPersonalCommentResults(feedId: Int) {
        startLoading()

        viewModelScope.launch {
            try {
                val commentListResponse = repository.getPersonalCommentList(feedId)

                _commentInfoLiveData.postValue(commentListResponse)

            } catch (e: Exception) {
                Log.e("fetchCommentResultsError", "Failed to load comment", e)
            } finally {
                endLoading()
            }
        }
    }

    fun initSetting(){
        Log.d("COMMENT_VIEWMODEL", "initSetting")

        nextPage = 0
        lastPage = false

        _commentInfoLiveData.postValue(VectoService.CommentListResponse(emptyList()))
    }

    fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false)
    }

    private fun startLoading(){
        if(nextPage == 0)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.postValue(true)
        else                //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.postValue(true)
    }

    private fun endLoading(){
        _isLoadingCenter.postValue(false)
        _isLoadingBottom.postValue(false)
    }

}