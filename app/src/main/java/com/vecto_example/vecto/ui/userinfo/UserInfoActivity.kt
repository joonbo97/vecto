package com.vecto_example.vecto.ui.userinfo

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityUserInfoBinding
import com.vecto_example.vecto.popupwindow.ReportPopupWindow
import com.vecto_example.vecto.dialog.ReportUserDialog
import com.vecto_example.vecto.dialog.UserProfileImageDialog
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.followinfo.FollowInfoActivity
import com.vecto_example.vecto.ui.main.MainActivity
import com.vecto_example.vecto.ui.myfeed.adapter.MyFeedAdapter
import com.vecto_example.vecto.utils.FeedDetailType
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.RequestLoginUtils
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ServerResponse
import com.vecto_example.vecto.utils.ShareFeedUtil
import com.vecto_example.vecto.utils.ToastMessageUtils
import kotlinx.coroutines.launch

class UserInfoActivity : AppCompatActivity(), MyFeedAdapter.OnFeedActionListener{
    lateinit var binding: ActivityUserInfoBinding

    private val userInfoViewModel: UserInfoViewModel by viewModels {
        UserInfoViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()), TokenRepository(VectoService.create()))
    }

    private lateinit var myFeedAdapter: MyFeedAdapter

    private var userId = ""
    private var originalLoginFlag = Auth.loginFlag.value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.getStringExtra("userId")?.let {
            userId = it
            userInfoViewModel.getUserInfo(it)

            Log.d("UserInfoActivity", "Init getFeed")
            getFeed(it)

            if(userId == Auth.userId.value) {
                binding.MenuIcon.visibility = View.GONE

                binding.FollowButton.setBackgroundResource(R.drawable.ripple_effect_following)
                binding.FollowButtonText.text = "마이페이지"
            }
        }

        initRecyclerView()
        initObservers()
        initListeners()

        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {

            if(!userInfoViewModel.checkLoading()){

                userInfoViewModel.initSetting()

                Log.d("UserInfoActivity", "By Swipe getFeed")
                getFeed(userId)
            }

            swipeRefreshLayout.isRefreshing = false

        }

    }

    private fun initListeners() {

        binding.MenuIcon.setOnClickListener {
            val reportPopupWindow = ReportPopupWindow(this,
                reportListener = {
                    if (Auth.loginFlag.value == false) {
                        RequestLoginUtils.requestLogin(this)
                        return@ReportPopupWindow
                    }

                    val reportUserDialog = ReportUserDialog(this)
                    reportUserDialog.showDialog()
                    reportUserDialog.onOkButtonClickListener = { selectedOptionId, detailContent ->
                        when(selectedOptionId) {
                            R.id.radioButton0 -> {
                                userInfoViewModel.postComplaint(VectoService.ComplaintRequest(ServerResponse.REPORT_TYPE_BAD_MANNER.code, userId, null))
                            }

                            R.id.radioButton1 -> {
                                userInfoViewModel.postComplaint(VectoService.ComplaintRequest(ServerResponse.REPORT_TYPE_INSULT.code, userId, null))
                            }
                            R.id.radioButton2 -> {
                                userInfoViewModel.postComplaint(VectoService.ComplaintRequest(ServerResponse.REPORT_TYPE_SEXUAL_HARASSMENT.code, userId, null))
                            }
                            R.id.radioButton3 -> {
                                if (detailContent != null) {
                                    userInfoViewModel.postComplaint(VectoService.ComplaintRequest(ServerResponse.REPORT_TYPE_OTHER_PROBLEM.code, userId, detailContent))
                                }
                                else{
                                    userInfoViewModel.postComplaint(VectoService.ComplaintRequest(ServerResponse.REPORT_TYPE_OTHER_PROBLEM.code, userId, null))
                                }
                            }
                        }
                    }
                } ,
                dismissListener = {

                }
            )

            // 앵커 뷰를 기준으로 팝업 윈도우 표시
            reportPopupWindow.showPopupWindow(binding.MenuIcon)
        }

        binding.FollowButton.setOnClickListener {
            if(Auth.loginFlag.value == false)
            {
                RequestLoginUtils.requestLogin(this)
            } else if(Auth.userId.value == userId){
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                intent.putExtra("MyPage", R.id.MypageFragment)
                startActivity(intent)
                finish()
            } else {
                if (!userInfoViewModel.isFollowRequestFinished) {
                    ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
                } else {
                    if (userInfoViewModel.isFollowing.value == null) {  //이전 팔로우 정보 확인 실패한 경우
                        userInfoViewModel.checkFollow(userId)
                    } else {
                        if (!userInfoViewModel.isFollowing.value!!) {
                            userInfoViewModel.postFollow(userId)
                        } else {
                            userInfoViewModel.deleteFollow(userId)
                        }
                    }
                }
            }

        }

        binding.FollowerTouchImage.setOnClickListener {
            val intent = Intent(this, FollowInfoActivity::class.java)
            intent.putExtra("userId", userInfoViewModel.userInfo.value?.userId)
            intent.putExtra("nickName", userInfoViewModel.userInfo.value?.nickName)
            intent.putExtra("type", "follower")
            intent.putExtra("follower", userInfoViewModel.userInfo.value?.followerCount)
            intent.putExtra("following", userInfoViewModel.userInfo.value?.followingCount)
            startActivity(intent)
        }

        binding.FollowingTouchImage.setOnClickListener {
            val intent = Intent(this, FollowInfoActivity::class.java)
            intent.putExtra("userId", userInfoViewModel.userInfo.value?.userId)
            intent.putExtra("nickName", userInfoViewModel.userInfo.value?.nickName)
            intent.putExtra("type", "following")
            intent.putExtra("follower", userInfoViewModel.userInfo.value?.followerCount)
            intent.putExtra("following", userInfoViewModel.userInfo.value?.followingCount)
            startActivity(intent)
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initObservers() {
        Auth.loginFlag.observe(this){
            if(Auth.loginFlag.value != originalLoginFlag) {

                userInfoViewModel.initSetting()

                Log.d("UserInfoActivity", "LoginFlag Observer getFeed")
                getFeed(userId)

                originalLoginFlag = Auth.loginFlag.value

            }
        }

        /*   게시글 관련 Observer   */
        userInfoViewModel.feedInfoLiveData.observe(this) {
            if(userInfoViewModel.firstFlag) {
                myFeedAdapter.feedInfo = userInfoViewModel.allFeedInfo
                myFeedAdapter.lastSize = userInfoViewModel.allFeedInfo.size

                myFeedAdapter.notifyDataSetChanged()
                userInfoViewModel.firstFlag = false

                userInfoViewModel.endLoading()
            } else {
                myFeedAdapter.addFeedInfoData()

                userInfoViewModel.endLoading()
            }

            if(userInfoViewModel.allFeedInfo.isEmpty()){
                setNoneImage()
            } else {
                clearNoneImage()
            }
        }

        /*   로딩 관련 Observer   */
        userInfoViewModel.isLoadingCenter.observe(this) {
            if(it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else
                binding.progressBarCenter.visibility = View.GONE
        }
        userInfoViewModel.isLoadingBottom.observe(this) {
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.GONE
        }

        /*   사용자 정보 Observer   */
        userInfoViewModel.userInfo.observe(this) {
            if(it.userId.isNotEmpty()) {
                setUserProfile(it)
            }
        }

        /*   게시글 좋아요 Observer   */
        userInfoViewModel.postFeedLikeResult.observe(this) { postFeedLikeResult ->
            postFeedLikeResult.onSuccess {
                myFeedAdapter.postFeedLikeSuccess()
            }.onFailure {
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
            }

            myFeedAdapter.postLikePosition = -1
        }

        userInfoViewModel.deleteFeedLikeResult.observe(this) { deleteFeedLikeResult ->
            deleteFeedLikeResult.onSuccess {
                myFeedAdapter.deleteFeedLikeSuccess()
            }.onFailure {
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
            }

            myFeedAdapter.deleteLikePosition = -1
        }

        /*   게시글 삭제   */
        userInfoViewModel.deleteFeedResult.observe(this) { deleteFeedResult ->
            deleteFeedResult.onSuccess {
                myFeedAdapter.deleteFeedSuccess()
                ToastMessageUtils.showToast(this, getString(R.string.delete_feed_success))
            }.onFailure {
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
            }

            myFeedAdapter.deleteFeedPosition = -1
        }

        /*   Follow 정보 Observer   */
        userInfoViewModel.isFollowing.observe(this) {
            setFollowButton(it)
        }

        userInfoViewModel.postFollowResult.observe(this) {
            if(it){
                ToastMessageUtils.showToast(this, getString(R.string.post_follow_success, binding.UserNameText.text))
            } else {
                ToastMessageUtils.showToast(this, getString(R.string.post_follow_already, binding.UserNameText.text))
            }
        }

        userInfoViewModel.deleteFollowResult.observe(this) {
            if(it){
                ToastMessageUtils.showToast(this, getString(R.string.delete_follow_success, binding.UserNameText.text))
            } else {
                ToastMessageUtils.showToast(this, getString(R.string.delete_follow_already, binding.UserNameText.text))
            }
        }

        /*   신고 Observer   */
        userInfoViewModel.postComplaintResult.observe(this) {
            if(it) {
                ToastMessageUtils.showToast(this, getString(R.string.report_success))
            }
        }

        lifecycleScope.launch {
            userInfoViewModel.reissueResponse.collect {
                SaveLoginDataUtils.changeToken(this@UserInfoActivity, it.userToken.accessToken, it.userToken.refreshToken)

                when(it.function){
                    UserInfoViewModel.Function.FetchUserFeedResults.name -> {
                        getFeed(userId)
                    }
                    UserInfoViewModel.Function.CheckFollow.name -> {
                        userInfoViewModel.checkFollow(userId)
                    }
                    UserInfoViewModel.Function.PostFollow.name -> {
                        userInfoViewModel.postFollow(userId)
                    }
                    UserInfoViewModel.Function.DeleteFollow.name -> {
                        userInfoViewModel.deleteFollow(userId)
                    }
                    UserInfoViewModel.Function.PostComplaint.name -> {
                        userInfoViewModel.postComplaint(userInfoViewModel.complaintRequest)
                    }
                    UserInfoViewModel.Function.PostFeedLike.name -> {
                        userInfoViewModel.postFeedLike(userInfoViewModel.postFeedLikeId)
                    }
                    UserInfoViewModel.Function.DeleteFeedLike.name -> {
                        userInfoViewModel.deleteFeedLike(userInfoViewModel.deleteFeedLikeId)
                    }
                    UserInfoViewModel.Function.DeleteFeed.name -> {
                        userInfoViewModel.deleteFeed(userInfoViewModel.deleteFeedId)
                    }
                }
            }
        }



        /*   오류 관련 Observer   */
        userInfoViewModel.errorMessage.observe(this){
            ToastMessageUtils.showToast(this, getString(it))

            if(it == R.string.expired_login) {
                SaveLoginDataUtils.deleteData(this)
            }
        }
    }

    private fun initRecyclerView(){
        myFeedAdapter = MyFeedAdapter()
        myFeedAdapter.feedActionListener = this

        val postRecyclerView = binding.UserPostRecyclerView
        postRecyclerView.adapter = myFeedAdapter
        postRecyclerView.itemAnimator = null

        postRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if(!recyclerView.canScrollVertically(1)) {
                    if(!userInfoViewModel.checkLoading()){
                        Log.d("UserInfoActivity", "By ScrollListener getFeed")
                        getFeed(userId)
                    }
                }
            }
        })
    }

    private fun getFeed(userId: String){
        userInfoViewModel.fetchUserFeedResults(userId)
    }

    //사용자 정보 설정
    private fun setUserProfile(userinfo: VectoService.UserInfoResponse) {
        binding.UserNameText.text = userinfo.nickName

        if (userinfo.profileUrl == null) {
            binding.ProfileImage.setImageResource(R.drawable.profile_basic)
        } else{ //사용자 정의 이미지가 있을 경우
            LoadImageUtils.loadUserProfileImage(this, binding.ProfileImage, userinfo.profileUrl)
        }

        binding.ProfileImage.setOnClickListener {
            UserProfileImageDialog(this, userInfoViewModel.userInfo.value?.profileUrl).showDialog()
        }

        binding.FeedCountText.text = userinfo.feedCount.toString()
        if(userinfo.feedCount == 0)
            binding.NoneText.text = "${userinfo.nickName}님이 작성한 게시물이 없어요!"

        binding.FollowerCount.text = userinfo.followerCount.toString()

        binding.FollowingCount.text = userinfo.followingCount.toString()

        if(Auth.loginFlag.value == true) {
            userInfoViewModel.checkFollow(userinfo.userId)
        }
    }

    //팔로우 버튼 설정
    private fun setFollowButton(isFollowing: Boolean){
        if(userId != Auth.userId.value)
        {
            when(isFollowing){
                true -> {
                    binding.FollowButton.setBackgroundResource(R.drawable.ripple_effect_following)
                    binding.FollowButtonText.text = "팔로잉"
                    binding.FollowButtonText.setTextColor(ContextCompat.getColor(this, R.color.white))
                }

                false -> {
                    binding.FollowButton.setBackgroundResource(R.drawable.ripple_effect_follow)
                    binding.FollowButtonText.text = "팔로우"
                    binding.FollowButtonText.setTextColor(ContextCompat.getColor(this, R.color.vecto_theme_orange))
                }
            }
        }

        binding.FollowButton.visibility = View.VISIBLE
        binding.FollowButtonText.visibility = View.VISIBLE
    }

    //결과 None 이미지
    private fun clearNoneImage() {
        binding.NoneImage.visibility = View.GONE
        binding.NoneText.visibility = View.GONE
        Log.d("NONE GONE", "NONE IMAGE IS GONE")
    }

    private fun setNoneImage() {
        binding.NoneImage.visibility = View.VISIBLE
        binding.NoneText.visibility = View.VISIBLE
        binding.NoneText.text = "${binding.UserNameText.text}님이 작성한 게시물이 없어요!"
        Log.d("NONE SET", "NONE IMAGE SET")
    }

    /*   Adapter CallBack 관련   */

    override fun onPostLike(feedID: Int) {
        if(!userInfoViewModel.postLikeLoading)
            userInfoViewModel.postFeedLike(feedID)
        else{
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            myFeedAdapter.postLikePosition = -1
        }
    }

    override fun onDeleteLike(feedID: Int) {
        if(!userInfoViewModel.deleteLikeLoading)
            userInfoViewModel.deleteFeedLike(feedID)
        else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            myFeedAdapter.deleteLikePosition = -1
        }
    }

    override fun onDeleteFeed(feedID: Int) {
        if(!userInfoViewModel.checkLoading())
            userInfoViewModel.deleteFeed(feedID)
        else{
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            myFeedAdapter.deleteFeedPosition = -1
        }
    }

    override fun onItemViewClick(position: Int) {
        if(position < 0  || position > userInfoViewModel.allFeedInfo.lastIndex)
            return

        var subList = userInfoViewModel.allFeedInfo.subList(position, userInfoViewModel.allFeedInfo.size)
        if(subList.size > 10) {
            subList = subList.subList(0, 10)
        }

        val feedInfoWithFollowList = subList.map {
            VectoService.FeedInfoWithFollow(feedInfo = it, isFollowing = userInfoViewModel.isFollowing.value!!)  // 모든 isFollowing 값을 false로 설정
        }

        val intent = Intent(this, FeedDetailActivity::class.java).apply {
            putExtra("feedInfoListJson", Gson().toJson(feedInfoWithFollowList))
            putExtra("type", FeedDetailType.INTENT_USERINFO.code)
            putExtra("query", "")
            putExtra("nextFeedId", userInfoViewModel.nextFeedId)
            putExtra("followPage", userInfoViewModel.followPage)
            putExtra("lastPage", userInfoViewModel.lastPage)
        }

        this.startActivity(intent)
    }

    override fun onShareClick(feedInfo: VectoService.FeedInfo) {
        ShareFeedUtil.shareFeed(this, feedInfo)
    }
}