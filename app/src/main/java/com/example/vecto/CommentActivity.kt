package com.example.vecto

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vecto.data.Auth
import com.example.vecto.databinding.ActivityCommentBinding
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

    }

    private fun loadComment(feedid: Int) {

        val vectoService = VectoService.create()

        val call = vectoService.getComment(feedid)
        call.enqueue(object : Callback<VectoService.VectoResponse<VectoService.CommentListResponse>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<VectoService.CommentListResponse>>, response: Response<VectoService.VectoResponse<VectoService.CommentListResponse>>) {
                if(response.isSuccessful){
                    Log.d("COMMENTINFO", "성공: ${response.body()}")
                    val result = response.body()!!.result!!.comments

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
            }

        })

    }
}