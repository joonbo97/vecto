package com.vecto_example.vecto.ui.detail.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.PathData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.databinding.PostDetailItemBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.decoration.VerticalOverlapItemDecoration
import com.vecto_example.vecto.ui.userinfo.UserInfoActivity
import com.vecto_example.vecto.utils.DateTimeUtils
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.RequestLoginUtils

class MyFeedDetailAdapter(): RecyclerView.Adapter<MyFeedDetailAdapter.ViewHolder>() {
    var actionPosition = -1

    var feedInfoWithFollow = mutableListOf<VectoService.FeedInfoWithFollow>()
    var lastSize = 0

    interface OnFeedActionListener {
        fun onPostFeedLike(feedId: Int)

        fun onDeleteFeedLike(feedId: Int)

        fun onPostFollow(userId: String)

        fun onDeleteFollow(userId: String)

        fun onTitleClick(position: Int)

        fun onShareClick(feedInfoWithFollow: VectoService.FeedInfoWithFollow)

        fun onVisitItemClick(visitData: VisitData, itemPosition: Int)

        fun onPathItemClick(pathData: PathData)
    }

    var feedActionListener: OnFeedActionListener? = null


    inner class ViewHolder(val binding: PostDetailItemBinding): RecyclerView.ViewHolder(binding.root) {
        private var visitListAdapter = VisitListAdapter()

        fun bind(feedInfoWithFollow: VectoService.FeedInfoWithFollow) {

            /*   이미지 설정   */
            if(feedInfoWithFollow.feedInfo.image.isEmpty())
            {
                binding.viewPager.visibility = View.GONE
                binding.indicator.visibility = View.GONE
                binding.topPageNumberText.visibility = View.GONE
                binding.topPageNumberBox.visibility = View.GONE
            }
            else if(feedInfoWithFollow.feedInfo.image.size == 1){
                binding.viewPager.visibility = View.VISIBLE
                binding.indicator.visibility = View.VISIBLE
                binding.topPageNumberText.visibility = View.GONE
                binding.topPageNumberBox.visibility = View.GONE

                binding.viewPager.adapter = ImageSliderAdapter(itemView.context, feedInfoWithFollow.feedInfo.image)

                binding.indicator.setViewPager(binding.viewPager)   //하나일 경우 indicator만 설정
            } else {
                binding.viewPager.visibility = View.VISIBLE
                binding.indicator.visibility = View.VISIBLE
                binding.topPageNumberText.visibility = View.VISIBLE
                binding.topPageNumberBox.visibility = View.VISIBLE

                val imageSize = feedInfoWithFollow.feedInfo.image.size

                binding.viewPager.adapter = ImageSliderAdapter(itemView.context, feedInfoWithFollow.feedInfo.image)
                binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        val textToShow = "${position + 1}/${imageSize}"
                        binding.topPageNumberText.text = textToShow // 각 ViewHolder의 textIndicator를 업데이트합니다.
                    }
                })

