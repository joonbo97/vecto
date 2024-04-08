package com.vecto_example.vecto.ui.detail.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.ui.login.LoginActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.VisitNumberAdapter
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.dialog.LoginRequestDialog
import com.vecto_example.vecto.retrofit.VectoService
import me.relex.circleindicator.CircleIndicator3
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyFeedDetailAdapter(private val context: Context): RecyclerView.Adapter<MyFeedDetailAdapter.ViewHolder>() {
    val feedInfo = mutableListOf<VectoService.FeedInfoResponse>()
    val feedID = mutableListOf<Int>()

    lateinit var visitdata: List<VisitData>
    lateinit var locationdata: List<LocationData>

    interface OnItemViewedListener {
        fun onItemViewed(feedInfo: VectoService.FeedInfoResponse)
    }

    var onItemViewedListener: OnItemViewedListener? = null


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewPager: ViewPager2 = view.findViewById(R.id.view_pager)
        val indicator: CircleIndicator3 = view.findViewById(R.id.indicator)

        val titleText: TextView = view.findViewById(R.id.TitleText)
        val totalTimeText: TextView = view.findViewById(R.id.TotalTimeText)
        val postTimeText: TextView = view.findViewById(R.id.PostTimeText)

        val numberRecyclerView: RecyclerView = view.findViewById(R.id.PostDetailRecyclerView)

        val contextText: TextView = view.findViewById(R.id.ContentText)

        val likeBox: ImageView = view.findViewById(R.id.LikeBox)
        val likeImage: ImageView = view.findViewById(R.id.LikeImage)
        val likeCount: TextView = view.findViewById(R.id.LikeCount)

        val commentBox: ImageView = view.findViewById(R.id.CommentBox)
        val commentCount: TextView = view.findViewById(R.id.CommentCount)

        val profileImage: ImageView = view.findViewById(R.id.ProfileImage)
        val userNamveText: TextView = view.findViewById(R.id.UserNameText)

        val followButton: ImageView = view.findViewById(R.id.FollowButton)
        val followText: TextView = view.findViewById(R.id.FollowButtonText)

        lateinit var visitNumberAdapter: VisitNumberAdapter

        init {
            // 내부 RecyclerView 설정
            visitNumberAdapter = VisitNumberAdapter(context)
            numberRecyclerView.adapter = visitNumberAdapter
            numberRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }

        fun bindVisitData(visitDataList: List<VisitData>) {
            // VisitNumberAdapter에 데이터 전달 및 업데이트
            visitNumberAdapter.visitdataList.clear()
            visitNumberAdapter.visitdataList.addAll(visitDataList)
            visitNumberAdapter.notifyDataSetChanged()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        /*제목 설정*/
        holder.titleText.text = feedInfo[position].title

        /*코스 시간 설정*/
        val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        val date1 = LocalDateTime.parse(feedInfo[position].visit.first().datetime, FORMAT)
        val date2 = LocalDateTime.parse(feedInfo[position].visit.last().datetime, FORMAT)

        val minutesPassed = Duration.between(date1, date2).toMinutes().toInt()

        var followFlag: Boolean = false

        if(minutesPassed < 60)
        {
            holder.totalTimeText.text = "약 1시간 이내 코스"
        }
        else{
            holder.totalTimeText.text = "약 ${minutesPassed/60}시간 코스"
        }

        holder.postTimeText.text = feedInfo[position].timeDifference

        /*넘버링 이미지 설정*/
        holder.bindVisitData(feedInfo[position].visit)

        /*이미지 설정*/
        if(feedInfo[position].image.isEmpty())
        {
            holder.viewPager.visibility = View.GONE
            holder.indicator.visibility = View.GONE
        }
        else {
            holder.viewPager.visibility = View.VISIBLE
            holder.indicator.visibility = View.VISIBLE

            holder.viewPager.adapter = ImageSliderAdapter(context, feedInfo[position].image)
            holder.indicator.setViewPager(holder.viewPager)
        }

        /*내용 설정*/
        if(feedInfo[position].content.isEmpty())
        {
            holder.contextText.visibility = View.GONE
        }
        else
        {
            holder.contextText.text = feedInfo[position].content
        }

        /*좋아요 설정*/
        holder.likeCount.text = feedInfo[position].likeCount.toString()
        if(feedInfo[position].likeFlag)
            holder.likeImage.setImageResource(R.drawable.post_like_on)
        else
            holder.likeImage.setImageResource(R.drawable.post_like_off)

        holder.likeBox.setOnClickListener {
            if(Auth.loginFlag.value == true) {
                if (feedInfo[position].likeFlag) {
                    holder.likeImage.setImageResource(R.drawable.post_like_off)

                    cancelLike(feedID[position])
                    feedInfo[position].likeFlag = false

                    feedInfo[position].likeCount--
                    holder.likeCount.text = feedInfo[position].likeCount.toString()
                } else {
                    holder.likeImage.setImageResource(R.drawable.post_like_on)
                    val anim = AnimationUtils.loadAnimation(context, R.anim.like_anim)
                    feedInfo[position].likeCount++
                    holder.likeCount.text = feedInfo[position].likeCount.toString()

                    anim.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}

                        override fun onAnimationEnd(animation: Animation?) {
                        }

                        override fun onAnimationRepeat(animation: Animation?) {}
                    })

                    holder.likeImage.startAnimation(anim)
                    sendLike(feedID[position])
                    feedInfo[position].likeFlag = true
                }
            }
            else {
                val loginRequestDialog = LoginRequestDialog(context)
                loginRequestDialog.showDialog()
                loginRequestDialog.onOkButtonClickListener = {
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                }
            }
        }

        /*댓글 설정*/
        holder.commentBox.setOnClickListener {
            val intent = Intent(context, CommentActivity::class.java)
            intent.putExtra("feedID", feedID[position])
            context.startActivity(intent)
        }

        holder.commentCount.text = feedInfo[position].commentCount.toString()

        /*작성자 프로필 설정*/
        if(feedInfo[position].userProfile != null)
        {
            Glide.with(context)
                .load(feedInfo[position].userProfile)
                .placeholder(R.drawable.profile_basic) // 로딩 중 표시될 이미지
                .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
                .circleCrop()
                .into(holder.profileImage)
        }
        else
            holder.profileImage.setImageResource(R.drawable.profile_basic)

        /*작성자 닉네임 설정*/
        holder.userNamveText.text = feedInfo[position].nickName

        /*팔로우 버튼 설정*/
        fun setFollowButton(flag: Boolean){
            if(flag)
            {
                holder.followButton.setImageResource(R.drawable.detail_following_button)
                holder.followText.text = "팔로잉"
                holder.followText.setTextColor(ContextCompat.getColor(context, R.color.detail_gray))
                followFlag = true
            }
            else
            {
                holder.followButton.setImageResource(R.drawable.detail_follow_button)
                holder.followText.text = "팔로우"
                holder.followText.setTextColor(ContextCompat.getColor(context, R.color.white))
                followFlag = false
            }
        }

        fun isfollow(userId: String) {
            val vectoService = VectoService.create()

            val call = vectoService.getFollow("Bearer ${Auth.token}", userId)
            call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
                override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                    if (response.isSuccessful) {
                        if(response.body()!!.code == "S027")
                            setFollowButton(true)
                        else
                            setFollowButton(false)

                        Log.d("GETFOLLOW", "팔로우 정보 조회 성공 : ${response.body()?.result}")
                    } else {
                        // 서버 에러 처리
                        Log.d("GETFOLLOW", "정보 조회 실패 : " + response.errorBody()?.string())
                    }
                }

                override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                    setFollowButton(false)
                    Log.d("GETFOLLOW", "팔로우 정보 조회 실패 : " + t.message)
                    Toast.makeText(context, R.string.APIFailToastMessage, Toast.LENGTH_SHORT).show()
                }
            })
        }
        if(Auth.loginFlag.value == true)
            isfollow(feedInfo[position].userId)

        holder.followButton.setOnClickListener {
            holder.followButton.isEnabled = false

            fun deleteFollow(userId: String) {
                val vectoService = VectoService.create()

                val call = vectoService.deleteFollow("Bearer ${Auth.token}", userId)
                call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
                    override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                        if (response.isSuccessful) {
                            if(response.body()!!.code == "S024") {
                                Toast.makeText(context, "${feedInfo[holder.adapterPosition].nickName}님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                            }
                            else if(response.body()!!.code == "S026"){

                                Toast.makeText(context, "이미 ${feedInfo[holder.adapterPosition].nickName}님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                            }
                            holder.followButton.setImageResource(R.drawable.detail_follow_button)
                            holder.followText.text = "팔로우"
                            holder.followText.setTextColor(ContextCompat.getColor(context,
                                R.color.white
                            ))
                            followFlag = false

                            Log.d("POSTFOLLOWCACEL", "팔로우 해제 성공 : ${response.body()}")
                        } else {
                            // 서버 에러 처리
                            Log.d("POSTFOLLOWCACEL", "팔로우 해제 실패 : " + response.errorBody()?.string())
                            Toast.makeText(context, "팔로우 취소 요청이 실패했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {

                        Log.d("POSTFOLLOWCACEL", "팔로우 해제 실패 : " + t.message)
                        Toast.makeText(context, R.string.APIFailToastMessage, Toast.LENGTH_SHORT).show()
                    }
                })
            }
            fun requestFollow(userId: String) {
                val vectoService = VectoService.create()

                val call = vectoService.sendFollow("Bearer ${Auth.token}", userId)
                call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
                    override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                        if (response.isSuccessful) {
                            if(response.body()!!.code == "S023") {
                                Toast.makeText(context, "${feedInfo[holder.adapterPosition].nickName}님을 팔로우하기 시작했습니다.", Toast.LENGTH_SHORT).show()
                            }
                            else if(response.body()!!.code == "S025") {
                                Toast.makeText(context, "이미 ${feedInfo[holder.adapterPosition].nickName}님을 팔로우 중입니다.", Toast.LENGTH_SHORT).show()
                            }
                            holder.followButton.setImageResource(R.drawable.detail_following_button)
                            holder.followText.text = "팔로잉"
                            holder.followText.setTextColor(ContextCompat.getColor(context,
                                R.color.detail_gray
                            ))
                            followFlag = true

                            Log.d("POSTFOLLOW", "팔로우 요청 성공 : ${response.body()?.result}")
                        } else {
                            // 서버 에러 처리
                            Log.d("POSTFOLLOW", "팔로우 요청 실패 : " + response.errorBody()?.string())
                            Toast.makeText(context, "팔로우 요청에 실패했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                        Log.d("POSTFOLLOW", "팔로우 요청 실패 : " + t.message)
                        Toast.makeText(context, R.string.APIFailToastMessage, Toast.LENGTH_SHORT).show()
                    }
                })
            }

            if(Auth.loginFlag.value == false) {
                loginDialog()
                return@setOnClickListener
            }

            holder.followButton.isEnabled = false
            if(!followFlag)
            {
                requestFollow(feedInfo[position].userId)
            }
            else
            {
                deleteFollow(feedInfo[position].userId)
            }

            holder.followButton.postDelayed({
                holder.followButton.isEnabled = true
            }, 1000)
        }


    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.post_detail_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return feedInfo.size
    }
    private fun sendLike(feedID: Int) {
        val vectoService = VectoService.create()

        val call = vectoService.sendLike("Bearer ${Auth.token}", feedID)
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if(response.isSuccessful){
                    Log.d("LIKE", "성공: ${response.body()}")
                }
                else{
                    Log.d("LIKE", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("LIKE", "실패 ${t.message.toString()}" )
            }

        })
    }

    private fun cancelLike(feedID: Int) {
        val vectoService = VectoService.create()

        val call = vectoService.cancelLike("Bearer ${Auth.token}", feedID)
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if(response.isSuccessful){
                    Log.d("LIKE", "성공: ${response.body()}")
                }
                else{
                    Log.d("LIKE", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("LIKE", "실패 ${t.message.toString()}" )
            }

        })
    }

    private fun loginDialog(){
        val loginRequestDialog = LoginRequestDialog(context)
        loginRequestDialog.showDialog()
        loginRequestDialog.onOkButtonClickListener = {
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
        }
    }

    fun addFeedInfoData(newData: List<VectoService.FeedInfoResponse>) {
        //데이터 추가 함수
        val startIdx = feedInfo.size
        feedInfo.addAll(newData)
        notifyItemRangeInserted(startIdx, newData.size)
    }

    fun addFeedIdData(newData: List<Int>){
        val startIdx = feedInfo.size
        feedID.addAll(newData)
        notifyItemRangeInserted(startIdx, newData.size)
    }

}