package com.vecto_example.vecto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.databinding.ActivityCommentBinding
import com.vecto_example.vecto.dialog.LoginRequestDialog
import com.vecto_example.vecto.retrofit.VectoService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommentActivity : AppCompatActivity(), MyCommentAdapter.OnEditActionListener {
    lateinit var binding: ActivityCommentBinding
    lateinit var myCommentAdapter: MyCommentAdapter

    var editFlag = false
    var editcommentId = -1
    var editcommentPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.EditCommentBox.setOnClickListener {
            if(editFlag)//수정중이라면
            {
                onEditCancelled()
            }
        }


        val feedID = intent.getIntExtra("feedID", -1)
        myCommentAdapter = MyCommentAdapter(this)
        myCommentAdapter.editActionListener = this
        val commentRecyclerView = binding.CommentRecyclerView
        commentRecyclerView.adapter = myCommentAdapter
        commentRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        if(feedID != -1)
            loadComment(feedID)
        else
            Toast.makeText(this, "오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()


        binding.CommentButton.setOnClickListener {
            if(Auth.loginFlag.value == false)
            {
                val loginRequestDialog = LoginRequestDialog(this)
                loginRequestDialog.showDialog()
                loginRequestDialog.onOkButtonClickListener = {
                    val intent = Intent(this, LoginActivity::class.java)
                    this.startActivity(intent)
                }
                return@setOnClickListener
            }

            if(binding.EditContent.text.isEmpty()) {
                if (editFlag)
                    Toast.makeText(this, "댓글 수정 내용을 작성해 주세요.", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, "댓글을 작성해 주세요.", Toast.LENGTH_SHORT).show()
            }
            else
            {
                if(feedID != -1) {
                    if(editFlag && editcommentId != -1)
                        updateComment(VectoService.CommentUpdateRequest(editcommentId, binding.EditContent.text.toString()))
                    else
                        sendComment(feedID, binding.EditContent.text.toString())
                }
            }
        }

        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            myCommentAdapter = MyCommentAdapter(this)
            myCommentAdapter.editActionListener = this
            binding.CommentRecyclerView.adapter = myCommentAdapter

            binding.CommentNullImage.visibility = View.GONE
            binding.CommentNullText.visibility = View.GONE

            myCommentAdapter.commentInfo.clear()
            myCommentAdapter.editFlag = false
            myCommentAdapter.selectedPosition = -1
            editFlag = false
            editcommentPosition = -1
            binding.EditContent.hint = "댓글을 작성해 주세요."
            binding.EditCommentBox.visibility = View.GONE
            binding.EditCommentText.visibility = View.GONE


            commentRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

            if(feedID != -1)
                loadComment(feedID)
            else
                Toast.makeText(this, "오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()

            swipeRefreshLayout.isRefreshing = false
        }

    }


    private fun sendComment(feedid: Int, text: String) {
        val vectoService = VectoService.create()

        val call = vectoService.sendComment("Bearer ${Auth.token}", VectoService.CommentRequest(feedid, text))
        call.enqueue(object : Callback<VectoService.VectoResponse<String>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<String>>, response: Response<VectoService.VectoResponse<String>>) {
                if(response.isSuccessful){
                    Log.d("COMMENTADD", "성공: ${response.body()}")
                    Toast.makeText(this@CommentActivity, "댓글을 등록하였습니다.", Toast.LENGTH_SHORT).show()

                    binding.EditContent.text.clear()

                    loadComment(feedid)
                }
                else{
                    Log.d("COMMENTADD", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    Toast.makeText(this@CommentActivity, "오류가 발생하였습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()

                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<String>>, t: Throwable) {
                Log.d("COMMENTADD", "실패")
                Toast.makeText(this@CommentActivity, getString(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()

            }

        })
    }

    private fun loadComment(feedid: Int) {
        val vectoService = VectoService.create()

        val call: Call<VectoService.VectoResponse<VectoService.CommentListResponse>>

        if(Auth.loginFlag.value == true)
        {
            call = vectoService.getComment("Bearer ${Auth.token}", feedid)
        }
        else
        {
            call = vectoService.getComment(feedid)
        }
        call.enqueue(object : Callback<VectoService.VectoResponse<VectoService.CommentListResponse>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<VectoService.CommentListResponse>>, response: Response<VectoService.VectoResponse<VectoService.CommentListResponse>>) {
                if(response.isSuccessful){
                    Log.d("COMMENTINFO", "성공: ${response.body()}")
                    val result = response.body()!!.result!!.comments

                    myCommentAdapter.commentInfo.clear()

                    if(result.isEmpty()){
                        binding.CommentNullImage.visibility = View.VISIBLE
                        binding.CommentNullText.visibility = View.VISIBLE
                    }
                    else {
                        binding.CommentNullImage.visibility = View.GONE
                        binding.CommentNullText.visibility = View.GONE
                    }

                    for(i in result.indices)
                        myCommentAdapter.commentInfo.add(result[i])

                    myCommentAdapter.notifyDataSetChanged()
                }
                else{
                    Log.d("COMMENTINFO", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<VectoService.CommentListResponse>>, t: Throwable) {
                Log.d("COMMENTINFO", "실패")
                Toast.makeText(this@CommentActivity, getString(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
            }

        })

    }

    private fun updateComment(commentUpdateRequest: VectoService.CommentUpdateRequest) {
        val vectoService = VectoService.create()

        val call = vectoService.updateComment("Bearer ${Auth.token}", commentUpdateRequest)
        call.enqueue(object : Callback<VectoService.VectoResponse<String>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<String>>, response: Response<VectoService.VectoResponse<String>>) {
                if (response.isSuccessful) {
                    // 성공
                    Log.d("UserUpdate", "업데이트 성공 : " + response.message())
                    Toast.makeText(this@CommentActivity, "변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    myCommentAdapter.commentInfo[editcommentPosition].content = commentUpdateRequest.content

                    myCommentAdapter.cancelEditing()
                    binding.EditContent.hint = "댓글을 작성해 주세요."
                    myCommentAdapter.editFlag = false
                    editcommentId = -1
                    editcommentPosition = -1
                    binding.EditCommentBox.visibility = View.GONE
                    binding.EditCommentText.visibility = View.GONE
                    binding.EditContent.text.clear()

                } else {
                    // 실패
                    Log.d("UserUpdate", "업데이트 실패 : " + response.errorBody()?.string())
                    Toast.makeText(this@CommentActivity, "오류가 발생했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }

            }
            override fun onFailure(call: Call<VectoService.VectoResponse<String>>, t: Throwable) {
                // 네트워크 등 기타 에러 처리
                Toast.makeText(this@CommentActivity, getText(R.string.APIFailToastMessage), Toast.LENGTH_SHORT).show()
                Log.d("UserUpdate", "업데이트 실패 : " + t.message)
            }
        })
    }

    override fun onEditAction(commentId: Int, position: Int) {
        binding.EditCommentBox.visibility = View.VISIBLE
        binding.EditCommentText.visibility = View.VISIBLE

        editcommentId = commentId
        editcommentPosition = position
        editFlag = true
        binding.EditContent.hint = "수정할 내용을 작성해 주세요."
    }

    fun onEditCancelled() {
        myCommentAdapter.cancelEditing()
        Toast.makeText(this, "댓글 수정이 취소 되었습니다.", Toast.LENGTH_SHORT).show()
        binding.EditContent.hint = "댓글을 작성해 주세요."

        myCommentAdapter.editFlag = false
        editcommentId = -1
        editcommentPosition = -1
        binding.EditCommentBox.visibility = View.GONE
        binding.EditCommentText.visibility = View.GONE

    }

}