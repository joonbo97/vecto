package com.vecto_example.vecto.ui.comment.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.databinding.CommentItemBinding
import com.vecto_example.vecto.dialog.ReportUserDialog
import com.vecto_example.vecto.popupwindow.EditDeletePopupWindow
import com.vecto_example.vecto.popupwindow.ReportPopupWindow
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.userinfo.UserInfoActivity
import com.vecto_example.vecto.utils.LoadImageUtils
import com.vecto_example.vecto.utils.RequestLoginUtils
import com.vecto_example.vecto.utils.ServerResponse

class MyCommentAdapter(): RecyclerView.Adapter<MyCommentAdapter.ViewHolder>(){
    var commentInfo = mutableListOf<VectoService.CommentResponse>()
    var lastSize = 0

    var editFlag = false

    var selectedPosition = -1   //댓글 선택 Position
    var actionPosition = -1     //댓글 좋아요, 삭제 Position

    //댓글 선택 Callback
    interface OnEditActionListener {
        fun onEditAction(commentId: Int, position: Int)
    }

    //댓글 좋아요, 삭제 Callback
    interface OnCommentActionListener {
        fun onSendLike(commentId: Int)
        fun onCancelLike(commentId: Int)
        fun onDelete(commentId: Int)
    }

    interface OnReportActionListener {
        fun onReportUser(userId: String, reportType: String, content: String?)
    }

    var editActionListener: OnEditActionListener? = null
    var commentActionListener: OnCommentActionListener? = null
    var reportActionListener: OnReportActionListener ? = null

    fun cancelEditing() {
        if (selectedPosition != -1) {
            editFlag = false
            notifyItemChanged(selectedPosition)
            selectedPosition = -1
        }
    }

