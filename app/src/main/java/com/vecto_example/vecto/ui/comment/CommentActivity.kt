package com.vecto_example.vecto.ui.comment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    var content = ""

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

        /*   Comment Load 관련 Observer   */
        commentViewModel.commentErrorLiveData.observe(this) {
            Toast.makeText(this, getString(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT)
                .show()
        }

        commentViewModel.commentInfoLiveData.observe(this) {
            myCommentAdapter.addCommentData(it.comments)

            if (it.comments.isEmpty() && myCommentAdapter.commentInfo.isEmpty() && commentViewModel.lastPage) {
                setNoneImage()
            }
            else
                clearNoneImage()
        }

        commentViewModel.addCommentResult.observe(this) { commentResult ->
            commentResult.onSuccess {

                clearNoneImage()
                clearData()
                clearUI()

                Toast.makeText(this, "댓글을 등록하였습니다.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                if (it.message == "FAIL") {
                    Toast.makeText(this, getText(R.string.APIFailToastMessage), Toast.LENGTH_SHORT)
                        .show()
                } else if (it.message == "ERROR") {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        commentViewModel.updateCommentResult.observe(this) { updateCommentResult ->
            updateCommentResult.onSuccess {
                myCommentAdapter.commentInfo[editcommentPosition].content = content

                clearUI()

                Toast.makeText(this, "변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

        }

        /*   로딩 관련 Observer   */
        commentViewModel.isLoadingCenter.observe(this) {
            if (it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else
                binding.progressBarCenter.visibility = View.GONE
        }
        commentViewModel.isLoadingBottom.observe(this) {
            if (it)
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
        commentRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1)) {
                    if(!commentViewModel.checkLoading())
                    {
                        loadComment(feedID)
                    }

                }

            }
        })
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
                content = binding.EditContent.text.toString()

                if(feedID != -1) {
                    if(!commentViewModel.checkLoading()) {
                        if (myCommentAdapter.editFlag && editcommentId != -1)   //수정
                            commentViewModel.updateComment(VectoService.CommentUpdateRequest(editcommentId, content))
                        else    //댓글 추가
                            commentViewModel.addComment(feedID, content)
                    }
                }
            }
        }
    }

    private fun loadComment(feedId: Int) {
        if(!commentViewModel.lastPage) {
            if (Auth.loginFlag.value == true)
                commentViewModel.fetchPersonalCommentResults(feedId)
            else
                commentViewModel.fetchCommentResults(feedId)
        }
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

        clearNoneImage()
    }

    private fun clearData(){
        myCommentAdapter.commentInfo.clear()
        commentViewModel.initSetting()
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