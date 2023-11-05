package com.example.vecto

import android.content.Context
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
import com.example.vecto.data.Auth
import com.example.vecto.retrofit.VectoService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyCommentAdapter(private val context: Context): RecyclerView.Adapter<MyCommentAdapter.ViewHolder>(){
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

        /*댓글 좋아요 설정*/
        if(comment.likeFlag)
            holder.likeImage.setImageResource(R.drawable.post_like_on)
        else
            holder.likeImage.setImageResource(R.drawable.post_like_off)
        holder.likeCount.text = comment.commentCount.toString()


        fun clickLikeAction() {
            if(Auth.loginFlag.value == true) {
                if (comment.likeFlag) {
                    holder.likeImage.setImageResource(R.drawable.post_like_off)

                    cancelCommentLike(comment.commentId)
                    comment.likeFlag = false

                    comment.commentCount--
                    holder.likeCount.text = comment.commentCount.toString()
                } else {
                    holder.likeImage.setImageResource(R.drawable.post_like_on)
                    val anim = AnimationUtils.loadAnimation(context, R.anim.like_anim)
                    comment.commentCount++
                    holder.likeCount.text = comment.commentCount.toString()

                    anim.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })

                    holder.likeImage.startAnimation(anim)
                    sendCommentLike(comment.commentId)
                    comment.likeFlag = true
                }
            }
            else {
                Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }


        holder.likeImage.setOnClickListener {
            clickLikeAction()
        }
        holder.likeCount.setOnClickListener {
            clickLikeAction()
        }




    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyCommentAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return commentInfo.size
    }

    private fun sendCommentLike(commentId: Int) {
        Log.d("CommentID", commentId.toString())

        val vectoService = VectoService.create()

        val call = vectoService.sendCommentLike("Bearer ${Auth.token}", commentId)
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if(response.isSuccessful){
                    Log.d("COMMENTLIKE", "성공: ${response.body()}")
                }
                else{
                    Log.d("COMMENTLIKE", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("COMMENTLIKE", "실패 ${t.message.toString()}" )
            }

        })
    }

    private fun cancelCommentLike(commentId: Int) {
        val vectoService = VectoService.create()

        val call = vectoService.cancelCommentLike("Bearer ${Auth.token}", commentId)
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if(response.isSuccessful){
                    Log.d("COMMENTLIKE", "성공: ${response.body()}")
                }
                else{
                    Log.d("COMMENTLIKE", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("COMMENTLIKE", "실패 ${t.message.toString()}" )
            }

        })
    }

}