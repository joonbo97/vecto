package com.vecto_example.vecto.ui.mypage.myfeed.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.userinfo.UserInfoActivity
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.dialog.DeletePostDialog
import com.vecto_example.vecto.popupwindow.EditDeletePopupWindow
import com.vecto_example.vecto.retrofit.VectoService
import com.google.gson.Gson
import com.vecto_example.vecto.ui.editfeed.EditPostActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.databinding.MypostItemBinding
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.RequestLoginUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyFeedAdapter(private val context: Context): RecyclerView.Adapter<MyFeedAdapter.ViewHolder>()
{
    val feedInfo = mutableListOf<VectoService.FeedInfoResponse>()
    val feedID = mutableListOf<Int>()
    var pageNo = 0
    var userId = ""

    var actionPosition = -1

    interface OnFeedActionListener {
        fun onPostLike(feedID: Int)

        fun onDeleteLike(feedID: Int)

        fun onDeleteFeed(feedID: Int)
    }

    var feedActionListener: OnFeedActionListener? = null

    inner class ViewHolder(val binding: MypostItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(feed: VectoService.FeedInfoResponse) {

            //이미지가 있는지 여부를 확인하여 style을 결정 하고 이미지 설정
            if (feed.image.isEmpty()) {//2:1 mapImage [1]
                binding.MapImageLarge.visibility = View.VISIBLE
                binding.MapImageSmall.visibility = View.INVISIBLE
                binding.Image.visibility = View.INVISIBLE

                LoadImageUtils.loadImage(context, binding.MapImageLarge, feed.mapImage[1])
            } else {//1:1 mapImage[0]
                binding.MapImageLarge.visibility = View.INVISIBLE
                binding.MapImageSmall.visibility = View.VISIBLE
                binding.Image.visibility = View.VISIBLE

                LoadImageUtils.loadImage(context, binding.MapImageSmall, feed.mapImage[0])
                LoadImageUtils.loadImage(context, binding.Image, feed.image[0])
            }

            /*   게시글 데이터 설정   */
            binding.TitleText.text = feed.title //제목
            LoadImageUtils.loadUserProfileImage(context, binding.ProfileImage, feed.userProfile)    //프로필 이미지
            binding.NicknameText.text = feed.nickName   //닉네임
            binding.PostTimeText.text = feed.timeDifference //업로드 시간
            binding.LikeCountText.text = feed.likeCount.toString()
            binding.CommentCountText.text = feed.commentCount.toString()

            /*   자신의 게시글이 아닌 경우, 게시글 메뉴 숨김   */
            if(Auth._userId.value != feedInfo[adapterPosition].userId) {
                binding.PostMenuImage.visibility = View.GONE
            }

            binding.UserTouchImage.setOnClickListener {
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("userId", feedInfo[adapterPosition].userId)
                context.startActivity(intent)
            }

            binding.LikeTouchImage.setOnClickListener {
                clickLikeAction(feedInfo[adapterPosition])
            }

            binding.CommentTouchImage.setOnClickListener {
                val intent = Intent(context, CommentActivity::class.java)
                intent.putExtra("feedID", feedID[adapterPosition])
                context.startActivity(intent)
            }


            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

            val date1 = LocalDateTime.parse(feed.visit.first().datetime, format)
            val date2 = LocalDateTime.parse(feed.visit.last().datetime, format)

            val minutesPassed = Duration.between(date1, date2).toMinutes().toInt()

            if(minutesPassed < 60)
            {
                binding.TotalTimeText.text = "약 1시간 이내 코스"
            }
            else{
                binding.TotalTimeText.text = "약 ${minutesPassed/60}시간 코스"
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

            binding.PostMenuImage.setOnClickListener {
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
                            actionPosition = adapterPosition
                            feedActionListener?.onDeleteFeed(feedID[adapterPosition])
                        }
                    },
                    dismissListener = {

                    })

                // 앵커 뷰를 기준으로 팝업 윈도우 표시
                editDeletePopupWindow.showPopupWindow(binding.PostMenuImage)
            }
        }

        //좋아요 클릭시 실행 함수
        private fun clickLikeAction(feed: VectoService.FeedInfoResponse) {

            if(Auth.loginFlag.value == true && actionPosition == -1) {
                if (feed.likeFlag) {
                    actionPosition = adapterPosition
                    feedActionListener?.onDeleteLike(feedID[adapterPosition])
                } else {
                    val anim = AnimationUtils.loadAnimation(context, R.anim.like_anim)

                    anim.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })

                    binding.LikeImage.startAnimation(anim)

                    actionPosition = adapterPosition
                    feedActionListener?.onPostLike(feedID[adapterPosition])

                }
            }
            else {
                RequestLoginUtils.requestLogin(context)
            }

        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MypostItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return feedInfo.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feed = feedInfo[position]
        holder.bind(feed)

        /*   좋아요 설정   */
        if(feed.likeFlag)
            holder.binding.LikeImage.setImageResource(R.drawable.post_like_on)
        else
            holder.binding.LikeImage.setImageResource(R.drawable.post_like_off)
    }

    //좋아요 성공 시 실행 함수
    fun postFeedLikeSuccess() {
        feedInfo[actionPosition].likeFlag = true
        feedInfo[actionPosition].likeCount++

        notifyItemChanged(actionPosition)
    }

    //좋아요 삭제 성공 시 실행 함수
    fun deleteFeedLikeSuccess() {
        feedInfo[actionPosition].likeFlag = false
        feedInfo[actionPosition].likeCount--

        notifyItemChanged(actionPosition)
    }

    //게시글 삭제 성공 시 실행 함수
    fun deleteFeedSuccess() {
        feedInfo.removeAt(actionPosition)

        notifyItemRemoved(actionPosition)
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