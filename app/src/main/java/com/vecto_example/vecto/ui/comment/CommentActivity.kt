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
import com.vecto_example.vecto.data.repository.CommentRepository
import com.vecto_example.vecto.databinding.ActivityCommentBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.RequestLoginUtils

class CommentActivity : AppCompatActivity(), MyCommentAdapter.OnEditActionListener, MyCommentAdapter.OnCommentActionListener {
    private lateinit var binding: ActivityCommentBinding
    private lateinit var myCommentAdapter: MyCommentAdapter

    private val commentViewModel: CommentViewModel by viewModels {
        CommentViewModelFactory(CommentRepository(VectoService.create()))
    }

    private var editcommentId = -1
    private var editcommentPosition = -1    //adapter에서 다른 view를 호출 했을 수도 있기 때문에 현재 수정중인 position을 가지고 있어야함
    private var feedID = -1

    private var content = ""

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

        /*   Comment Like Observer   */
        commentViewModel.sendCommentLikeResult.observe(this) { sendCommentLikeResult ->
            sendCommentLikeResult.onSuccess {
                myCommentAdapter.sendCommentLikeSuccess()
            }.onFailure {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myCommentAdapter.actionPosition = -1
        }

        commentViewModel.cancelCommentLikeResult.observe(this) { cancelCommentLikeResult ->
            cancelCommentLikeResult.onSuccess {
                myCommentAdapter.cancelCommentLikeSuccess()
            }.onFailure {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myCommentAdapter.actionPosition = -1
        }

        /*   Comment Update Observer   */
        commentViewModel.updateCommentResult.observe(this) { updateCommentResult ->
            updateCommentResult.onSuccess {
                myCommentAdapter.commentInfo[editcommentPosition].content = content

                clearUI()

                Toast.makeText(this, "변경이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

        }

        /*   Comment Delete Observer   */
        commentViewModel.deleteCommentResult.observe(this) { deleteCommentResult ->
            deleteCommentResult.onSuccess {
                myCommentAdapter.deleteCommentSuccess()
                Toast.makeText(this, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myCommentAdapter.actionPosition = -1
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
        myCommentAdapter.commentActionListener = this
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

    private fun onEditCancelled() {
        clearUI()

        Toast.makeText(this, "댓글 수정이 취소 되었습니다.", Toast.LENGTH_SHORT).show()
    }

    //UI 초기화 함수
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

    //데이터 초기화 함수
    private fun clearData(){
        myCommentAdapter.commentInfo.clear()
        commentViewModel.initSetting()
    }

    //NoneImage 제거 함수
    private fun clearNoneImage() {
        binding.CommentNullImage.visibility = View.GONE
        binding.CommentNullText.visibility = View.GONE
        Log.d("NONE GONE", "NONE IMAGE IS GONE")
    }

    //NoneImage 호출 함수
    private fun setNoneImage() {
        binding.CommentNullImage.visibility = View.VISIBLE
        binding.CommentNullText.visibility = View.VISIBLE
        Log.d("NONE VISIBLE", "NONE IMAGE IS VISIBLE")
    }


    //수정 선택시 실행
    override fun onEditAction(commentId: Int, position: Int) {
        binding.EditCommentBox.visibility = View.VISIBLE
        binding.EditCommentText.visibility = View.VISIBLE

        myCommentAdapter.editFlag = true

        editcommentPosition = position
        myCommentAdapter.selectedPosition = position

        editcommentId = commentId

        binding.EditContent.hint = "수정할 내용을 작성해 주세요."
    }

    //좋아요 선택시 실행
    override fun onSendLike(commentId: Int) {
        if(!commentViewModel.checkLoading()) {
            commentViewModel.sendCommentLike(commentId)
        }
        else
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
    }

    //좋아요 취소시 실행
    override fun onCancelLike(commentId: Int) {
        if(!commentViewModel.checkLoading())
            commentViewModel.cancelCommentLike(commentId)
        else
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
    }

    //댓글 삭제시 실행
    override fun onDelete(commentId: Int) {
        if(!commentViewModel.checkLoading())
            commentViewModel.deleteComment(commentId)
        else
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
    }

}