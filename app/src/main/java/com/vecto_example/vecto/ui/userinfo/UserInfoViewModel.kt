package com.vecto_example.vecto.ui.userinfo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class UserInfoViewModel(private val repository: FeedRepository, private val userRepository: UserRepository) : ViewModel() {
    var nextPage: Int = 0

    var isFollowRequestFinished = true  //팔로우 요청 완료 확인

    private val _userInfo = MutableLiveData<VectoService.UserInfoResponse>()
    val userInfo: LiveData<VectoService.UserInfoResponse> = _userInfo

    private var lastPage: Boolean = false
    private var followPage: Boolean = true

    val allFeedIds = mutableListOf<Int>()
    val allFeedInfo = mutableListOf<VectoService.FeedInfoResponse>()

    /*   로딩   */
    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    /*   게시글   */
    private val _feedIdsLiveData = MutableLiveData<VectoService.FeedPageResponse>()
    val feedIdsLiveData: LiveData<VectoService.FeedPageResponse> = _feedIdsLiveData

    private val _feedInfoLiveData = MutableLiveData<List<VectoService.FeedInfoResponse>>()
    val feedInfoLiveData: LiveData<List<VectoService.FeedInfoResponse>> = _feedInfoLiveData

    /*   사용자 정보   */
    private val _userInfoResult = MutableLiveData<Result<VectoService.UserInfoResponse>>()
    val userInfoResult: LiveData<Result<VectoService.UserInfoResponse>> = _userInfoResult

    /*   팔로우   */
    //팔로우 여부
    private val _isFollowing = MutableLiveData<Boolean>()
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
    private val _getFollowError = MutableLiveData<String>()
    val getFollowError: LiveData<String> = _getFollowError

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

        if(nextPage == 0)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.value = true
        else                //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.value = true
    }

    private fun endLoading(){
        Log.d("UserInfoViewModel", "Loading End")

        _isLoadingCenter.value = false
        _isLoadingBottom.value = false
    }

    fun fetchUserFeedResults(userId: String){
        startLoading()

        viewModelScope.launch {
            val feedListResponse = repository.getUserFeedList(userId, nextPage)

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
                    followPage = feedPageResponse.followPage
                }
                endLoading()
            }.onFailure {
                _feedErrorLiveData.value = it.message
                endLoading()
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

    fun getFollow(userId: String) {

        viewModelScope.launch {
            val followResponse = userRepository.getFollow(userId)

            followResponse.onSuccess {
                if(it == ServerResponse.SUCCESS_GETFOLLOW_FOLLOWING.code)
                    _isFollowing.value = true
                else if(it == ServerResponse.SUCCESS_GETFOLLOW_UNFOLLOWING.code)
                    _isFollowing.value = false
            }.onFailure {
                _getFollowError.value = it.message
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