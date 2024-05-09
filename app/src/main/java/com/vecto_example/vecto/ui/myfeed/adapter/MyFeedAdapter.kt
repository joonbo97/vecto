package com.vecto_example.vecto.ui.myfeed.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.dialog.DeletePostDialog
import com.vecto_example.vecto.popupwindow.EditDeletePopupWindow
import com.vecto_example.vecto.retrofit.VectoService
import com.google.gson.Gson
import com.vecto_example.vecto.ui.editfeed.EditFeedActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.databinding.MypostItemBinding
import com.vecto_example.vecto.utils.DateTimeUtils
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.RequestLoginUtils

class MyFeedAdapter(): RecyclerView.Adapter<MyFeedAdapter.ViewHolder>()
{
    var actionPosition = -1
    var feedInfo = mutableListOf<VectoService.FeedInfo>()
    var lastSize = 0


    interface OnFeedActionListener {
        fun onPostLike(feedID: Int)

        fun onDeleteLike(feedID: Int)

        fun onDeleteFeed(feedID: Int)

        fun onItemViewClick(position: Int)
    }

    var feedActionListener: OnFeedActionListener? = null

    inner class ViewHolder(val binding: MypostItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(feed: VectoService.FeedInfo) {

            /*   사용자 정보   */
            LoadImageUtils.loadUserProfileImage(itemView.context, binding.ProfileImage, feed.userProfile)    //프로필 이미지
            binding.NicknameText.text = feed.nickName   //닉네임

            /*   게시글   */

            //이미지 존재 확인 후 스타일 결정
            if (feed.image.isEmpty()) {
                binding.MapImageLarge.visibility = View.VISIBLE

                binding.MapImageSmall.visibility = View.INVISIBLE
                binding.Image.visibility = View.INVISIBLE

                LoadImageUtils.loadImage(itemView.context, binding.MapImageLarge, feed.mapImage[1])
            } else {
                binding.MapImageLarge.visibility = View.INVISIBLE

                binding.MapImageSmall.visibility = View.VISIBLE
                binding.Image.visibility = View.VISIBLE

                LoadImageUtils.loadImage(itemView.context, binding.MapImageSmall, feed.mapImage[0])
                LoadImageUtils.loadImage(itemView.context, binding.Image, feed.image[0])
            }

            binding.TitleText.text = feed.title //제목
            binding.PostTimeText.text = feed.timeDifference //업로드 시간
            binding.TotalTimeText.text = DateTimeUtils.getCourseTime(feed.visit.first().datetime, feed.visit.last().datetime)

            /*   게시글 메뉴   */
            if(Auth._userId.value != feedInfo[adapterPosition].userId) {
                binding.PostMenuImage.visibility = View.GONE
            }

            else {
                binding.PostMenuImage.setOnClickListener {
                    val editDeletePopupWindow = EditDeletePopupWindow(itemView.context,
                        editListener = {
                            val intent = Intent(itemView.context, EditFeedActivity::class.java).apply {
                                putExtra("feedInfoJson", Gson().toJson(feedInfo[adapterPosition]))
                                putExtra("feedId", feedInfo[adapterPosition].feedId)
                            }
                            itemView.context.startActivity(intent)
                        },
                        deleteListener = {
                            val deletePostDialog = DeletePostDialog(itemView.context)
                            deletePostDialog.showDialog()
                            deletePostDialog.onOkButtonClickListener = {
                                actionPosition = adapterPosition
                                feedActionListener?.onDeleteFeed(feedInfo[adapterPosition].feedId)
                            }
                        },
                        dismissListener = {

                        })

                    // 앵커 뷰를 기준으로 팝업 윈도우 표시
                    editDeletePopupWindow.showPopupWindow(binding.PostMenuImage)
                }
            }

            /*   좋아요   */
            if(feed.likeFlag)
                binding.LikeImage.setImageResource(R.drawable.post_like_on)
            else
                binding.LikeImage.setImageResource(R.drawable.post_like_off)

            binding.LikeCountText.text = feed.likeCount.toString()

            binding.LikeTouchImage.setOnClickListener {
                clickLikeAction(feedInfo[adapterPosition])
            }

            /*   댓글   */
            binding.CommentCountText.text = feed.commentCount.toString()

            binding.CommentTouchImage.setOnClickListener {
                val intent = Intent(itemView.context, CommentActivity::class.java)
                intent.putExtra("feedID", feedInfo[adapterPosition].feedId)
                itemView.context.startActivity(intent)
            }

            itemView.setOnClickListener {
                feedActionListener?.onItemViewClick(adapterPosition)
            }
        }

        //좋아요 클릭시 실행 함수
        private fun clickLikeAction(feed: VectoService.FeedInfo) {

            if(Auth.loginFlag.value == true && actionPosition == -1) {
                if (feed.likeFlag) {
                    feedActionListener?.onDeleteLike(feedInfo[adapterPosition].feedId)
                } else {
                    val anim = AnimationUtils.loadAnimation(itemView.context, R.anim.like_anim)

                    anim.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })

                    binding.LikeImage.startAnimation(anim)

                    feedActionListener?.onPostLike(feedInfo[adapterPosition].feedId)
                }

                actionPosition = adapterPosition
            }
            else {
                RequestLoginUtils.requestLogin(itemView.context)
            }

        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MypostItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return feedInfo.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feed = feedInfo[position]
        holder.bind(feed)
    }

    //좋아요 성공 시 실행 함수
    fun postFeedLikeSuccess() {
        if(actionPosition != -1) {
            feedInfo[actionPosition].likeFlag = true
            feedInfo[actionPosition].likeCount++

            notifyItemChanged(actionPosition)
        }
    }

    //좋아요 삭제 성공 시 실행 함수
    fun deleteFeedLikeSuccess() {
        if(actionPosition != -1) {
            feedInfo[actionPosition].likeFlag = false
            feedInfo[actionPosition].likeCount--

            notifyItemChanged(actionPosition)
        }
    }

    //게시글 삭제 성공 시 실행 함수
    fun deleteFeedSuccess() {
        if(actionPosition != -1) {
            feedInfo.removeAt(actionPosition)

            notifyItemRemoved(actionPosition)
        }
    }


    fun addFeedInfoData() {
        notifyItemRangeInserted(lastSize, feedInfo.size - lastSize)

        lastSize = feedInfo.size
    }

}