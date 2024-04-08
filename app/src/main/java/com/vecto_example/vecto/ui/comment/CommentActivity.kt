package com.vecto_example.vecto.ui.comment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.vecto_example.vecto.ui.comment.adapter.MyCommentAdapter
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.databinding.ActivityCommentBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.RequestLoginUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommentActivity : AppCompatActivity(), MyCommentAdapter.OnEditActionListener {
    lateinit var binding: ActivityCommentBinding
    lateinit var myCommentAdapter: MyCommentAdapter

    private val commentViewModel: CommentViewModel by viewModels {
        CommentViewModelFactory(CommentRepository(VectoService.create()))
    }

    var editcommentId = -1
    var editcommentPosition = -1
    var feedID = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
        initRecyclerView()
        initObservers()

        feedID = intent.getIntExtra("feedID", -1)

        if(feedID != -1)
            loadComment(feedID)
        else
            Toast.makeText(this, "오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()

        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            binding.CommentRecyclerView.adapter = myCommentAdapter

            clearNoneImage()
            clearData()
            clearUI()

            if(feedID != -1)
                loadComment(feedID)
            else
                Toast.makeText(this, "오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()

            swipeRefreshLayout.isRefreshing = false
        }

    }

    private fun initObservers() {
        commentViewModel.commentInfoLiveData.observe(this){
            myCommentAdapter.addCommentData(it.comments)
        }

        /*   로딩 관련 Observer   */
        commentViewModel.isLoadingCenter.observe(this) {
            if(it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else
                binding.progressBarCenter.visibility = View.GONE
        }
        commentViewModel.isLoadingBottom.observe(this) {
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.GONE
        }
    }

    private fun initRecyclerView() {
        /*   RecyclerView 초기화 함수   */

        myCommentAdapter = MyCommentAdapter(this)
        myCommentAdapter.editActionListener = this
        val commentRecyclerView = binding.CommentRecyclerView
        commentRecyclerView.adapter = myCommentAdapter
        commentRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun initListener() {
        /*   리스너 초기화 함수   */

        //댓글 수정 취소
        binding.EditCommentBox.setOnClickListener {
            if(myCommentAdapter.editFlag)//수정중이라면
            {
                onEditCancelled()
            }
        }

        //뒤로 가기 버튼
        binding.BackButton.setOnClickListener {
            finish()
        }

        //댓글 작성 버튼
        binding.CommentButton.setOnClickListener {
            if(Auth.loginFlag.value == false)
            {
                RequestLoginUtils.requestLogin(this)
                return@setOnClickListener
            }

            if(binding.EditContent.text.isEmpty()) {
                if (myCommentAdapter.editFlag)
                    Toast.makeText(this, "댓글 수정 내용을 작성해 주세요.", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, "댓글을 작성해 주세요.", Toast.LENGTH_SHORT).show()
            }
            else
            {
                if(feedID != -1) {
                    if(true/*!loadingFlag*/) {
                        if (myCommentAdapter.editFlag && editcommentId != -1)
                            updateComment(VectoService.CommentUpdateRequest(editcommentId, binding.EditContent.text.toString()))
                        else
                            addComment(feedID, binding.EditContent.text.toString())
                    }
                }
            }

        }
    }


    private fun addComment(feedid: Int, text: String) {
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
        commentViewModel.fetchCommentResults(feedid)

        /*val vectoService = VectoService.create()

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

        })*/

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

                    clearUI()

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

        myCommentAdapter.editFlag = true

        editcommentPosition = position
        myCommentAdapter.selectedPosition = position

        editcommentId = commentId

        binding.EditContent.hint = "수정할 내용을 작성해 주세요."
    }

    private fun onEditCancelled() {
        clearUI()

        Toast.makeText(this, "댓글 수정이 취소 되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun clearUI(){
        binding.EditCommentBox.visibility = View.GONE
        binding.EditCommentText.visibility = View.GONE

        binding.EditContent.text.clear()
        binding.EditContent.hint = "댓글을 작성해 주세요."

        editcommentId = -1
        editcommentPosition = -1
        myCommentAdapter.cancelEditing()
    }

    private fun clearData(){
        myCommentAdapter.commentInfo.clear()
    }

    private fun clearNoneImage() {
        binding.CommentNullImage.visibility = View.GONE
        binding.CommentNullText.visibility = View.GONE
        Log.d("NONE GONE", "NONE IMAGE IS GONE")
    }

    private fun setNoneImage() {
        binding.CommentNullImage.visibility = View.VISIBLE
        binding.CommentNullText.visibility = View.VISIBLE
        Log.d("NONE VISIBLE", "NONE IMAGE IS VISIBLE")
    }

}