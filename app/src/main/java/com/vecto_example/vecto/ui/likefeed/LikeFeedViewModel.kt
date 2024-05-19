package com.vecto_example.vecto.ui.likefeed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.detail.FeedDetailViewModel
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.launch

class LikeFeedViewModel(private val feedRepository: FeedRepository, private val userRepository: UserRepository, private val tokenRepository: TokenRepository) : ViewModel() {
    private val _reissueResponse = MutableLiveData<String>()
    val reissueResponse: LiveData<String> = _reissueResponse

    var accessToken: String? = null
    var refreshToken: String? = null

    var firstFlag = true    //처음 게시글 정보를 받아볼 경우 확인을 위한 Flag

    /*   로딩 관련   */
    private val _isLoadingCenter = MutableLiveData<Boolean>()
    val isLoadingCenter: LiveData<Boolean> = _isLoadingCenter

    private val _isLoadingBottom = MutableLiveData<Boolean>()
    val isLoadingBottom: LiveData<Boolean> = _isLoadingBottom

    var postLikeLoading = false
    var deleteLikeLoading = false
    var postFollowLoading = false
    var deleteFollowLoading = false

    var postFeedLikeId = -1
    var deleteFeedLikeId = -1
    var postFollowId = ""
    var deleteFollowId = ""

    /*   게시글 정보   */
    var nextPage: Int = 0
    var lastPage: Boolean = false
    var followPage: Boolean = true

    private val _feedInfoLiveData = MutableLiveData<VectoService.FeedPageResponse>()
    val feedInfoLiveData: LiveData<VectoService.FeedPageResponse> = _feedInfoLiveData

    var allFeedInfo = mutableListOf<VectoService.FeedInfoWithFollow>()

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
    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    //팔로우 정보 조회 실패
    private val _followErrorLiveData = MutableLiveData<String>()
    val followErrorLiveData: LiveData<String> = _followErrorLiveData

    //팔로우 요청 실패
    private val _postFollowError = MutableLiveData<String>()
    val postFollowError: LiveData<String> = _postFollowError

    //팔로우 삭제 요청 실패
    private val _deleteFollowError = MutableLiveData<String>()
    val deleteFollowError: LiveData<String> = _deleteFollowError

    enum class Function {
        GetLikeFeedList, PostFeedLike, DeleteFeedLike, PostFollow, DeleteFollow
    }


    private fun startLoading(){
        Log.d("MyPageLikeFeedViewModel", "Start Loading")

        if(firstFlag)   //처음 실행하는 경우 center 로딩
            _isLoadingCenter.value = true
        else            //하단 스크롤인 경우 bottom 로딩
            _isLoadingBottom.value = true
    }

    fun endLoading(){
        Log.d("MyPageLikeFeedViewModel", "End Loading")

        _isLoadingCenter.value = false
        _isLoadingBottom.value = false

        postLikeLoading = false
        deleteLikeLoading = false
        postFollowLoading = false
        deleteFollowLoading = false
    }

    fun getLikeFeedList(){
        if(!lastPage)
            startLoading()

        viewModelScope.launch {
            if(!lastPage){
                val feedListResponse = feedRepository.postLikeFeedList(nextPage)

                feedListResponse.onSuccess {

                    val newFeedInfoWithFollow = it.feeds.map { feedInfo ->
                        VectoService.FeedInfoWithFollow(feedInfo, false)
                    }

                    checkFollow(newFeedInfoWithFollow, it)

                    nextPage = it.nextPage    //페이지 정보값 변경
                    lastPage = it.lastPage
                    followPage = it.followPage
                }.onFailure {
                    when(it.message){
                        ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                            reissueToken(FeedDetailViewModel.Function.GetFeedList.name)
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

                if(firstFlag) {
                    allFeedInfo = newFeedInfoWithFollow.toMutableList()
                } else {
                    allFeedInfo.addAll(newFeedInfoWithFollow)
                }
                _feedInfoLiveData.postValue(feedPageResponse)

            }.onFailure {
                if(firstFlag) {
                    allFeedInfo = newFeedInfoWithFollow.toMutableList()
                } else {
                    allFeedInfo.addAll(newFeedInfoWithFollow)
                }
                _feedInfoLiveData.postValue(feedPageResponse)

                _followErrorLiveData.value = it.message
                endLoading()
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

    fun postFollow(userId: String) {
        postFollowLoading = true

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
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.PostFollow.name)
                    return@launch
                }

                _postFollowError.value = it.message

                endLoading()
            }
        }
    }

    fun deleteFollow(userId: String) {
        deleteFollowLoading = true

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
                if(it.message == ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code){
                    reissueToken(Function.DeleteFollow.name)
                    return@launch
                }

                _deleteFollowError.value = it.message

                endLoading()
            }
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