                binding.indicator.setViewPager(binding.viewPager)
            }

            /*   게시글 설정   */
            binding.TitleText.text = feedInfoWithFollow.feedInfo.title  //제목 설정

            binding.titleTouchImage.setOnClickListener {
                feedActionListener?.onTitleClick(adapterPosition)
            }

            binding.TotalTimeText.text = DateTimeUtils.getCourseTime(   //소요 시간 설정
                feedInfoWithFollow.feedInfo.visit.first().datetime,
                feedInfoWithFollow.feedInfo.visit.last().datetime)

            binding.PostTimeText.text = feedInfoWithFollow.feedInfo.timeDifference  //업로드 시간

            if(feedInfoWithFollow.feedInfo.content.isEmpty()){   //내용
                binding.ContentText.visibility = View.GONE
            } else {
                binding.ContentText.visibility = View.VISIBLE
                binding.ContentText.text = feedInfoWithFollow.feedInfo.content
            }

            /*   게시글 작성자 설정   */
            LoadImageUtils.loadUserProfileImage(itemView.context, binding.ProfileImage, feedInfoWithFollow.feedInfo.userProfile)

            binding.UserNameText.text = feedInfoWithFollow.feedInfo.nickName

            binding.ProfileImage.setOnClickListener {
                val intent = Intent(itemView.context, UserInfoActivity::class.java)
                intent.putExtra("userId", feedInfoWithFollow.feedInfo.userId)
                itemView.context.startActivity(intent)
            }

            /*   방문지 목록 설정   */
            bindVisitData(feedInfoWithFollow.feedInfo.visit, feedInfoWithFollow.feedInfo.location)

            /*   좋아요 설정   */
            binding.LikeCount.text = feedInfoWithFollow.feedInfo.likeCount.toString()

            if(feedInfoWithFollow.feedInfo.likeFlag)
                binding.LikeImage.setImageResource(R.drawable.post_like_on)
            else
                binding.LikeImage.setImageResource(R.drawable.post_like_off)

            binding.LikeTouchImage.setOnClickListener {
                clickLikeAction(feedInfoWithFollow)
            }

            /*   댓글 설정   */
            binding.CommentCount.text = feedInfoWithFollow.feedInfo.commentCount.toString()

            binding.CommentTouchImage.setOnClickListener {
                val intent = Intent(itemView.context, CommentActivity::class.java)
                intent.putExtra("feedID", feedInfoWithFollow.feedInfo.feedId)
                itemView.context.startActivity(intent)
            }

            /*   팔로우 설정   */
            if(feedInfoWithFollow.isFollowing){
                binding.FollowButton.setBackgroundResource(R.drawable.ripple_effect_feed_following)
                binding.FollowButtonText.text = "팔로잉"
                binding.FollowButtonText.setTextColor(ContextCompat.getColor(itemView.context, R.color.vecto_theme_orange))
            } else {
                binding.FollowButton.setBackgroundResource(R.drawable.ripple_effect_feed_follow)
                binding.FollowButtonText.text = "팔로우"
                binding.FollowButtonText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            }

            if(Auth._userId.value == feedInfoWithFollow.feedInfo.userId){
                binding.FollowButton.visibility = View.GONE
                binding.FollowButtonText.visibility = View.GONE
            } else {
                binding.FollowButton.visibility = View.VISIBLE
                binding.FollowButtonText.visibility = View.VISIBLE
            }

            binding.FollowButton.setOnClickListener {
                clickFollowAction(feedInfoWithFollow)
            }

            binding.ShareTouchImage.setOnClickListener {
                feedActionListener?.onShareClick(feedInfoWithFollow)
            }

        }

        private fun clickLikeAction(feedInfoWithFollow: VectoService.FeedInfoWithFollow) {
            if(Auth.loginFlag.value == false) {
                RequestLoginUtils.requestLogin(itemView.context)
            } else if(actionPosition == -1) {

                if(feedInfoWithFollow.feedInfo.likeFlag) {
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

        init {
            // 내부 RecyclerView 설정
            binding.PostDetailRecyclerView.adapter = visitListAdapter
            binding.PostDetailRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)

            binding.PostDetailRecyclerView.addItemDecoration(VerticalOverlapItemDecoration(60))

            visitListAdapter.detailItemClickListener = object : VisitListAdapter.OnDetailItemClickListener{
                override fun onVisitItemClick(visitData: VisitData, itemPosition: Int) {
                    feedActionListener?.onVisitItemClick(visitData, itemPosition)
                }

                override fun onPathItemClick(pathData: PathData) {
                    feedActionListener?.onPathItemClick(pathData)
                }
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun bindVisitData(visitDataList: List<VisitData>, allPathData: List<LocationData>) {
            visitListAdapter.visitData.clear()
            visitListAdapter.visitData.addAll(visitDataList)

            visitListAdapter.pathData.clear()

            val locationDataForPath = mutableListOf<LocationData>()
            var cnt = 1

            for (locationData in allPathData) {
                if(visitDataList.size > 1) {
                    if (locationData.datetime == visitDataList[cnt].datetime) {
                        locationDataForPath.add(locationData)
                        val pathData = PathData(locationDataForPath.toMutableList())
                        visitListAdapter.pathData.add(pathData)

                        locationDataForPath.clear()
                        locationDataForPath.add(locationData)

                        cnt++

                        if (cnt == visitDataList.size) {
                            break
                        }
                    } else {
                        locationDataForPath.add(locationData)
                    }
                }
            }

            visitListAdapter.notifyDataSetChanged()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(feedInfoWithFollow[position])
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostDetailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return feedInfoWithFollow.size
    }

    fun postFeedLikeSuccess() {
        if(actionPosition != -1 ) {
            feedInfoWithFollow[actionPosition].feedInfo.likeFlag = true
            feedInfoWithFollow[actionPosition].feedInfo.likeCount++

            notifyItemChanged(actionPosition)
        }
    }

    fun deleteFeedLikeSuccess() {
        if(actionPosition != -1 ) {
            feedInfoWithFollow[actionPosition].feedInfo.likeFlag = false
            feedInfoWithFollow[actionPosition].feedInfo.likeCount--

            notifyItemChanged(actionPosition)
        }
    }

    fun postFollowSuccess() {
        if(actionPosition != -1 ) {
            feedInfoWithFollow[actionPosition].isFollowing = true

            notifyItemChanged(actionPosition)
        }
    }

    fun deleteFollowSuccess() {
        if(actionPosition != -1 ) {
            feedInfoWithFollow[actionPosition].isFollowing = false

            notifyItemChanged(actionPosition)
        }
    }

    fun addFeedData(){
        Log.d("MyFeedDetailAdapter", "addFeedData")
        notifyItemRangeInserted(lastSize, (feedInfoWithFollow.size - lastSize))
        Log.d("MyFeedDetailAdapter", "${lastSize}, ${feedInfoWithFollow.size}")

        lastSize = feedInfoWithFollow.size
    }


}