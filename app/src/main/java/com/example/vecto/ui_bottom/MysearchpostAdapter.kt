package com.example.vecto.ui_bottom

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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.vecto.CommentActivity
import com.example.vecto.LoginActivity
import com.example.vecto.PostDetailActivity
import com.example.vecto.R
import com.example.vecto.UserInfoActivity
import com.example.vecto.data.Auth
import com.example.vecto.dialog.LoginRequestDialog
import com.example.vecto.retrofit.VectoService
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MysearchpostAdapter(private val context: Context) : RecyclerView.Adapter<MysearchpostAdapter.ViewHolder>()
{
    val feedInfo = mutableListOf<VectoService.PostResponse>()
    val feedID = mutableListOf<Int>()

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {

        private val titleText: TextView = view.findViewById(R.id.TitleText)
        private val profileImage: ImageView = view.findViewById(R.id.ProfileImage)
        private val nicknameText: TextView = view.findViewById(R.id.NicknameText)
        private val posttimeText: TextView = view.findViewById(R.id.PostTimeText)
        private val followButton: ImageView = view.findViewById(R.id.FollowButton)
        private val followText: TextView = view.findViewById(R.id.ButtonText)

        private var followFlag: Boolean = false

        private val postImage: ImageView = view.findViewById(R.id.Image)

        private val courseTime: TextView = view.findViewById(R.id.TotalTimeText)

        private val likeCount: TextView = view.findViewById(R.id.LikeCountText)
        private val likeIcon: ImageView = view.findViewById(R.id.LikeImage)
        private val commentCount: TextView = view.findViewById(R.id.CommentCountText)


        private val likeTouch: ImageView = view.findViewById(R.id.LikeTouchImage)
        private val commentTouch: ImageView = view.findViewById(R.id.CommentTouchImage)


        private val mapSmall: ImageView = view.findViewById(R.id.MapImageSmall)
        private val mapLarge: ImageView = view.findViewById(R.id.MapImageLarge)


        fun bind(feed: VectoService.PostResponse) {
            Log.d("FEED", "FeedImage Size: ${feed.image.size}")
            //이미지가 있는지 여부를 확인하여 style을 결정
            if (feed.image.isEmpty()) {//2:1 mapImage [1]
                mapLarge.visibility = View.VISIBLE
                mapSmall.visibility = View.INVISIBLE
                postImage.visibility = View.INVISIBLE

                Glide.with(context)
                    .load(feed.mapImage[1])
                    .placeholder(R.drawable.empty_image) // 로딩 중 표시될 이미지
                    .error(R.drawable.empty_image) // 에러 발생 시 표시될 이미지
                    .into(mapLarge)

            } else {//1:1 mapImage[0]
                mapLarge.visibility = View.INVISIBLE
                mapSmall.visibility = View.VISIBLE
                postImage.visibility = View.VISIBLE

                Glide.with(context)
                    .load(feed.mapImage[0])
                    .placeholder(R.drawable.empty_image) // 로딩 중 표시될 이미지
                    .error(R.drawable.empty_image) // 에러 발생 시 표시될 이미지
                    .into(mapSmall)

                Glide.with(context)
                    .load(feed.image[0])
                    .placeholder(R.drawable.empty_image) // 로딩 중 표시될 이미지
                    .error(R.drawable.empty_image) // 에러 발생 시 표시될 이미지
                    .into(postImage)
            }

            titleText.text = feed.title

            if(feed.userProfile != null)
            {
                Glide.with(context)
                    .load(feed.userProfile)
                    .placeholder(R.drawable.profile_basic) // 로딩 중 표시될 이미지
                    .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
                    .circleCrop()
                    .into(profileImage)
            }
            else
                profileImage.setImageResource(R.drawable.profile_basic)

            nicknameText.text = feed.nickName
            posttimeText.text = feed.timeDifference


            fun setFollowButton(flag: Boolean){
                if(flag)
                {
                    followButton.setImageResource(R.drawable.following_button)
                    followText.text = "팔로잉"
                    followText.setTextColor(ContextCompat.getColor(context, R.color.white))
                    followFlag = true
                }
                else
                {
                    followButton.setImageResource(R.drawable.follow_button)
                    followText.text = "팔로우"
                    followText.setTextColor(ContextCompat.getColor(context, R.color.vecto_theme_orange))
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
            fun deleteFollow(userId: String) {
                val vectoService = VectoService.create()

                val call = vectoService.deleteFollow("Bearer ${Auth.token}", userId)
                call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
                    override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                        if (response.isSuccessful) {
                            if(response.body()!!.code == "S024") {
                                Toast.makeText(context, "${feed.nickName}님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                            }
                            else if(response.body()!!.code == "S026"){
                                Toast.makeText(context, "이미 ${feed.nickName}님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                            }
                            followButton.setImageResource(R.drawable.follow_button)
                            followText.text = "팔로우"
                            followText.setTextColor(ContextCompat.getColor(context, R.color.vecto_theme_orange))
                            followFlag = false

                            Log.d("POSTFOLLOWCACEL", "팔로우 해제 성공 : ${response.body()}")
                        } else {
                            // 서버 에러 처리
                            Log.d("POSTFOLLOWCACEL", "팔로우 해제 실패 : " + response.errorBody()?.string())
                            Toast.makeText(context, "팔로우 요청에 실패했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                        setFollowButton(false)
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
                                Toast.makeText(context, "${feed.nickName}님을 팔로우하기 시작했습니다.", Toast.LENGTH_SHORT).show()
                            }
                            else if(response.body()!!.code == "S025") {
                                Toast.makeText(context, "이미 ${feed.nickName}님을 팔로우 중입니다.", Toast.LENGTH_SHORT).show()
                            }

                            followButton.setImageResource(R.drawable.following_button)
                            followText.text = "팔로잉"
                            followText.setTextColor(ContextCompat.getColor(context, R.color.white))
                            followFlag = true
                            Log.d("POSTFOLLOW", "팔로우 요청 성공 : ${response.body()?.result}")
                        } else {
                            // 서버 에러 처리
                            Log.d("POSTFOLLOW", "팔로우 요청 실패 : " + response.errorBody()?.string())
                            Toast.makeText(context, "팔로우 요청에 실패했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                        setFollowButton(false)
                        Log.d("POSTFOLLOW", "팔로우 요청 실패 : " + t.message)
                        Toast.makeText(context, R.string.APIFailToastMessage, Toast.LENGTH_SHORT).show()
                    }
                })
            }

            isfollow(feed.userId)

            followButton.setOnClickListener {
                followButton.isEnabled = false
                if(!followFlag)
                {
                    requestFollow(feed.userId)
                }
                else
                {
                    deleteFollow(feed.userId)
                }

                followButton.postDelayed({
                    followButton.isEnabled = true
                }, 1000)
            }

            if(feed.likeFlag)
                likeIcon.setImageResource(R.drawable.post_like_on)
            else
                likeIcon.setImageResource(R.drawable.post_like_off)

            likeCount.text = feed.likeCount.toString()
            commentCount.text = feed.commentCount.toString()

            fun clickLikeAction() {
                if(Auth.loginFlag.value == true) {
                    if (feed.likeFlag) {
                        likeIcon.setImageResource(R.drawable.post_like_off)

                        cancelLike(feedID[adapterPosition])
                        feed.likeFlag = false

                        feed.likeCount--
                        likeCount.text = feed.likeCount.toString()
                    } else {
                        likeIcon.setImageResource(R.drawable.post_like_on)
                        val anim = AnimationUtils.loadAnimation(context, R.anim.like_anim)
                        feed.likeCount++
                        likeCount.text = feed.likeCount.toString()

                        anim.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {}

                            override fun onAnimationEnd(animation: Animation?) {
                            }

                            override fun onAnimationRepeat(animation: Animation?) {}
                        })

                        likeIcon.startAnimation(anim)
                        sendLike(feedID[adapterPosition])
                        feed.likeFlag = true
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

            profileImage.setOnClickListener {
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("userId", feedInfo[adapterPosition].userId)
                context.startActivity(intent)
            }

            nicknameText.setOnClickListener {
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("userId", feedInfo[adapterPosition].userId)
                context.startActivity(intent)
            }

            likeTouch.setOnClickListener {
                clickLikeAction()
            }

            commentTouch.setOnClickListener {
                val intent = Intent(context, CommentActivity::class.java)
                intent.putExtra("feedID", feedID[adapterPosition])
                context.startActivity(intent)
            }


            val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

            val date1 = LocalDateTime.parse(feed.visit.first().datetime, FORMAT)
            val date2 = LocalDateTime.parse(feed.visit.last().datetime, FORMAT)

            val minutesPassed = Duration.between(date1, date2).toMinutes().toInt()

            if(minutesPassed < 60)
            {
                courseTime.text = "약 1시간 이내 코스"
            }
            else{
                courseTime.text = "약 ${minutesPassed/60}시간 코스"
            }

            itemView.setOnClickListener {
                val intent = Intent(context, PostDetailActivity::class.java).apply {
                    putExtra("feedInfoListJson", Gson().toJson(feedInfo))
                    putExtra("feedIDListJson", Gson().toJson(feedID))
                }
                context.startActivity(intent)
            }

        }

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MysearchpostAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.post_small_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return feedInfo.size
    }

    override fun onBindViewHolder(holder: MysearchpostAdapter.ViewHolder, position: Int) {
        val feed = feedInfo[position]
        holder.bind(feed)
    }

    private fun sendLike(feedID: Int) {
        val vectoService = VectoService.create()

        val call = vectoService.sendLike("Bearer ${Auth.token}", feedID)
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>>{
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
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>>{
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
}