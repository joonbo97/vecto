package com.vecto_example.vecto.ui.followinfo.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.databinding.FollowUserlistLitemBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.userinfo.UserInfoActivity
import com.vecto_example.vecto.utils.LoadImageUtils

class FollowListAdapter(): RecyclerView.Adapter<FollowListAdapter.ViewHolder>() {
    val followList = mutableListOf<VectoService.FollowList>()

    var actionPosition = -1

    interface OnFollowActionListener {
        fun onPostFollow(userId: String)

        fun onDeleteFollow(userId: String)
    }

    var followActionListener: OnFollowActionListener? = null

    inner class ViewHolder(val binding: FollowUserlistLitemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(followUser: VectoService.FollowList) {
            /*   사용자 정보   */
            LoadImageUtils.loadUserProfileImage(itemView.context, binding.ProfileImage, followUser.profileUrl)  //프로필 이미지
            binding.NicknameText.text = followUser.nickName

            /*   팔로우 버튼 설정   */
            if(followUser.relation == "followed" || followUser.relation == "all")
                setFollowButton(true)
            else
                setFollowButton(false)

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, UserInfoActivity::class.java)
                intent.putExtra("userId", followUser.userId)
                itemView.context.startActivity(intent)
            }

            binding.FollowButton.setOnClickListener {
                if(followUser.relation == "followed" || followUser.relation == "all")
                    followActionListener?.onDeleteFollow(followUser.userId)
                else
                    followActionListener?.onPostFollow(followUser.userId)

                actionPosition = adapterPosition
            }
        }

        private fun setFollowButton(followFlag: Boolean) {
            if(followFlag){
                binding.FollowButton.setBackgroundResource(R.drawable.ripple_effect_feed_following)
                binding.ButtonText.text = "팔로잉"
                binding.ButtonText.setTextColor(ContextCompat.getColor(itemView.context, R.color.vecto_theme_orange))
            } else {
                binding.FollowButton.setBackgroundResource(R.drawable.ripple_effect_feed_follow)
                binding.ButtonText.text = "팔로우"
                binding.ButtonText.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FollowUserlistLitemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FollowListAdapter.ViewHolder, position: Int) {
        holder.bind(followList[position])
    }

    override fun getItemCount(): Int {
        return followList.size
    }

    //팔로우 요청 성공시 실행 함수
    fun postFollowSuccess(){
        if(actionPosition != -1 ) {
            if(followList[actionPosition].relation == "none")
                followList[actionPosition].relation = "followed"
            else if(followList[actionPosition].relation == "follower")
                followList[actionPosition].relation = "all"

            notifyItemChanged(actionPosition)
        }
    }

    //팔로우 삭제 요청 성공시 실행 함수
    fun deleteFollowSuccess(){
        if(actionPosition != -1 ) {
            if(followList[actionPosition].relation == "followed")
                followList[actionPosition].relation = "none"
            else if(followList[actionPosition].relation == "all")
                followList[actionPosition].relation = "follower"

            notifyItemChanged(actionPosition)
        }
    }

    fun addFollowListData(newData: List<VectoService.FollowList>){
        val startIdx = followList.size
        followList.addAll(newData)
        notifyItemRangeInserted(startIdx, newData.size)
    }

}