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
import com.bumptech.glide.Glide
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.databinding.ActivityUserInfoBinding
import com.vecto_example.vecto.dialog.ReportPopupWindow
import com.vecto_example.vecto.dialog.ReportUserDialog
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui_bottom.MypostAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserInfoActivity : AppCompatActivity() {
    lateinit var binding: ActivityUserInfoBinding
    private val viewModel: UserInfoViewModel by viewModels {
        UserInfoViewModelFactory(FeedRepository(VectoService.create()))
    }

    var followFlag: Boolean = false

    private lateinit var mypostAdapter: MypostAdapter

    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.getStringExtra("userId")?.let {
            userId = it
            getUserInfo(it)
            getFeed(it)

            if(userId == Auth._userId.value)
                binding.MenuIcon.visibility = View.GONE
        }

        initRecyclerView()
        initObservers()


        binding.MenuIcon.setOnClickListener {
            val reportPopupWindow = ReportPopupWindow(this,
                reportListener = {

                    val reportUserDialog = ReportUserDialog(this)
                    reportUserDialog.showDialog()
                    reportUserDialog.onOkButtonClickListener = { selectedOptionId, detailContent ->
                        when(selectedOptionId) {
                            R.id.radioButton0 -> {
                                complaintUser("BAD_MANNER", userId, null)
                            }

                            R.id.radioButton1 -> {
                                complaintUser("INSULT", userId, null)
                            }
                            R.id.radioButton2 -> {
                                complaintUser("SEXUAL_HARASSMENT", userId, null)
                            }
                            R.id.radioButton3 -> {
                                if (detailContent != null) {
                                    complaintUser("OTHER_PROBLEM", userId, detailContent)
                                }
                                else{
                                    complaintUser("OTHER_PROBLEM", userId, null)
                                }
                            }
                        }


                        Toast.makeText(this@UserInfoActivity, "신고처리 되었습니다. 검토후 조치 예정입니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // 앵커 뷰를 기준으로 팝업 윈도우 표시
            reportPopupWindow.showPopupWindow(binding.MenuIcon)
        }


        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {

            if(!viewModel.checkLoading()){
                viewModel.initSetting()
                clearRecyclerView()
                clearNoneImage()

                getFeed(userId)
            }

            swipeRefreshLayout.isRefreshing = false

        }

    }

    private fun initObservers() {
        /*   게시글 관련 Observer   */
        viewModel.feedInfoLiveData.observe(this) {
            //새로운 feed 정보를 받았을 때의 처리
            mypostAdapter.pageNo = viewModel.nextPage //다음 page 정보
            viewModel.feedInfoLiveData.value?.let { mypostAdapter.addFeedInfoData(it) }   //새로 받은 게시글 정보 추가
        }

        viewModel.feedIdsLiveData.observe(this) {
            viewModel.feedIdsLiveData.value?.let { mypostAdapter.addFeedIdData(it.feedIds) }

            if(viewModel.allFeedIds.isEmpty() && viewModel.feedIdsLiveData.value?.feedIds.isNullOrEmpty()){
                setNoneImage()
            }
        }

        /*   로딩 관련 Observer   */
        viewModel.isLoadingCenter.observe(this) {
            if(it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else
                binding.progressBarCenter.visibility = View.GONE
        }
        viewModel.isLoadingBottom.observe(this) {
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.GONE
        }
    }

    private fun initRecyclerView(){
        mypostAdapter = MypostAdapter(this)

        clearRecyclerView()

        mypostAdapter.addFeedInfoData(viewModel.allFeedInfo)
        mypostAdapter.addFeedIdData(viewModel.allFeedIds)

        val postRecyclerView = binding.UserPostRecyclerView
        postRecyclerView.adapter = mypostAdapter
        mypostAdapter.userId = userId
        postRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if(!recyclerView.canScrollVertically(1)) {
                    if(!viewModel.checkLoading()){
                        getFeed(userId)
                    }
                }
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearRecyclerView() {
        mypostAdapter.feedID.clear()
        mypostAdapter.feedInfo.clear()
        mypostAdapter.notifyDataSetChanged()
    }


    private fun getUserInfo(userId: String) {
        val vectoService = VectoService.create()

        val call = vectoService.getUserInfo(userId)
        call.enqueue(object : Callback<VectoService.VectoResponse<VectoService.UserInfoResponse>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<VectoService.UserInfoResponse>>, response: Response<VectoService.VectoResponse<VectoService.UserInfoResponse>>) {
                if (response.isSuccessful) {

                    val body = response.body()?.result
                    if(body != null)
                    {
                        setUserProfile(body)
                    }
                    Log.d("USERINFO", "정보 조회 성공 : $body")
                } else {
                    // 서버 에러 처리
                    Log.d("USERINFO", "정보 조회 실패 : " + response.errorBody()?.string())
                    Toast.makeText(this@UserInfoActivity, "정보 요청에 실패했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<VectoService.UserInfoResponse>>, t: Throwable) {
                Log.d("USERINFO", "정보 조회 실패 : " + t.message)
                Toast.makeText(this@UserInfoActivity, R.string.APIFailToastMessage, Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun setUserProfile(userinfo: VectoService.UserInfoResponse) {
        binding.UserNameText.text = userinfo.nickName

        if (userinfo.profileUrl == null) {
            binding.ProfileImage.setImageResource(R.drawable.profile_basic)
        }
        else//사용자 정의 이미지가 있을 경우
        {
            Glide.with(this)
                .load(userinfo.profileUrl)
                .circleCrop()
                .placeholder(R.drawable.profile_basic) // 로딩 중 표시될 이미지
                .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
                .into(binding.ProfileImage)
        }

        binding.PostCountText.text = userinfo.feedCount.toString()
        if(userinfo.feedCount == 0)
            binding.NoneText.text = "${userinfo.nickName}님이 작성한 게시물이 없어요!"

        binding.FollowerCount.text = userinfo.followerCount.toString()

        binding.FollowingCount.text = userinfo.followingCount.toString()

        binding.FollowButton.setOnClickListener {
            if(!followFlag) {
                requestFollow(userinfo.userId)
                Log.d("TEST", followFlag.toString() + "requestFollow 실행")
            }
            else {
                deleteFollow(userinfo.userId)
                Log.d("TEST", followFlag.toString() + "deleteFollow 실행")

            }

            binding.FollowButton.postDelayed({
                binding.FollowButton.isEnabled = true
            }, 1000)
        }


        if(Auth.loginFlag.value == true) {
            isfollow(userinfo.userId)
        }
        else
        {
            binding.FollowButton.visibility = View.VISIBLE
            binding.FollowButtonText.visibility = View.VISIBLE
        }
    }

    private fun requestFollow(userId: String) {
        val vectoService = VectoService.create()

        val call = vectoService.sendFollow("Bearer ${Auth.token}", userId)
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if (response.isSuccessful) {
                    if(response.body()!!.code == "S023") {
                        setFollowButton(true)
                        Toast.makeText(this@UserInfoActivity, "${binding.UserNameText.text}님을 팔로우하기 시작했습니다.", Toast.LENGTH_SHORT).show()
                    }

                    Log.d("POSTFOLLOW", "팔로우 요청 성공 : ${response.body()?.result}")
                } else {
                    // 서버 에러 처리
                    Log.d("POSTFOLLOW", "팔로우 요청 실패 : " + response.errorBody()?.string())
                    Toast.makeText(this@UserInfoActivity, "팔로우 요청에 실패했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                setFollowButton(false)
                Log.d("POSTFOLLOW", "팔로우 요청 실패 : " + t.message)
                Toast.makeText(this@UserInfoActivity, R.string.APIFailToastMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteFollow(userId: String) {
        val vectoService = VectoService.create()

        val call = vectoService.deleteFollow("Bearer ${Auth.token}", userId)
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if (response.isSuccessful) {
                    if(response.body()!!.code == "S026") {
                        setFollowButton(false)

                        Toast.makeText(this@UserInfoActivity, "${binding.UserNameText.text}님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                    }

                    Log.d("POSTFOLLOW", "팔로우 요청 성공 : ${response.body()?.result}")
                } else {
                    // 서버 에러 처리
                    Log.d("POSTFOLLOW", "팔로우 요청 실패 : " + response.errorBody()?.string())
                    Toast.makeText(this@UserInfoActivity, "팔로우 요청에 실패했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                setFollowButton(false)
                Log.d("POSTFOLLOW", "팔로우 요청 실패 : " + t.message)
                Toast.makeText(this@UserInfoActivity, R.string.APIFailToastMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun isfollow(userId: String){

        val vectoService = VectoService.create()

        val call = vectoService.getFollow("Bearer ${Auth.token}", userId)
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if (response.isSuccessful) {
                    if(response.body()!!.code == "S027")
                        setFollowButton(true)
                    else
                        setFollowButton(false)

                    Log.d("GETFOLLOW", "팔로우 정보 조회 성공 : ${response.body()}")
                } else {
                    // 서버 에러 처리
                    Log.d("GETFOLLOW", "정보 조회 실패 : " + response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                setFollowButton(false)
                Log.d("GETFOLLOW", "팔로우 정보 조회 실패 : " + t.message)
                Toast.makeText(this@UserInfoActivity, R.string.APIFailToastMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun complaintUser(type: String, userId: String, content: String?){

        val vectoService = VectoService.create()

        val call = vectoService.postComplaint("Bearer ${Auth.token}", VectoService.ComplaintRequest(type, userId, content))
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if (response.isSuccessful) {
                    Log.d("complaintUser", "성공 : " + response.body())
                } else {
                    // 서버 에러 처리
                    Log.d("complaintUser", "실패 : " + response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("complaintUser", "실패 : " + t.message)
            }
        })
    }
    private fun setFollowButton(followflag: Boolean){
        if(Auth._userId.value == userId)
        {
            binding.FollowButton.visibility = View.GONE
            binding.FollowButtonText.visibility = View.GONE

            return
        }

        if(followflag)//이미 팔로우 한 상태라면
        {
            binding.FollowButton.setImageResource(R.drawable.userinfo_following_button)
            binding.FollowButtonText.text = "팔로잉"
            binding.FollowButtonText.setTextColor(ContextCompat.getColor(this,
                R.color.vecto_theme_orange
            ))
            followFlag = true
        }
        else
        {
            binding.FollowButton.setImageResource(R.drawable.userinfo_follow_button)
            binding.FollowButtonText.text = "팔로우"
            binding.FollowButtonText.setTextColor(ContextCompat.getColor(this, R.color.white))
            followFlag = false
        }

        binding.FollowButton.visibility = View.VISIBLE
        binding.FollowButtonText.visibility = View.VISIBLE
    }

    private fun getFeed(userId: String){
        viewModel.fetchUserFeedResults(userId)
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