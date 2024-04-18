package com.vecto_example.vecto.ui.userinfo

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityUserInfoBinding
import com.vecto_example.vecto.popupwindow.ReportPopupWindow
import com.vecto_example.vecto.dialog.ReportUserDialog
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.mypage.myfeed.adapter.MyPostAdapter
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.RequestLoginUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserInfoActivity : AppCompatActivity() {
    lateinit var binding: ActivityUserInfoBinding
    private val userInfoViewModel: UserInfoViewModel by viewModels {
        UserInfoViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
    }

    private lateinit var myPostAdapter: MyPostAdapter

    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.getStringExtra("userId")?.let {
            userId = it
            userInfoViewModel.getUserInfo(it)
            getFeed(it)

            if(userId == Auth._userId.value) {
                binding.MenuIcon.visibility = View.GONE
                binding.FollowButton.visibility = View.GONE
                binding.FollowButtonText.visibility = View.GONE
            }
        }

        initRecyclerView()
        initObservers()
        initListeners()


        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {

            if(!userInfoViewModel.checkLoading()){
                userInfoViewModel.initSetting()
                clearRecyclerView()
                clearNoneImage()

                getFeed(userId)
            }

            swipeRefreshLayout.isRefreshing = false

        }

    }

    private fun initListeners() {
        binding.MenuIcon.setOnClickListener {
            val reportPopupWindow = ReportPopupWindow(this,
                reportListener = {

                    val reportUserDialog = ReportUserDialog(this)
                    reportUserDialog.showDialog()
                    reportUserDialog.onOkButtonClickListener = { selectedOptionId, detailContent ->
                        when(selectedOptionId) {
                            R.id.radioButton0 -> {
                                userInfoViewModel.postComplaint(VectoService.ComplaintRequest("BAD_MANNER", userId, null))
                            }

                            R.id.radioButton1 -> {
                                userInfoViewModel.postComplaint(VectoService.ComplaintRequest("INSULT", userId, null))
                            }
                            R.id.radioButton2 -> {
                                userInfoViewModel.postComplaint(VectoService.ComplaintRequest("SEXUAL_HARASSMENT", userId, null))
                            }
                            R.id.radioButton3 -> {
                                if (detailContent != null) {
                                    userInfoViewModel.postComplaint(VectoService.ComplaintRequest("OTHER_PROBLEM", userId, detailContent))
                                }
                                else{
                                    userInfoViewModel.postComplaint(VectoService.ComplaintRequest("OTHER_PROBLEM", userId, null))
                                }
                            }
                        }
                    }
                }
            )

            // 앵커 뷰를 기준으로 팝업 윈도우 표시
            reportPopupWindow.showPopupWindow(binding.MenuIcon)
        }

        binding.FollowButton.setOnClickListener {
            if(Auth.loginFlag.value == false)
            {
                RequestLoginUtils.requestLogin(this)
                return@setOnClickListener
            }

            if(!userInfoViewModel.isFollowRequestFinished){
                Toast.makeText(this, "이전 요청을 처리 중입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                if(userInfoViewModel.isFollowing.value == null){
                    userInfoViewModel.getFollow(userId)
                }
                else {
                    if (!userInfoViewModel.isFollowing.value!!) {
                        userInfoViewModel.postFollow(userId)
                    } else {
                        userInfoViewModel.deleteFollow(userId)
                    }
                }
            }
        }
    }

    private fun initObservers() {
        Auth.loginFlag.observe(this){
            if(Auth._userId.value == userId){
                binding.FollowButton.visibility = View.GONE
                binding.FollowButtonText.visibility = View.GONE
            }
        }
        /*   게시글 관련 Observer   */
        userInfoViewModel.feedInfoLiveData.observe(this) {
            //새로운 feed 정보를 받았을 때의 처리
            myPostAdapter.pageNo = userInfoViewModel.nextPage //다음 page 정보
            userInfoViewModel.feedInfoLiveData.value?.let { myPostAdapter.addFeedInfoData(it) }   //새로 받은 게시글 정보 추가
        }

        userInfoViewModel.feedIdsLiveData.observe(this) {
            userInfoViewModel.feedIdsLiveData.value?.let { myPostAdapter.addFeedIdData(it.feedIds) }

            if(userInfoViewModel.allFeedIds.isEmpty() && userInfoViewModel.feedIdsLiveData.value?.feedIds.isNullOrEmpty()){
                setNoneImage()
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

        /*   Follow 정보 Observer   */
        userInfoViewModel.isFollowing.observe(this) {
            setFollowButton(it)
        }

        userInfoViewModel.getFollowError.observe(this) {
            if(it == "FAIL"){
                Toast.makeText(this, "팔로우 정보를 불러오는데 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }

        userInfoViewModel.postFollowResult.observe(this) {
            if(it){
                Toast.makeText(this, "${binding.UserNameText.text} 님을 팔로우하기 시작했습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "이미 ${binding.UserNameText.text} 님을 팔로우 중입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        userInfoViewModel.postFollowError.observe(this) {
            if(it == "FAIL"){
                Toast.makeText(this, "팔로우 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }

        userInfoViewModel.deleteFollowResult.observe(this) {
            if(it){
                Toast.makeText(this, "${binding.UserNameText.text} 님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "이미 ${binding.UserNameText.text} 님을 팔로우하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        userInfoViewModel.deleteFollowError.observe(this) {
            if(it == "FAIL"){
                Toast.makeText(this, "팔로우 취소 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }

        userInfoViewModel.postComplaintResult.observe(this) {
            if(it) {
                Toast.makeText(this, "신고처리되었습니다. 검토 후 조치 예정입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        userInfoViewModel.postComplaintError.observe(this) {
            if(it == "FAIL"){
                Toast.makeText(this, "신고하기 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun initRecyclerView(){
        myPostAdapter = MyPostAdapter(this)

        clearRecyclerView()

        myPostAdapter.addFeedInfoData(userInfoViewModel.allFeedInfo)
        myPostAdapter.addFeedIdData(userInfoViewModel.allFeedIds)

        val postRecyclerView = binding.UserPostRecyclerView
        postRecyclerView.adapter = myPostAdapter
        myPostAdapter.userId = userId
        postRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if(!recyclerView.canScrollVertically(1)) {
                    if(!userInfoViewModel.checkLoading()){
                        getFeed(userId)
                    }
                }
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearRecyclerView() {
        myPostAdapter.feedID.clear()
        myPostAdapter.feedInfo.clear()
        myPostAdapter.notifyDataSetChanged()
    }

    private fun setUserProfile(userinfo: VectoService.UserInfoResponse) {
        binding.UserNameText.text = userinfo.nickName

        if (userinfo.profileUrl == null) {
            binding.ProfileImage.setImageResource(R.drawable.profile_basic)
        }
        else//사용자 정의 이미지가 있을 경우
        {
            LoadImageUtils.loadUserProfileImage(this, binding.ProfileImage, userinfo.profileUrl)
        }

        binding.PostCountText.text = userinfo.feedCount.toString()
        if(userinfo.feedCount == 0)
            binding.NoneText.text = "${userinfo.nickName}님이 작성한 게시물이 없어요!"

        binding.FollowerCount.text = userinfo.followerCount.toString()

        binding.FollowingCount.text = userinfo.followingCount.toString()

        if(Auth.loginFlag.value == true) {
            userInfoViewModel.getFollow(userinfo.userId)
        }
    }

    private fun setFollowButton(isFollowing: Boolean){
        if(isFollowing)//이미 팔로우 한 상태라면
        {
            binding.FollowButton.setImageResource(R.drawable.userinfo_following_button)
            binding.FollowButtonText.text = "팔로잉"
            binding.FollowButtonText.setTextColor(ContextCompat.getColor(this, R.color.vecto_theme_orange))
        }
        else
        {
            binding.FollowButton.setImageResource(R.drawable.userinfo_follow_button)
            binding.FollowButtonText.text = "팔로우"
            binding.FollowButtonText.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun getFeed(userId: String){
        userInfoViewModel.fetchUserFeedResults(userId)
    }

    private fun setNoneImage() {
        binding.NoneImage.visibility = View.VISIBLE
        binding.NoneText.visibility = View.VISIBLE
        binding.NoneText.text = "${binding.UserNameText.text}님이 작성한 게시물이 없어요!"
        Log.d("NONE SET", "NONE IMAGE SET")
    }


    private fun clearNoneImage() {
        binding.NoneImage.visibility = View.GONE
        binding.NoneText.visibility = View.GONE
        Log.d("NONE GONE", "NONE IMAGE IS GONE")
    }
}