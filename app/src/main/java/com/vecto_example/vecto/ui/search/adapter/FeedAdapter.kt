package com.vecto_example.vecto.ui.search.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.databinding.PostSmallItemBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.ui.userinfo.UserInfoActivity
import com.vecto_example.vecto.utils.DateTimeUtils
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.RequestLoginUtils

class FeedAdapter(): RecyclerView.Adapter<FeedAdapter.ViewHolder>() {
    var actionPosition = -1
    var feedInfoWithFollow = mutableListOf<VectoService.FeedInfoWithFollow>()
    var lastSize = 0

    interface OnFeedActionListener {
        fun onPostFeedLike(feedId: Int)

        fun onDeleteFeedLike(feedId: Int)

        fun onPostFollow(userId: String)

        fun onDeleteFollow(userId: String)

        fun onItemClick(position: Int)
    }

    var feedActionListener: OnFeedActionListener? = null

    inner class ViewHolder(val binding: PostSmallItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(feedInfoWithFollow: VectoService.FeedInfoWithFollow) {

            /*   사용자 정보   */
            LoadImageUtils.loadUserProfileImage(itemView.context, binding.ProfileImage, feedInfoWithFollow.feedInfo.userProfile)
            binding.NicknameText.text = feedInfoWithFollow.feedInfo.nickName

            binding.UserTouchImage.setOnClickListener {
                val intent = Intent(itemView.context, UserInfoActivity::class.java)
                intent.putExtra("userId", feedInfoWithFollow.feedInfo.userId)
                itemView.context.startActivity(intent)
            }

            /*   게시글   */

            //이미지 존재 확인 후 스타일 결정
            if(feedInfoWithFollow.feedInfo.image.isEmpty()) {
                binding.MapImageLarge.visibility = View.VISIBLE

                binding.MapImageSmall.visibility = View.INVISIBLE
                binding.Image.visibility = View.INVISIBLE

                LoadImageUtils.loadImage(itemView.context, binding.MapImageLarge, feedInfoWithFollow.feedInfo.mapImage[1])
            } else {
                binding.MapImageLarge.visibility = View.INVISIBLE

                binding.MapImageSmall.visibility = View.VISIBLE
                binding.Image.visibility = View.VISIBLE

                LoadImageUtils.loadImage(itemView.context, binding.MapImageSmall, feedInfoWithFollow.feedInfo.mapImage[0])
                LoadImageUtils.loadImage(itemView.context, binding.Image, feedInfoWithFollow.feedInfo.image[0])
            }

            binding.TitleText.text = feedInfoWithFollow.feedInfo.title //제목
            binding.PostTimeText.text = feedInfoWithFollow.feedInfo.timeDifference
            binding.TotalTimeText.text = DateTimeUtils.getCourseTime(feedInfoWithFollow.feedInfo.visit.first().datetime, feedInfoWithFollow.feedInfo.visit.last().endtime)    //코스 소요 시간

            /*   팔로우   */
            if(Auth._userId.value == feedInfoWithFollow.feedInfo.userId){   //내 게시글 인 경우
                binding.FollowButton.visibility = View.GONE
                binding.ButtonText.visibility = View.GONE
            } else {
                binding.FollowButton.visibility = View.VISIBLE
                binding.ButtonText.visibility = View.VISIBLE

                if(feedInfoWithFollow.isFollowing){
                    binding.FollowButton.setImageResource(R.drawable.ripple_effect_feed_following)

                    binding.ButtonText.text = "팔로잉"
                    binding.ButtonText.setTextColor(ContextCompat.getColor(itemView.context, R.color.vecto_theme_orange))
                } else {
                    binding.FollowButton.setImageResource(R.drawable.ripple_effect_feed_follow)

                    binding.ButtonText.text = "팔로우"
                    binding.ButtonText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
            }

            binding.FollowButton.setOnClickListener {
                clickFollowAction(feedInfoWithFollow)
            }

            /*   좋아요   */
            if(feedInfoWithFollow.feedInfo.likeFlag){
                binding.LikeImage.setImageResource(R.drawable.post_like_on)
            } else {
                binding.LikeImage.setImageResource(R.drawable.post_like_off)
            }

            binding.LikeCountText.text = feedInfoWithFollow.feedInfo.likeCount.toString()

            binding.LikeTouchImage.setOnClickListener {
                clickLikeAction(feedInfoWithFollow)
            }

            /*   댓글   */
            binding.CommentCountText.text = feedInfoWithFollow.feedInfo.commentCount.toString()

            binding.CommentTouchImage.setOnClickListener {
                val intent = Intent(itemView.context, CommentActivity::class.java)
                intent.putExtra("feedID", feedInfoWithFollow.feedInfo.feedId)
                itemView.context.startActivity(intent)
            }

            itemView.setOnClickListener {
                feedActionListener?.onItemClick(adapterPosition)
            }
        }

        private fun clickLikeAction(feedInfoWithFollow: VectoService.FeedInfoWithFollow) {

            if(Auth.loginFlag.value == false) {
                RequestLoginUtils.requestLogin(itemView.context)
            } else if(actionPosition == -1){

                if(feedInfoWithFollow.feedInfo.likeFlag){
                    feedActionListener?.onDeleteFeedLike(feedInfoWithFollow.feedInfo.feedId)
                } else {

                    val anim = AnimationUtils.loadAnimation(itemView.context, R.anim.like_anim)

                    anim.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })

                    binding.LikeImage.startAnimation(anim)

                    feedActionListener?.onPostFeedLike(feedInfoWithFollow.feedInfo.feedId)
                }

                actionPosition = adapterPosition

            }

        }

