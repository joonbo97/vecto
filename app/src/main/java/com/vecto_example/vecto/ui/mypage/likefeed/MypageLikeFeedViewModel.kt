package com.vecto_example.vecto.ui.mypage.likefeed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.launch

class MypageLikeFeedViewModel(private val repository: FeedRepository, private val userRepository: UserRepository) : ViewModel() {
    var firstFlag = true    //처음 게시글 정보를 받아볼 경우 확인을 위한 Flag

    /*   로딩 관련   */
    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    private var tempLoading = false

    /*   게시글 정보   */
    var nextPage: Int = 0
    var lastPage: Boolean = false
    var followPage: Boolean = true

    private val _feedInfoLiveData = MutableLiveData<VectoService.FeedPageResponse>()
    val feedInfoLiveData: LiveData<VectoService.FeedPageResponse> = _feedInfoLiveData

    val allFeedInfo = mutableListOf<VectoService.FeedInfoWithFollow>()

    /*   좋아요   */
    private val _postFeedLikeResult = MutableLiveData<Result<String>>()
    val postFeedLikeResult: LiveData<Result<String>> = _postFeedLikeResult

    private val _deleteFeedLikeResult = MutableLiveData<Result<String>>()
    val deleteFeedLikeResult: LiveData<Result<String>> = _deleteFeedLikeResult

    //팔로우 요청
    private val _postFollowResult = MutableLiveData<Boolean>()
    val postFollowResult: LiveData<Boolean> = _postFollowResult

    //팔로우 취소
    private val _deleteFollowResult = MutableLiveData<Boolean>()
    val deleteFollowResult: LiveData<Boolean> = _deleteFollowResult

    /*   에러   */
    //피드 정보 요청 오류
    private val _feedErrorLiveData = MutableLiveData<String>()
    val feedErrorLiveData: LiveData<String> = _feedErrorLiveData

    //팔로우 정보 조회 실패
    private val _followErrorLiveData = MutableLiveData<String>()
    val followErrorLiveData: LiveData<String> = _followErrorLiveData

    //팔로우 요청 실패
    private val _postFollowError = MutableLiveData<String>()
    val postFollowError: LiveData<String> = _postFollowError

    //팔로우 삭제 요청 실패
    private val _deleteFollowError = MutableLiveData<String>()
    val deleteFollowError: LiveData<String> = _deleteFollowError


    private fun startLoading(){
        Log.d("MyPageLikeFeedViewModel", "Start Loading")

        if(nextPage == 0)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.value = true
        else                //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.value = true
    }

    private fun endLoading(){
        Log.d("MyPageLikeFeedViewModel", "End Loading")

        _isLoadingCenter.value = false
        _isLoadingBottom.value = false

        tempLoading = false
    }

    fun getLikeFeedList(){
        if(!lastPage)
            startLoading()

        viewModelScope.launch {
            val feedListResponse = repository.postLikeFeedList(nextPage)

            feedListResponse.onSuccess {
                val newFeedInfoWithFollow = it.feeds.map { feedInfo ->
                    VectoService.FeedInfoWithFollow(feedInfo, false)
                }

                checkFollow(newFeedInfoWithFollow, it)

                nextPage = it.nextPage    //페이지 정보값 변경
                lastPage = it.lastPage
                followPage = it.followPage

                endLoading()
            }.onFailure {
                _feedErrorLiveData.value = it.message
                endLoading()
            }

        }

    }

    private fun checkFollow(newFeedInfoWithFollow: List<VectoService.FeedInfoWithFollow>, feedPageResponse: VectoService.FeedPageResponse){

        val userIdList = newFeedInfoWithFollow.map {
            it.feedInfo.userId
        }
        viewModelScope.launch {
            val followResponse = userRepository.getFollowRelation(userIdList)

            followResponse.onSuccess { followStatuses ->

                for(i in 0 until followStatuses.followRelations.size){
                    if(followStatuses.followRelations[i].relation == "followed" || followStatuses.followRelations[i].relation == "all")
                        newFeedInfoWithFollow[i].isFollowing = true
                }

                allFeedInfo.addAll(newFeedInfoWithFollow)
                _feedInfoLiveData.postValue(feedPageResponse)

                endLoading()
            }.onFailure {
                allFeedInfo.addAll(newFeedInfoWithFollow)
                _feedInfoLiveData.postValue(feedPageResponse)

                _followErrorLiveData.value = it.message
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

    fun postFollow(userId: String) {
        tempLoading = true

        viewModelScope.launch {
            val followResponse = userRepository.postFollow(userId)

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
            val followResponse = userRepository.deleteFollow(userId)

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

    fun initSetting(){

        nextPage = 0
        lastPage = false
        followPage = true

        firstFlag = true

        allFeedInfo.clear()
    }

    fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false && !tempLoading)
    }
}