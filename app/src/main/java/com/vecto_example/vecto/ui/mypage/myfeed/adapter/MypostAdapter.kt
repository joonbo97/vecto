package com.vecto_example.vecto.ui.mypage.myfeed.adapter

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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.ui.login.LoginActivity
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.userinfo.UserInfoActivity
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.dialog.DeletePostDialog
import com.vecto_example.vecto.popupwindow.EditDeletePopupWindow
import com.vecto_example.vecto.dialog.LoginRequestDialog
import com.vecto_example.vecto.retrofit.VectoService
import com.google.gson.Gson
import com.vecto_example.vecto.EditPostActivity
import com.vecto_example.vecto.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MypostAdapter(private val context: Context): RecyclerView.Adapter<MypostAdapter.ViewHolder>()
{
    val feedInfo = mutableListOf<VectoService.FeedInfoResponse>()
    val feedID = mutableListOf<Int>()
    var pageNo = 0
    var userId = ""


    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {

        private val titleText: TextView = view.findViewById(R.id.TitleText)
        private val profileImage: ImageView = view.findViewById(R.id.ProfileImage)
        private val nicknameText: TextView = view.findViewById(R.id.NicknameText)
        private val posttimeText: TextView = view.findViewById(R.id.PostTimeText)

        private val postImage: ImageView = view.findViewById(R.id.Image)

        private val courseTime: TextView = view.findViewById(R.id.TotalTimeText)

        private val likeCount: TextView = view.findViewById(R.id.LikeCountText)
        private val likeIcon: ImageView = view.findViewById(R.id.LikeImage)
        private val commentCount: TextView = view.findViewById(R.id.CommentCountText)


        private val likeTouch: ImageView = view.findViewById(R.id.LikeTouchImage)
        private val commentTouch: ImageView = view.findViewById(R.id.CommentTouchImage)


        private val mapSmall: ImageView = view.findViewById(R.id.MapImageSmall)
        private val mapLarge: ImageView = view.findViewById(R.id.MapImageLarge)

        private val menu: ImageView = view.findViewById(R.id.PostMenuImage)


        fun bind(feed: VectoService.FeedInfoResponse) {
            Log.d("FEED", "FeedImage Size: ${feed.image.size}")
            //이미지가 있는지 여부를 확인하여 style을 결정
            if (feed.image.isEmpty()) {//2:1 mapImage [1]
                mapLarge.visibility = View.VISIBLE
                mapSmall.visibility = View.INVISIBLE
                postImage.visibility = View.INVISIBLE

                Glide.with(context)
                    .load(feed.mapImage[1])
                    .error(R.drawable.error_image) // 에러 발생 시 표시될 이미지
                    .into(mapLarge)

            } else {//1:1 mapImage[0]
                mapLarge.visibility = View.INVISIBLE
                mapSmall.visibility = View.VISIBLE
                postImage.visibility = View.VISIBLE

                Glide.with(context)
                    .load(feed.mapImage[0])
                    .error(R.drawable.error_image) // 에러 발생 시 표시될 이미지
                    .into(mapSmall)

                Glide.with(context)
                    .load(feed.image[0])
                    .error(R.drawable.error_image) // 에러 발생 시 표시될 이미지
                    .into(postImage)
            }

            titleText.text = feed.title

            if(Auth._userId.value != feedInfo[adapterPosition].userId) {
                menu.visibility = View.GONE
            }

            if(feed.userProfile != null)
            {
                Glide.with(context)
                    .load(feed.userProfile)
                    .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
                    .circleCrop()
                    .into(profileImage)
            }
            else
                profileImage.setImageResource(R.drawable.profile_basic)

            nicknameText.text = feed.nickName
            posttimeText.text = feed.timeDifference


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
                val intent = Intent(context, FeedDetailActivity::class.java).apply {
                    putExtra("feedInfoListJson", Gson().toJson(feedInfo))
                    putExtra("feedIDListJson", Gson().toJson(feedID))
                    putExtra("userId", userId)
                    putExtra("pageNo", pageNo)
                }
                context.startActivity(intent)
            }

            menu.setOnClickListener {
                val editDeletePopupWindow = EditDeletePopupWindow(context,
                    editListener = {
                        val intent = Intent(context, EditPostActivity::class.java).apply {
                            putExtra("feedInfoJson", Gson().toJson(feedInfo[adapterPosition]))
                            putExtra("feedId", feedID[adapterPosition])
                        }
                        context.startActivity(intent)
                    },
                    deleteListener = {
                        val deletePostDialog = DeletePostDialog(context)
                        deletePostDialog.showDialog()
                        deletePostDialog.onOkButtonClickListener = {
                            deletePost(feedID[adapterPosition], adapterPosition)
                        }
                    },
                    dismissListener = {

                    })

                // 앵커 뷰를 기준으로 팝업 윈도우 표시
                editDeletePopupWindow.showPopupWindow(menu)
            }


        }

    }

    private fun deletePost(feedid: Int, position: Int) {
        val vectoService = VectoService.create()

        val call = vectoService.deleteFeed("Bearer ${Auth.token}", feedid)
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if (response.isSuccessful) {

                    Log.d("DELETEPOSST", "게시글 삭제 성공 : ${response.body()?.result}")
                    Toast.makeText(context, "게시글 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show()

                    feedID.removeAt(position)
                    feedInfo.removeAt(position)
                    notifyItemRemoved(position)

                } else {
                    // 서버 에러 처리
                    Log.d("DELETEPOSST", "게시글 삭제 요청 실패 : " + response.errorBody()?.string())
                    Toast.makeText(context, "게시글 삭제 요청에 실패했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("DELETEPOSST", "게시글 삭제 요청 실패 : " + t.message)
                Toast.makeText(context, R.string.APIErrorToastMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.mypost_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return feedInfo.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feed = feedInfo[position]
        holder.bind(feed)
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