package com.vecto_example.vecto.ui.comment

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.ui.comment.adapter.MyCommentAdapter
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.CommentRepository
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityCommentBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModel
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModelFactory
import com.vecto_example.vecto.utils.RequestLoginUtils
import com.vecto_example.vecto.utils.ToastMessageUtils

class CommentActivity : AppCompatActivity(), MyCommentAdapter.OnEditActionListener, MyCommentAdapter.OnCommentActionListener,
    MyCommentAdapter.OnReportActionListener {
    private lateinit var binding: ActivityCommentBinding
    private lateinit var myCommentAdapter: MyCommentAdapter

    private val commentViewModel: CommentViewModel by viewModels {
        CommentViewModelFactory(CommentRepository(VectoService.create()))
    }

    private val userInfoViewModel: UserInfoViewModel by viewModels {
        UserInfoViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
    }

    private var editCommentId = -1
    private var editCommentPosition = -1    //adapter에서 다른 view를 호출 했을 수도 있기 때문에 현재 수정중인 position을 가지고 있어야함
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
            ToastMessageUtils.showToast(this, getString(R.string.basic_error))
        binding.swipeRefreshLayout.setOnRefreshListener {
            if(myCommentAdapter.editFlag)
                clearUI(true)
            else
                clearUI(false)
            commentViewModel.initSetting()

            if(feedID != -1)
                loadComment(feedID)
            else
                ToastMessageUtils.showToast(this, getString(R.string.basic_error))

            binding.swipeRefreshLayout.isRefreshing = false
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initObservers() {

        /*   Comment Load 관련 Observer   */
        commentViewModel.commentErrorLiveData.observe(this) {
            ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
        }

        commentViewModel.commentInfoLiveData.observe(this) {
            if(commentViewModel.firstFlag){
                myCommentAdapter.commentInfo = commentViewModel.allCommentInfo
                myCommentAdapter.lastSize = commentViewModel.allCommentInfo.size

                myCommentAdapter.notifyDataSetChanged()
                commentViewModel.firstFlag = false
            } else {
                myCommentAdapter.addCommentData()
            }

            if(commentViewModel.allCommentInfo.isEmpty()){
                setNoneImage()
            } else {
                clearNoneImage()
            }
        }

        commentViewModel.addCommentResult.observe(this) { commentResult ->
            commentResult.onSuccess {

                commentViewModel.initSetting()

                clearUI(true)

                loadComment(feedID)

                ToastMessageUtils.showToast(this, getString(R.string.comment_add_success))
            }.onFailure {
                if (it.message == "FAIL") {
                    ToastMessageUtils.showToast(this, getString(R.string.APIFailToastMessage))
                } else if (it.message == "ERROR") {
                    ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
                }
            }
        }

        /*   Comment Like Observer   */
        commentViewModel.sendCommentLikeResult.observe(this) { sendCommentLikeResult ->
            sendCommentLikeResult.onSuccess {
                myCommentAdapter.sendCommentLikeSuccess()
            }.onFailure {
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
            }

            myCommentAdapter.actionPosition = -1
        }

        commentViewModel.cancelCommentLikeResult.observe(this) { cancelCommentLikeResult ->
            cancelCommentLikeResult.onSuccess {
                myCommentAdapter.cancelCommentLikeSuccess()
            }.onFailure {
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
            }

            myCommentAdapter.actionPosition = -1
        }

        /*   Comment Update Observer   */
        commentViewModel.updateCommentResult.observe(this) { updateCommentResult ->
            updateCommentResult.onSuccess {
                myCommentAdapter.commentInfo[editCommentPosition].content = content

                clearUI(true)

                ToastMessageUtils.showToast(this, getString(R.string.comment_update_success))
            }.onFailure {
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
            }

        }

        /*   Comment Delete Observer   */
        commentViewModel.deleteCommentResult.observe(this) { deleteCommentResult ->
            deleteCommentResult.onSuccess {
                myCommentAdapter.deleteCommentSuccess()
                ToastMessageUtils.showToast(this, getString(R.string.comment_delete_success))
            }.onFailure {
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
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

        /*   신고 Observer   */
        userInfoViewModel.postComplaintResult.observe(this) {
            if(it) {
                ToastMessageUtils.showToast(this, getString(R.string.report_success))
            }
        }

        userInfoViewModel.postComplaintError.observe(this) {
            if(it == "FAIL"){
                ToastMessageUtils.showToast(this, getString(R.string.basic_error))
            } else {
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
            }
        }
    }


    private fun initRecyclerView() {
        /*   RecyclerView 초기화 함수   */

        myCommentAdapter = MyCommentAdapter()
        myCommentAdapter.editActionListener = this
        myCommentAdapter.commentActionListener = this
        myCommentAdapter.reportActionListener = this
        val commentRecyclerView = binding.CommentRecyclerView
        commentRecyclerView.adapter = myCommentAdapter
        commentRecyclerView.itemAnimator = null
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
                    ToastMessageUtils.showToast(this, getString(R.string.comment_edit_error_empty))
                else
                    ToastMessageUtils.showToast(this, getString(R.string.comment_error_empty))
            }
            else
            {
                content = binding.EditContent.text.toString()

                if(feedID != -1) {
                    if(!commentViewModel.checkLoading()) {
                        if (myCommentAdapter.editFlag && editCommentId != -1)   //수정
                            commentViewModel.updateComment(VectoService.CommentUpdateRequest(editCommentId, content))
                        else    //댓글 추가
                            commentViewModel.addComment(feedID, content)
                    }
                }
            }
        }
    }

    private fun loadComment(feedId: Int) {
        if(!commentViewModel.lastPage) {

            commentViewModel.getCommentList(feedId)
        }
    }

    private fun onEditCancelled() {
        clearUI(true)
        binding.EditContent.text = null

        ToastMessageUtils.showToast(this, getString(R.string.comment_edit_cancel))
    }

    //UI 초기화 함수
    private fun clearUI(flag: Boolean){ //true 면, 사용자 입력 값도 초기화
        binding.EditCommentBox.visibility = View.GONE
        binding.EditCommentText.visibility = View.GONE

        editCommentId = -1
        editCommentPosition = -1
        myCommentAdapter.cancelEditing()

        clearNoneImage()

        if(flag)
            binding.EditContent.text = null
    }

    //NoneImage 제거 함수
    private fun clearNoneImage() {
        binding.CommentNullImage.visibility = View.GONE
        binding.CommentNullText.visibility = View.GONE
    }

    //NoneImage 호출 함수
    private fun setNoneImage() {
        binding.CommentNullImage.visibility = View.VISIBLE
        binding.CommentNullText.visibility = View.VISIBLE
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {

        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

        return super.dispatchTouchEvent(ev)
    }


    //수정 선택시 실행
    override fun onEditAction(commentId: Int, position: Int) {
        binding.EditCommentBox.visibility = View.VISIBLE
        binding.EditCommentText.visibility = View.VISIBLE

        binding.EditContent.setText("${myCommentAdapter.commentInfo[position].content}")

        myCommentAdapter.editFlag = true

        editCommentPosition = position
        myCommentAdapter.selectedPosition = position

        editCommentId = commentId
    }

    //좋아요 선택시 실행
    override fun onSendLike(commentId: Int) {
        if(!commentViewModel.checkLoading()) {
            commentViewModel.sendCommentLike(commentId)
        }
        else
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
    }

    //좋아요 취소시 실행
    override fun onCancelLike(commentId: Int) {
        if(!commentViewModel.checkLoading())
            commentViewModel.cancelCommentLike(commentId)
        else
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
    }

    //댓글 삭제시 실행
    override fun onDelete(commentId: Int) {
        if(!commentViewModel.checkLoading())
            commentViewModel.deleteComment(commentId)
        else
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
    }

    override fun onReportUser(userId: String, reportType: String, content: String?) {
        userInfoViewModel.postComplaint(VectoService.ComplaintRequest(reportType, userId, content))
    }

}