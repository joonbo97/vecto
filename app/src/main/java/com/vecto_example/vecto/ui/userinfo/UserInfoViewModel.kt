package com.vecto_example.vecto.ui.userinfo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.launch

class UserInfoViewModel(private val repository: FeedRepository, private val userRepository: UserRepository) : ViewModel() {
    var nextPage: Int = 0

    var isFollowRequestFinished = true  //팔로우 요청 완료 확인

    private val _userInfo = MutableLiveData<VectoService.UserInfoResponse>()
    val userInfo: LiveData<VectoService.UserInfoResponse> = _userInfo

    var firstFlag = true

    var lastPage: Boolean = false
    var followPage: Boolean = true

    /*   로딩   */
    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    private var tempLoading = false

    /*   게시글   */
    private val _feedInfoLiveData = MutableLiveData<VectoService.FeedPageResponse>()
    val feedInfoLiveData: LiveData<VectoService.FeedPageResponse> = _feedInfoLiveData

    var allFeedInfo = mutableListOf<VectoService.FeedInfo>()

    /*   사용자 정보   */
    private val _userInfoResult = MutableLiveData<Result<VectoService.UserInfoResponse>>()
    val userInfoResult: LiveData<Result<VectoService.UserInfoResponse>> = _userInfoResult

    /*   좋아요   */
    private val _postFeedLikeResult = MutableLiveData<Result<String>>()
    val postFeedLikeResult: LiveData<Result<String>> = _postFeedLikeResult

    private val _deleteFeedLikeResult = MutableLiveData<Result<String>>()
    val deleteFeedLikeResult: LiveData<Result<String>> = _deleteFeedLikeResult

    /*   게시글 삭제   */
    private val _deleteFeedResult = MutableLiveData<Result<String>>()
    val deleteFeedResult: LiveData<Result<String>> = _deleteFeedResult

    /*   팔로우   */
    //팔로우 여부
    private val _isFollowing = MutableLiveData<Boolean>(false)
    val isFollowing: LiveData<Boolean> = _isFollowing

    //팔로우 요청
    private val _postFollowResult = MutableLiveData<Boolean>()
    val postFollowResult: LiveData<Boolean> = _postFollowResult

    //팔로우 취소
    private val _deleteFollowResult = MutableLiveData<Boolean>()
    val deleteFollowResult: LiveData<Boolean> = _deleteFollowResult

    /*   신고   */
    private val _postComplaintResult = MutableLiveData<Boolean>()
    val postComplaintResult: LiveData<Boolean> = _postComplaintResult

    /*   에러   */
    private val _getFollowRelationError = MutableLiveData<String>()
    val getFollowRelationError: LiveData<String> = _getFollowRelationError

    private val _postFollowError = MutableLiveData<String>()
    val postFollowError: LiveData<String> = _postFollowError

    private val _deleteFollowError = MutableLiveData<String>()
    val deleteFollowError: LiveData<String> = _deleteFollowError

    private val _postComplaintError = MutableLiveData<String>()
    val postComplaintError: LiveData<String> = _postComplaintError

    private val _feedErrorLiveData = MutableLiveData<String>()
    val feedErrorLiveData: LiveData<String> = _feedErrorLiveData

    private fun startLoading(){
        Log.d("UserInfoViewModel", "Loading Start")

        if(firstFlag)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.value = true
        else                //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.value = true
    }

    private fun startCenterLoading(){
        _isLoadingCenter.value = true
    }

    fun endLoading(){
        Log.d("UserInfoViewModel", "Loading End")

        _isLoadingCenter.value = false
        _isLoadingBottom.value = false

        tempLoading = false
    }

    fun fetchUserFeedResults(userId: String){
        if(!lastPage)
            startLoading()

        viewModelScope.launch {
            if(!lastPage){

                val feedListResponse: Result<VectoService.FeedPageResponse>

                if(Auth.loginFlag.value == true)
                    feedListResponse = repository.postUserFeedList(userId, nextPage)
                else
                    feedListResponse = repository.getUserFeedList(userId, nextPage)

                feedListResponse.onSuccess { feedPageResponse ->

                    if(firstFlag) {
                        allFeedInfo = feedPageResponse.feeds.toMutableList()
                    } else {
                        allFeedInfo.addAll(feedPageResponse.feeds)
                    }
                    _feedInfoLiveData.postValue(feedPageResponse)

                    nextPage = feedPageResponse.nextPage    //페이지 정보값 변경
                    lastPage = feedPageResponse.lastPage
                    followPage = feedPageResponse.followPage

                }.onFailure {
                    _feedErrorLiveData.value = it.message
                    endLoading()
                }

            }
        }
    }

    fun getUserInfo(userId: String){

        viewModelScope.launch {
            val userInfoResponse = userRepository.getUserInfo(userId)

            userInfoResponse.onSuccess {
                _userInfo.value = it
            }.onFailure {
                _userInfoResult.value = userInfoResponse
            }

        }
    }

    fun checkFollow(userId: String) {
        val userIdList = mutableListOf<String>()
        userIdList.add(userId)

        viewModelScope.launch {
            val followResponse = userRepository.getFollowRelation(userIdList)

            followResponse.onSuccess { followStatuses ->

                _isFollowing.value = followStatuses.followRelations[0].relation == "followed" || followStatuses.followRelations[0].relation == "all"

            }.onFailure {
                _getFollowRelationError.value = it.message
            }
        }

    }

    fun postFollow(userId: String) {
        isFollowRequestFinished = false

        viewModelScope.launch {
            val followResponse = userRepository.postFollow(userId)

            followResponse.onSuccess {
                if(it == ServerResponse.SUCCESS_POSTFOLLOW.code){
                    _postFollowResult.value = true
                    _isFollowing.value = true

                    changeFollowerCount(1)
                } else if(it == ServerResponse.SUCCESS_ALREADY_POSTFOLLOW.code) {
                    _postFollowResult.value = false
                }

                isFollowRequestFinished = true
            }.onFailure {
                _postFollowError.value = it.message

                isFollowRequestFinished = true
            }
        }
    }

    private fun changeFollowerCount(changeValue: Int) {
        _userInfo.value = VectoService.UserInfoResponse(
            _userInfo.value!!.userId, _userInfo.value!!.provider, _userInfo.value!!.nickName,
            _userInfo.value!!.email, _userInfo.value!!.profileUrl, _userInfo.value!!.feedCount,
            _userInfo.value!!.followerCount + changeValue, _userInfo.value!!.followingCount)
    }

    fun deleteFollow(userId: String) {
        isFollowRequestFinished = false

        viewModelScope.launch {
            val followResponse = userRepository.deleteFollow(userId)

            followResponse.onSuccess {
                if(it == ServerResponse.SUCCESS_DELETEFOLLOW.code){
                    _deleteFollowResult.value = true
                    _isFollowing.value = false

                    changeFollowerCount(-1)
                } else if(it == ServerResponse.SUCCESS_ALREADY_DELETEFOLLOW.code) {
                    _deleteFollowResult.value = false
                }

                isFollowRequestFinished = true
            }.onFailure {
                _deleteFollowError.value = it.message

                isFollowRequestFinished = true
            }
        }
    }

    fun postComplaint(complaintRequest: VectoService.ComplaintRequest) {

        viewModelScope.launch {
            val followResponse = userRepository.postComplaint(complaintRequest)

            followResponse.onSuccess {
                _postComplaintResult.value = true
            }.onFailure {
                _postComplaintError.value = it.message
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

        firstFlag = true
    }

    fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false && !tempLoading)
    }
}