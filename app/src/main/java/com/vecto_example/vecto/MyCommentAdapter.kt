package com.vecto_example.vecto

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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vecto_example.vecto.model.data.Auth
import com.vecto_example.vecto.dialog.EditDeletePopupWindow
import com.vecto_example.vecto.dialog.LoginRequestDialog
import com.vecto_example.vecto.retrofit.VectoService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyCommentAdapter(private val context: Context): RecyclerView.Adapter<MyCommentAdapter.ViewHolder>(){
    val commentInfo = mutableListOf<VectoService.CommentResponse>()
    var editFlag = false

    var selectedPosition = -1
    interface OnEditActionListener {
        fun onEditAction(commentId: Int, position: Int)
    }

    var editActionListener: OnEditActionListener? = null

    fun cancelEditing() {
        if (selectedPosition != -1) {
            editFlag = false
            notifyItemChanged(selectedPosition)
        }
    }


    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.ProfileImage)
        val likeImage: ImageView = view.findViewById(R.id.CommentLikeImage)

        val nicknameText: TextView = view.findViewById(R.id.NicknameText)
        val timeText: TextView = view.findViewById(R.id.CommentTimeText)
        val commentText: TextView = view.findViewById(R.id.CommentText)
        val likeCount: TextView = view.findViewById(R.id.CommentLikeCountText)

        val menu: ImageView = view.findViewById(R.id.CommentMenuImage)

        val constraintLayout: ConstraintLayout = view.findViewById(R.id.constraintLayout)
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
        holder.profileImage.setOnClickListener {
            val intent = Intent(context, UserInfoActivity::class.java)
            intent.putExtra("userId", commentInfo[position].userId)
            context.startActivity(intent)
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
                val loginRequestDialog = LoginRequestDialog(context)
                loginRequestDialog.showDialog()
                loginRequestDialog.onOkButtonClickListener = {
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                }
            }
        }


        holder.likeImage.setOnClickListener {
            clickLikeAction()
        }
        holder.likeCount.setOnClickListener {
            clickLikeAction()
        }

        if(selectedPosition != -1 && !editFlag) {
            holder.constraintLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            selectedPosition = -1
        }
        else if(editFlag && selectedPosition == position)
            holder.constraintLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.vecto_alphagray))



        holder.menu.setOnClickListener {
            val editDeletePopupWindow = EditDeletePopupWindow(context,
                editListener = {
                    if(Auth.loginFlag.value == false)
                    {
                        val loginRequestDialog = LoginRequestDialog(context)
                        loginRequestDialog.showDialog()
                        loginRequestDialog.onOkButtonClickListener = {
                            val intent = Intent(context, LoginActivity::class.java)
                            context.startActivity(intent)
                        }

                        return@EditDeletePopupWindow
                    }
                    else if(editFlag)
                    {
                        Toast.makeText(context, "한번에 하나의 댓글만 수정할 수 있습니다.", Toast.LENGTH_SHORT).show()
                        return@EditDeletePopupWindow
                    }
                    else if(Auth._userId.value != commentInfo[position].userId)
                    {
                        Toast.makeText(context, "본인의 댓글만 수정할 수 있습니다.", Toast.LENGTH_SHORT).show()
                        return@EditDeletePopupWindow
                    }
                    else//로그인이 되어있고, 처음 선택하는 것이며, 본인의 댓글인 경우
                    {
                        holder.constraintLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.vecto_alphagray))
                        editActionListener?.onEditAction(comment.commentId, position)
                        selectedPosition = position
                    }

                },
                deleteListener = {
                    deleteComment(commentInfo[position].commentId, position)
                })

            // 앵커 뷰를 기준으로 팝업 윈도우 표시
            editDeletePopupWindow.showPopupWindow(holder.menu)
        }

    }

    private fun deleteComment(commentId: Int, position: Int) {

        val vectoService = VectoService.create()
        Log.d("COMMENTDELETE", commentId.toString())

        val call = vectoService.deleteComment("Bearer ${Auth.token}", commentId)
        call.enqueue(object : Callback<VectoService.VectoResponse<Unit>>{
            override fun onResponse(call: Call<VectoService.VectoResponse<Unit>>, response: Response<VectoService.VectoResponse<Unit>>) {
                if(response.isSuccessful){
                    Log.d("COMMENTDELETE", "성공: ${response.body()}")
                    Toast.makeText(context, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    commentInfo.removeAt(position)
                    notifyDataSetChanged()
                }
                else{
                    Log.d("COMMENTDELETE", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    Toast.makeText(context, "댓글 삭제에 실패하였습니다. 잠시후 시도해주세요.", Toast.LENGTH_SHORT).show()

                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Unit>>, t: Throwable) {
                Log.d("COMMENTDELETE", "실패 ${t.message.toString()}" )
                Toast.makeText(context, R.string.APIFailToastMessage, Toast.LENGTH_SHORT).show()
            }

        })
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