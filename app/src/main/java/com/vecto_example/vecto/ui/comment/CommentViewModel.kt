package com.vecto_example.vecto.ui.comment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.launch

class CommentViewModel(private val repository: CommentRepository): ViewModel() {

    private val _commentInfoLiveData = MutableLiveData<VectoService.CommentListResponse>()
    val commentInfoLiveData: LiveData<VectoService.CommentListResponse> = _commentInfoLiveData

    fun fetchCommentResults(feedId: Int){
        //startLoading()

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
                //endLoading()
            }
        }
    }

    fun fetchPersonalCommentResults(feedId: Int) {
        //startLoading()

        viewModelScope.launch {
            try {
                val commentListResponse = repository.getPersonalCommentList(feedId)

                _commentInfoLiveData.postValue(commentListResponse)

            } catch (e: Exception) {
                Log.e("fetchCommentResultsError", "Failed to load comment", e)
            } finally {
                //endLoading()
            }
        }
    }

}