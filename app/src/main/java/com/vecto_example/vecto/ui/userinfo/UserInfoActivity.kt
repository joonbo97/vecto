package com.vecto_example.vecto.ui.userinfo

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
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
import com.vecto_example.vecto.utils.ServerResponse
import com.vecto_example.vecto.utils.ShareFeedUtil

class UserInfoActivity : AppCompatActivity(), MyFeedAdapter.OnFeedActionListener{
    lateinit var binding: ActivityUserInfoBinding

    private val userInfoViewModel: UserInfoViewModel by viewModels {
        UserInfoViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
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

            if(userId == Auth._userId.value) {
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
            } else if(Auth._userId.value == userId){
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                intent.putExtra("MyPage", R.id.MypageFragment)
                startActivity(intent)
                finish()
            } else {
                if (!userInfoViewModel.isFollowRequestFinished) {
                    Toast.makeText(this, "이전 요청을 처리 중입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT)
                        .show()
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
        userInfoViewModel.userInfoResult.observe(this) { userInfoResult ->
            userInfoResult.onSuccess {

            }.onFailure {
                if(it.message == "E020"){
                    Toast.makeText(this, "사용자 정보가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }

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
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myFeedAdapter.actionPosition = -1
        }

        userInfoViewModel.deleteFeedLikeResult.observe(this) { deleteFeedLikeResult ->
            deleteFeedLikeResult.onSuccess {
                myFeedAdapter.deleteFeedLikeSuccess()
            }.onFailure {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myFeedAdapter.actionPosition = -1
        }

        /*   게시글 삭제   */
        userInfoViewModel.deleteFeedResult.observe(this) { deleteFeedResult ->
            deleteFeedResult.onSuccess {
                myFeedAdapter.deleteFeedSuccess()
                Toast.makeText(this, "게시글 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myFeedAdapter.actionPosition = -1
        }

        /*   Follow 정보 Observer   */
        userInfoViewModel.isFollowing.observe(this) {
            setFollowButton(it)
        }

        userInfoViewModel.postFollowResult.observe(this) {
            if(it){
                Toast.makeText(this, "${binding.UserNameText.text} 님을 팔로우하기 시작했습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "이미 ${binding.UserNameText.text} 님을 팔로우 중입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        userInfoViewModel.deleteFollowResult.observe(this) {
            if(it){
                Toast.makeText(this, "${binding.UserNameText.text} 님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "이미 ${binding.UserNameText.text} 님을 팔로우하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        /*   신고 Observer   */
        userInfoViewModel.postComplaintResult.observe(this) {
            if(it) {
                Toast.makeText(this, "신고처리되었습니다. 검토 후 조치 예정입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        /*   오류 관련 Observer   */

        userInfoViewModel.getFollowRelationError.observe(this) {
            if(it == "FAIL"){
                Toast.makeText(this, "팔로우 정보를 불러오는데 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            setFollowButton(false)
        }

        userInfoViewModel.postFollowError.observe(this) {
            if(it == "FAIL"){
                Toast.makeText(this, "팔로우 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }

        userInfoViewModel.deleteFollowError.observe(this) {
            if(it == "FAIL"){
                Toast.makeText(this, "팔로우 취소 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }

        userInfoViewModel.postComplaintError.observe(this) {
            if(it == "FAIL"){
                Toast.makeText(this, "신고하기 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }

        userInfoViewModel.feedErrorLiveData.observe(this) {
            if(it == "FAIL") {
                Toast.makeText(this, "게시글 불러오기에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else if(it == "ERROR") {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
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
        if(userId != Auth._userId.value)
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
        if(!userInfoViewModel.checkLoading())
            userInfoViewModel.postFeedLike(feedID)
        else{
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            myFeedAdapter.actionPosition = -1
        }
    }

    override fun onDeleteLike(feedID: Int) {
        if(!userInfoViewModel.checkLoading())
            userInfoViewModel.deleteFeedLike(feedID)
        else {
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            myFeedAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFeed(feedID: Int) {
        if(!userInfoViewModel.checkLoading())
            userInfoViewModel.deleteFeed(feedID)
        else{
            Toast.makeText(this, "이전 작업을 처리중 입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            myFeedAdapter.actionPosition = -1
        }
    }

    override fun onItemViewClick(position: Int) {
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
            putExtra("nextPage", userInfoViewModel.nextPage)
            putExtra("followPage", userInfoViewModel.followPage)
            putExtra("lastPage", userInfoViewModel.lastPage)
        }

        if(!userInfoViewModel.checkLoading())
            this.startActivity(intent)
        else
            Toast.makeText(this, "이전 작업을 처리중 입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
    }

    override fun onShareClick(feedInfo: VectoService.FeedInfo) {
        ShareFeedUtil.shareFeed(this, feedInfo)
    }
}