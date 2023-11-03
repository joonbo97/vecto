package com.example.vecto

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.vecto.retrofit.VectoService

class MyCommentAdapter(private val context: Context): RecyclerView.Adapter<MyCommentAdapter.ViewHolder>(){
    //TODO 좋아요 flag, 좋아요 개수
    val commentInfo = mutableListOf<VectoService.CommentResponse>()


    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.ProfileImage)
        val likeImage: ImageView = view.findViewById(R.id.CommentLikeImage)

        val nicknameText: TextView = view.findViewById(R.id.NicknameText)
        val timeText: TextView = view.findViewById(R.id.CommentTimeText)
        val commentText: TextView = view.findViewById(R.id.CommentText)
        val likeCount: TextView = view.findViewById(R.id.CommentLikeCountText)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = commentInfo[position]

        /*댓글 프사 설정*/
        if(comment.profileUrl == null)
        {
            holder.profileImage.setImageResource(R.drawable.profile_basic)
        }
        else
        {
            Glide.with(context)
                .load(comment.profileUrl)
                .placeholder(R.drawable.profile_basic) // 로딩 중 표시될 이미지
                .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
                .circleCrop()
                .into(holder.profileImage)
        }

        /*댓글 닉네임 설정*/
        holder.nicknameText.text = comment.nickName

        /*시간 설정*/
        holder.timeText.text = " · " + comment.timeDifference

        /*댓글 내용 설정*/
        holder.commentText.text = comment.content

        //TODO 좋아요 설정
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyCommentAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return commentInfo.size
    }

}