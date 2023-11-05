package com.example.vecto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vecto.data.Auth
import com.example.vecto.databinding.ActivityCommentBinding
import com.example.vecto.dialog.LoginRequestDialog
import com.example.vecto.dialog.StartServiceDialog
import com.example.vecto.retrofit.VectoService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommentActivity : AppCompatActivity() {
    lateinit var binding: ActivityCommentBinding
    lateinit var myCommentAdapter: MyCommentAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val feedID = intent.getIntExtra("feedID", -1)
        myCommentAdapter = MyCommentAdapter(this)
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

            if(binding.EditContent.text.isEmpty())
                Toast.makeText(this, "댓글을 작성해주세요.", Toast.LENGTH_SHORT).show()
            else
            {
                if(feedID != -1)
                    sendComment(feedID, binding.EditContent.text.toString())
            }
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
}