        private fun clickFollowAction(feedInfoWithFollow: VectoService.FeedInfoWithFollow) {

            if(Auth.loginFlag.value == false) {
                RequestLoginUtils.requestLogin(itemView.context)
            } else if(actionPosition == -1) {

                if(feedInfoWithFollow.isFollowing){
                    feedActionListener?.onDeleteFollow(feedInfoWithFollow.feedInfo.userId)
                } else {
                    feedActionListener?.onPostFollow(feedInfoWithFollow.feedInfo.userId)
                }

                actionPosition = adapterPosition
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostSmallItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return feedInfoWithFollow.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(feedInfoWithFollow[position])
    }

    //좋아요 성공시 실행 함수
    fun postFeedLikeSuccess(){
        if(actionPosition != -1 ) {
            feedInfoWithFollow[actionPosition].feedInfo.likeFlag = true
            feedInfoWithFollow[actionPosition].feedInfo.likeCount++

            notifyItemChanged(actionPosition)
        }
    }

    //좋아요 취소 성공시 실행 함수
    fun deleteFeedLikeSuccess(){
        if(actionPosition != -1 ) {
            feedInfoWithFollow[actionPosition].feedInfo.likeFlag = false
            feedInfoWithFollow[actionPosition].feedInfo.likeCount--

            notifyItemChanged(actionPosition)
        }
    }

    //팔로우 요청 성공시 실행 함수
    fun postFollowSuccess(){
        if(actionPosition != -1 ) {
            feedInfoWithFollow[actionPosition].isFollowing = true

            notifyItemChanged(actionPosition)
        }
    }

    //팔로우 삭제 요청 성공시 실행 함수
    fun deleteFollowSuccess(){
        if(actionPosition != -1 ) {
            feedInfoWithFollow[actionPosition].isFollowing = false

            notifyItemChanged(actionPosition)
        }
    }

    fun addFeedData(){
        Log.d("FeedAdapter", "addFeedData")
        notifyItemRangeInserted(lastSize, (feedInfoWithFollow.size - lastSize))

        lastSize = feedInfoWithFollow.size
    }
}