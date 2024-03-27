package com.vecto_example.vecto

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vecto_example.vecto.data.Auth
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
    var followFlag: Boolean = false

    private lateinit var mypostAdapter: MypostAdapter

    private var userId = ""

    private var cnt = 0
    private var pageNo = 0
    private var pageList = mutableListOf<Int>()
    private var responseData = mutableListOf<VectoService.PostResponse>()
    private var responsePageData = mutableListOf<Int>()

    private var loadingFlag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.getStringExtra("userId")?.let {
            startLoading(0)
            userId = it
            getUserInfo(it)
            getPostList(it)

            if(userId == Auth._userId.value)
                binding.MenuIcon.visibility = View.GONE
        }

        mypostAdapter = MypostAdapter(this)
        val postRecyclerView = binding.UserPostRecyclerView
        postRecyclerView.adapter = mypostAdapter
        mypostAdapter.userId = userId
        postRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if(!recyclerView.canScrollVertically(1)) {
                    if(pageNo != -1 && !loadingFlag)
                    {
                        startLoading(1)
                        pageNo++
                        mypostAdapter.pageNo = pageNo
                        getPostList(userId)
                    }


                }
            }
        })


        binding.MenuIcon.setOnClickListener {
            val reportPopupWindow = ReportPopupWindow(this,
                reportListener = {

                    val reportUserDialog = ReportUserDialog(this)
                    reportUserDialog.showDialog()
                    reportUserDialog.onOkButtonClickListener = {

                    }

                    Toast.makeText(this@UserInfoActivity, "신고처리 되었습니다. 검토후 조치 예정입니다.", Toast.LENGTH_SHORT).show()
                }
            )

            // 앵커 뷰를 기준으로 팝업 윈도우 표시
            reportPopupWindow.showPopupWindow(binding.MenuIcon)
        }


        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {

            if(!loadingFlag) {
                startLoading(0)

                pageNo = 0
                cnt = 0
                mypostAdapter = MypostAdapter(this)
                binding.UserPostRecyclerView.adapter = mypostAdapter

                binding.NoneImage.visibility = View.GONE
                binding.NoneText.visibility = View.GONE

                mypostAdapter.feedID.clear()
                mypostAdapter.feedInfo.clear()

                getPostList(userId)

                loadingFlag = false
            }

            swipeRefreshLayout.isRefreshing = false

        }

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
            binding.FollowButtonText.setTextColor(ContextCompat.getColor(this, R.color.vecto_theme_orange))
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


    private fun getPostList(userId: String) {
        val vectoService = VectoService.create()

        val call = vectoService.getUserPost(userId, pageNo)
        call.enqueue(object : Callback<VectoService.VectoResponse<List<Int>>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<List<Int>>>, response: Response<VectoService.VectoResponse<List<Int>>>) {
                if(response.isSuccessful){
                    Log.d("POSTID", "성공: ${response.body()}")

                    cnt = 0
                    responseData.clear()
                    responsePageData.clear()

                    if(response.body()?.result?.isEmpty() == true)
                    {
                        if(pageNo == 0)//검색결과가 없을 경우
                        {
                            binding.UserPostRecyclerView.adapter = null
                            binding.NoneImage.visibility = View.VISIBLE
                            binding.NoneText.visibility = View.VISIBLE
                            pageNo = -1
                            mypostAdapter.pageNo = -1
                        }

                        pageNo = -1
                        endLoading()
                    }
                    else
                    {
                        pageList = response.body()?.result!!.toMutableList()

                        for(item in response.body()!!.result!!){
                            getPostInfo(item)
                        }

                    }
                }
                else{
                    Log.d("POSTID", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    Toast.makeText(this@UserInfoActivity, "잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<List<Int>>>, t: Throwable) {
                Log.d("POSTID", "실패")
                Toast.makeText(this@UserInfoActivity, getText(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun getPostInfo(feedid: Int) {
        val vectoService = VectoService.create()

        val call: Call<VectoService.VectoResponse<VectoService.PostResponse>>

        if(Auth.loginFlag.value == true)
        {
            call = vectoService.getFeedInfo("Bearer ${Auth.token}", feedid)
        }
        else
        {
            call = vectoService.getFeedInfo(feedid)
        }

        call.enqueue(object : Callback<VectoService.VectoResponse<VectoService.PostResponse>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<VectoService.PostResponse>>, response: Response<VectoService.VectoResponse<VectoService.PostResponse>>) {
                if(response.isSuccessful){
                    Log.d("POSTINFO", "성공: ${response.body()}")

                    val result = response.body()!!.result


                    responseData.add(result!!)
                    responsePageData.add(feedid)
                    cnt++

                    if(cnt == pageList.size)//마지막 항목일 경우
                    {

                        var idxcnt = 0

                        while(cnt != 0) {
                            for (i in 0 until pageList.size) {
                                if (pageList[idxcnt] == responsePageData[i]) {
                                    mypostAdapter.feedInfo.add(responseData[i])
                                    mypostAdapter.feedID.add(responsePageData[i])
                                    cnt--
                                    break
                                }
                            }

                            idxcnt++
                        }

                        mypostAdapter.notifyDataSetChanged()
                        endLoading()
                    }
                }
                else{
                    Log.d("POSTINFO", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    Toast.makeText(this@UserInfoActivity, "잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    endLoading()
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<VectoService.PostResponse>>, t: Throwable) {
                Log.d("POSTINFO", "실패")
                Toast.makeText(this@UserInfoActivity, getText(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
                endLoading()
            }

        })
    }

    private fun startLoading(type: Int){
        when(type){
            0 -> binding.progressBarCenter.visibility = View.VISIBLE
            1 -> binding.progressBar.visibility = View.VISIBLE
        }
    }
    private fun endLoading(){
        binding.progressBarCenter.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }
}