    inner class ViewHolder(val binding: CommentItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: VectoService.CommentResponse) {
            /*댓글 프사 설정*/
            LoadImageUtils.loadUserProfileImage(itemView.context, binding.ProfileImage, comment.profileUrl)

            /*댓글 닉네임 설정*/
            binding.NicknameText.text = comment.nickName

            /*시간 및 수정 여부 설정*/
            if (comment.updatedBefore)
                binding.CommentTimeText.text = itemView.context.getString(R.string.comment_time_edited, comment.timeDifference)
            else
                binding.CommentTimeText.text = itemView.context.getString(R.string.comment_time, comment.timeDifference)


            /*댓글 내용 설정*/
            binding.CommentText.text = comment.content

            // 아이템 선택 상태에 따라 배경색 설정
            if (selectedPosition == adapterPosition) {
                // 선택된 아이템의 배경색 변경
                binding.constraintLayout.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.edit_course_highlight))
            } else {
                // 선택되지 않은 아이템의 배경색을 기본값으로 설정
                binding.constraintLayout.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
            }

            /*좋아요 설정*/
            if(comment.likeFlag)
                binding.CommentLikeImage.setImageResource(R.drawable.post_like_on)
            else
                binding.CommentLikeImage.setImageResource(R.drawable.post_like_off)

            /*좋아요 개수 설정*/
            binding.CommentLikeCountText.text = comment.commentCount.toString()


            /*   리스너 설정   */

            //프로필 사진 클릭
            binding.ProfileImage.setOnClickListener {
                val intent = Intent(itemView.context, UserInfoActivity::class.java)
                intent.putExtra("userId", comment.userId)
                itemView.context.startActivity(intent)
            }

            //좋아요 클릭
            binding.LikeTouchImage.setOnClickListener {
                clickLikeAction(comment)
            }

            itemView.setOnLongClickListener {
                if (!editFlag)
                    showPopupWindow(comment)

                true
            }
        }

        //좋아요 클릭 실행 함수
        private fun clickLikeAction(comment: VectoService.CommentResponse) {

            if (Auth.loginFlag.value == false) {

                RequestLoginUtils.requestLogin(itemView.context)

            } else if (actionPosition == -1) {

                if (comment.likeFlag) {

                    actionPosition = adapterPosition
                    commentActionListener?.onCancelLike(comment.commentId)

                } else {

                    val anim = AnimationUtils.loadAnimation(itemView.context, R.anim.like_anim)

                    anim.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })

                    binding.CommentLikeImage.startAnimation(anim)

                    actionPosition = adapterPosition
                    commentActionListener?.onSendLike(comment.commentId)

                }
            }
        }

        //PopupWindow 호출 함수
        private fun showPopupWindow(comment: VectoService.CommentResponse) {
            selectedPosition = adapterPosition

            notifyItemChanged(selectedPosition) //호출 시 배경 변경

            var isClicked = false

            if(Auth._userId.value != comment.userId){   //다른 User 인 경우

                val reportPopupWindow = ReportPopupWindow(itemView.context,
                    reportListener = {
                        isClicked = true

                        if (Auth.loginFlag.value == false) {
                            RequestLoginUtils.requestLogin(itemView.context)
                            dismissPopupWindow()
                            return@ReportPopupWindow
                        }

                        val reportUserDialog = ReportUserDialog(itemView.context)
                        reportUserDialog.showDialog()
                        reportUserDialog.onOkButtonClickListener = { selectedOptionId, detailContent ->
                            when(selectedOptionId) {
                                R.id.radioButton0 -> {
                                    reportActionListener?.onReportUser(comment.userId, ServerResponse.REPORT_TYPE_BAD_MANNER.code, null)
                                }

                                R.id.radioButton1 -> {
                                    reportActionListener?.onReportUser(comment.userId, ServerResponse.REPORT_TYPE_INSULT.code, null)
                                }
                                R.id.radioButton2 -> {
                                    reportActionListener?.onReportUser(comment.userId, ServerResponse.REPORT_TYPE_SEXUAL_HARASSMENT.code, null)
                                }
                                R.id.radioButton3 -> {
                                    if (detailContent != null) {
                                        reportActionListener?.onReportUser(comment.userId, ServerResponse.REPORT_TYPE_OTHER_PROBLEM.code, detailContent)
                                    }
                                    else{
                                        reportActionListener?.onReportUser(comment.userId, ServerResponse.REPORT_TYPE_OTHER_PROBLEM.code, null)
                                    }
                                }
                            }

                            dismissPopupWindow()
                        }
                    },
                    dismissListener = {
                        if (!isClicked)  //아무것도 선택하지 않고 닫힐 때만 실행
                            dismissPopupWindow()
                    }
                )

                //뷰를 기준으로 팝업 윈도우 표시
                reportPopupWindow.showPopupWindow(binding.ProfileImage)
            } else {    //본인 댓글인 경우

                val editDeletePopupWindow = EditDeletePopupWindow(itemView.context,
                    editListener = {
                        isClicked = true

                        if (Auth.loginFlag.value == false) {
                            RequestLoginUtils.requestLogin(itemView.context)
                            dismissPopupWindow()
                            return@EditDeletePopupWindow
                        } else if (editFlag) {
                            Toast.makeText(itemView.context, "한번에 하나의 댓글만 수정할 수 있습니다.", Toast.LENGTH_SHORT)
                                .show()
                            return@EditDeletePopupWindow
                        } else if (Auth._userId.value != comment.userId) {
                            Toast.makeText(itemView.context, "본인의 댓글만 수정할 수 있습니다.", Toast.LENGTH_SHORT).show()
                            dismissPopupWindow()
                            return@EditDeletePopupWindow
                        } else//로그인이 되어있고, 처음 선택하는 것이며, 본인의 댓글인 경우
                        {
                            binding.constraintLayout.setBackgroundColor(
                                ContextCompat.getColor(itemView.context, R.color.edit_course_highlight)
                            )
                            editActionListener?.onEditAction(comment.commentId, selectedPosition)
                        }

                    },
                    deleteListener = {
                        isClicked = true

                        if (Auth.loginFlag.value == false) {
                            RequestLoginUtils.requestLogin(itemView.context)
                            dismissPopupWindow()
                            return@EditDeletePopupWindow
                        }

                        actionPosition = selectedPosition   //adaperposition을 쓰지않는 이유는 popup 때문에 adapterposition이 유효한 값을 가지지 않아서
                        commentActionListener?.onDelete(comment.commentId)
                        dismissPopupWindow()
                    },

                    dismissListener = {
                        if (!isClicked)  //아무것도 선택하지 않고 닫힐 때만 실행
                            dismissPopupWindow()
                    })

                //뷰를 기준으로 팝업 윈도우 표시
                editDeletePopupWindow.showPopupWindow(binding.ProfileImage)
            }


        }

        private fun dismissPopupWindow() {
            if (selectedPosition != -1) {
                notifyItemChanged(selectedPosition)
                selectedPosition = -1
            }

        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = commentInfo[position]
        holder.bind(comment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return commentInfo.size
    }

    //좋아요 성공시 실행 함수
    fun sendCommentLikeSuccess(){

        commentInfo[actionPosition].likeFlag = true
        commentInfo[actionPosition].commentCount++

        notifyItemChanged(actionPosition)
    }

    //좋아요 취소 성공시 실행 함수
    fun cancelCommentLikeSuccess(){
        commentInfo[actionPosition].likeFlag = false
        commentInfo[actionPosition].commentCount--

        notifyItemChanged(actionPosition)
    }

    //삭제 성공시 실행 함수
    fun deleteCommentSuccess(){
        commentInfo.removeAt(actionPosition)
        lastSize = commentInfo.size

        notifyItemRemoved(actionPosition)
    }

    //댓글 데이터 추가 함수
    fun addCommentData(){
        notifyItemRangeInserted(lastSize, commentInfo.size - lastSize)

        lastSize = commentInfo.size
    }

}