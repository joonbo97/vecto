package com.vecto_example.vecto.ui.detail.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.databinding.PostDetailItemBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.DateTimeUtils
import com.vecto_example.vecto.utils.LoadImageUtils

class MyFeedDetailAdapter(): RecyclerView.Adapter<MyFeedDetailAdapter.ViewHolder>() {
    var actionPosition = -1
    var feedInfoWithFollow = mutableListOf<VectoService.FeedInfoWithFollow>()
    var lastSize = 0

    interface OnFeedActionListener {
        fun onPostFeedLike(feedId: Int)

        fun onDeleteFeedLike(feedId: Int)

        fun onPostFollow(userId: String)

        fun onDeleteFollow(userId: String)
    }

    var feedActionListener: OnFeedActionListener? = null


    inner class ViewHolder(val binding: PostDetailItemBinding): RecyclerView.ViewHolder(binding.root) {
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

            /*   방문지 목록 설정   */
            bindVisitData(feedInfoWithFollow.feedInfo.visit)

            /*   좋아요 설정   */
            binding.LikeCount.text = feedInfoWithFollow.feedInfo.likeCount.toString()

            if(feedInfoWithFollow.feedInfo.likeFlag)
                binding.LikeImage.setImageResource(R.drawable.post_like_on)
            else
                binding.LikeImage.setImageResource(R.drawable.post_like_off)

            /*   댓글 설정   */
            binding.CommentCount.text = feedInfoWithFollow.feedInfo.commentCount.toString()

            binding.CommentTouchImage.setOnClickListener {
                val intent = Intent(itemView.context, CommentActivity::class.java)
                intent.putExtra("feedID", feedInfoWithFollow.feedInfo.feedId)
                itemView.context.startActivity(intent)
            }

            /*   팔로우 설정   */
            if(feedInfoWithFollow.isFollowing){

            } else {

            }

        }

        private var visitNumberAdapter: VisitNumberAdapter = VisitNumberAdapter(itemView.context)

        init {
            // 내부 RecyclerView 설정
            binding.PostDetailRecyclerView.adapter = visitNumberAdapter
            binding.PostDetailRecyclerView.layoutManager = LinearLayoutManager(
                itemView.context, LinearLayoutManager.VERTICAL, false)
        }

        @SuppressLint("NotifyDataSetChanged")
        fun bindVisitData(visitDataList: List<VisitData>) {
            // VisitNumberAdapter에 데이터 전달 및 업데이트
            visitNumberAdapter.visitdataList.clear()
            visitNumberAdapter.visitdataList.addAll(visitDataList)
            visitNumberAdapter.notifyDataSetChanged()
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

    fun addFeedData(){
        Log.d("MyFeedDetailAdapter", "addFeedData")
        notifyItemRangeInserted(lastSize, (feedInfoWithFollow.size - lastSize))
        Log.d("MyFeedDetailAdapter", "${lastSize}, ${feedInfoWithFollow.size}")

        lastSize = feedInfoWithFollow.size
    }

    fun postFeedLikeSuccess() {
        TODO("Not yet implemented")
    }

    fun deleteFeedLikeSuccess() {
        TODO("Not yet implemented")
    }

    fun postFollowSuccess() {
        TODO("Not yet implemented")
    }

    fun deleteFollowSuccess() {
        TODO("Not yet implemented")
    }
}