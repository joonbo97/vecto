package com.vecto_example.vecto.ui.userinfo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.launch

class UserInfoViewModel(private val feedRepository: FeedRepository, private val userRepository: UserRepository, private val tokenRepository: TokenRepository) : ViewModel() {
    private val _reissueResponse = MutableLiveData<String>()
    val reissueResponse: LiveData<String> = _reissueResponse

    var accessToken: String? = null
    var refreshToken: String? = null

    var nextPage: Int = 0

    var isFollowRequestFinished = true  //팔로우 요청 완료 확인

    lateinit var complaintRequest: VectoService.ComplaintRequest
    var postFeedLikeId = -1
    var deleteFeedLikeId = -1
    var deleteFeedId = -1

    var firstFlag = true

    var lastPage: Boolean = false
    var followPage: Boolean = true

    /*   로딩   */
    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    var postLikeLoading = false
    var deleteLikeLoading = false

    /*   게시글   */
    private val _feedInfoLiveData = MutableLiveData<VectoService.FeedPageResponse>()
    val feedInfoLiveData: LiveData<VectoService.FeedPageResponse> = _feedInfoLiveData

    var allFeedInfo = mutableListOf<VectoService.FeedInfo>()

    /*   사용자 정보   */
    private val _userInfo = MutableLiveData<VectoService.UserInfoResponse>()
    val userInfo: LiveData<VectoService.UserInfoResponse> = _userInfo

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

    /*   로그 아웃   */
    private val _postLogoutResult = MutableLiveData<Boolean>()
    val postLogoutResult: LiveData<Boolean> = _postLogoutResult

    /*   에러   */
    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    enum class Function {
        FetchUserFeedResults, CheckFollow, PostFollow, DeleteFollow, PostComplaint, PostFeedLike, DeleteFeedLike, DeleteFeed, PostLogout
    }

    private fun startLoading(){
        if(firstFlag)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.value = true
        else                //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.value = true
    }

    private fun startCenterLoading(){
        _isLoadingCenter.value = true
    }

    fun endLoading(){
        _isLoadingCenter.value = false
        _isLoadingBottom.value = false

        postLikeLoading = false
        deleteLikeLoading = false
    }

    fun fetchUserFeedResults(userId: String){
        if(!lastPage)
            startLoading()

        viewModelScope.launch {
            if(!lastPage){
                val feedListResponse: Result<VectoService.FeedPageResponse>

                if(Auth.loginFlag.value == true)
                    feedListResponse = feedRepository.postUserFeedList(userId, nextPage)
                else
                    feedListResponse = feedRepository.getUserFeedList(userId, nextPage)

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
                    when(it.message){
                        ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                            reissueToken(Function.FetchUserFeedResults.name)
                        }
                        ServerResponse.ERROR.code -> {
                            _errorMessage.postValue(R.string.APIErrorToastMessage)
                            endLoading()
                        }
                        else -> {
                            _errorMessage.postValue(R.string.get_feed_fail)
                            endLoading()
                        }
                    }
                }
            }
        }
    }

    fun getUserInfo(userId: String){
        viewModelScope.launch {
            val userInfoResponse = userRepository.getUserInfo(userId)

            userInfoResponse.onSuccess {
                _userInfo.postValue(it)
            }.onFailure {
                when(it.message){
                    ServerResponse.FAIL_GET_USERINFO.code -> {
                        _errorMessage.postValue(R.string.login_none)
                        endLoading()
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        endLoading()
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                        endLoading()
                    }
                }
            }

        }
    }

    fun checkFollow(userId: String) {
        val userIdList = mutableListOf<String>()
        userIdList.add(userId)

        viewModelScope.launch {
            val followResponse = userRepository.getFollowRelation(userIdList)

            followResponse.onSuccess { followStatuses ->
                _isFollowing.postValue(followStatuses.followRelations[0].relation == "followed" || followStatuses.followRelations[0].relation == "all")
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.CheckFollow.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        _isFollowing.value = false
                        endLoading()
                    }
                    else -> {
                        _errorMessage.postValue(R.string.get_follow_relation_fail)
                        _isFollowing.value = false
                        endLoading()
                    }
                }
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
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.PostFollow.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        isFollowRequestFinished = true
                    }
                    else -> {
                        _errorMessage.postValue(R.string.post_follow_fail)
                        isFollowRequestFinished = true
                    }
                }
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
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.DeleteFollow.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        isFollowRequestFinished = true
                    }
                    else -> {
                        _errorMessage.postValue(R.string.delete_follow_fail)
                        isFollowRequestFinished = true
                    }
                }
            }
        }
    }

    fun postComplaint(complaintRequest: VectoService.ComplaintRequest) {
        this.complaintRequest = complaintRequest

        viewModelScope.launch {
            val followResponse = userRepository.postComplaint(complaintRequest)

            followResponse.onSuccess {
                _postComplaintResult.value = true
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.PostComplaint.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                }
            }
        }
    }

    fun postLogout(){
        startLoading()

        viewModelScope.launch {
            val logoutResponse = userRepository.postLogout()

            logoutResponse.onSuccess {
                _postLogoutResult.postValue(true)
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.PostLogout.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                }
            }
        }
    }

    fun postFeedLike(feedId: Int) {
        postLikeLoading = true
        postFeedLikeId = feedId

        viewModelScope.launch {
            val postFeedLikeResponse = feedRepository.postFeedLike(feedId)

            postFeedLikeResponse.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.PostFeedLike.name)
                    return@launch
                }
            }

            _postFeedLikeResult.value = postFeedLikeResponse

            endLoading()
        }
    }

    fun deleteFeedLike(feedId: Int) {
        deleteLikeLoading = true
        deleteFeedLikeId = feedId

        viewModelScope.launch {
            val deleteFeedLikeResponse = feedRepository.deleteFeedLike(feedId)

            deleteFeedLikeResponse.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.DeleteFeedLike.name)
                    return@launch
                }
            }

            _deleteFeedLikeResult.value = deleteFeedLikeResponse

            endLoading()
        }
    }

    fun deleteFeed(feedId: Int) {
        deleteFeedId = feedId

        startCenterLoading()
        viewModelScope.launch {
            val deleteFeedResponse = feedRepository.deleteFeed(feedId)

            deleteFeedResponse.onFailure {
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.DeleteFeed.name)
                    return@launch
                }
            }

            _deleteFeedResult.value = deleteFeedResponse

            endLoading()
        }
    }

    private fun reissueToken(function: String){
        viewModelScope.launch {
            val reissueResponse = tokenRepository.reissueToken()

            reissueResponse.onSuccess { //Access Token이 만료되어서 갱신됨
                accessToken = it.accessToken
                refreshToken = it.refreshToken
                _reissueResponse.postValue(function)
            }.onFailure {
                when(it.message){
                    //아직 유효한 경우
                    ServerResponse.ACCESS_TOKEN_VALID_ERROR.code -> {}
                    //Refresh Token 만료
                    ServerResponse.REFRESH_TOKEN_INVALID_ERROR.code -> {
                        _errorMessage.postValue(R.string.expired_login)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                }
                endLoading()
            }
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
        return !(isLoadingBottom.value == false && isLoadingCenter.value == false)
    }
}