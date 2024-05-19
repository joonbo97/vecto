package com.vecto_example.vecto.ui.onefeed

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityOneFeedBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.search.SearchViewModel
import com.vecto_example.vecto.ui.search.SearchViewModelFactory
import com.vecto_example.vecto.ui.search.adapter.FeedAdapter
import com.vecto_example.vecto.utils.FeedDetailType
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ShareFeedUtil
import com.vecto_example.vecto.utils.ToastMessageUtils
import com.vecto_example.vecto.utils.ToastMessageUtils.errorMessageHandler

class OneFeedActivity : AppCompatActivity(), FeedAdapter.OnFeedActionListener {
    private lateinit var binding: ActivityOneFeedBinding

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()), TokenRepository(VectoService.create()))
    }

    private lateinit var feedAdapter: FeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOneFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val feedId = intent.getIntExtra("feedId", -1)
        val isComment = intent.getBooleanExtra("isComment", false)

        initListener()
        initRecyclerView()
        initObservers()

        if(feedId != -1)
            viewModel.getOneFeed(feedId)
        else
            ToastMessageUtils.showToast(this, getString(R.string.basic_error))

        if(isComment){
            val intent = Intent(this, CommentActivity::class.java)
            intent.putExtra("feedID", feedId)
            this.startActivity(intent)
        }
    }

    private fun initListener() {
        /*   리스너 초기화 함수   */

        //뒤로 가기 버튼
        binding.BackButton.setOnClickListener {
            finish()
        }
    }
    private fun initRecyclerView() {
        /*   Recycler 초기화 함수   */
        feedAdapter = FeedAdapter()
        feedAdapter.feedActionListener = this

        val oneFeedRecyclerView = binding.oneFeedRecyclerView
        oneFeedRecyclerView.adapter = feedAdapter

        oneFeedRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        oneFeedRecyclerView.itemAnimator = null
    }

    private fun initObservers() {
        /*   게시글 관련 Observer   */
        viewModel.oneFeedLiveData.observe(this) {
            feedAdapter.feedInfoWithFollow.add(it)

            feedAdapter.notifyItemInserted(0)
        }

        /*   로딩 관련 Observer   */
        viewModel.isLoadingCenter.observe(this) {
            if(it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else
                binding.progressBarCenter.visibility = View.GONE
        }

        /*   게시글 좋아요   */
        viewModel.postFeedLikeResult.observe(this) { postFeedLikeResult ->
            if(feedAdapter.postLikePosition != -1)
            {
                postFeedLikeResult.onSuccess {
                    feedAdapter.postFeedLikeSuccess()
                }.onFailure {
                    ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
                }

                feedAdapter.postLikePosition = -1
            }
        }

        viewModel.deleteFeedLikeResult.observe(this) { deleteFeedLikeResult ->
            if(feedAdapter.deleteLikePosition != -1) {
                deleteFeedLikeResult.onSuccess {
                    feedAdapter.deleteFeedLikeSuccess()
                }.onFailure {
                    ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
                }

                feedAdapter.deleteLikePosition = -1
            }
        }

        //팔로우
        viewModel.postFollowResult.observe(this) {
            if(feedAdapter.postFollowPosition != -1) {
                if (it) {
                    feedAdapter.postFollowSuccess()
                    ToastMessageUtils.showToast(this, getString(R.string.post_follow_success, viewModel.allFeedInfo[0].feedInfo.nickName))
                } else {
                    ToastMessageUtils.showToast(this, getString(R.string.post_follow_already, viewModel.allFeedInfo[0].feedInfo.nickName))
                }

                feedAdapter.postFollowPosition = -1
            }
        }

        viewModel.deleteFollowResult.observe(this) {
            if(feedAdapter.deleteFollowPosition != -1) {
                if (it) {
                    feedAdapter.deleteFollowSuccess()
                    ToastMessageUtils.showToast(this, getString(R.string.delete_follow_success, viewModel.allFeedInfo[0].feedInfo.nickName))
                } else {
                    ToastMessageUtils.showToast(this, getString(R.string.delete_follow_already, viewModel.allFeedInfo[0].feedInfo.nickName))
                }

                feedAdapter.deleteFollowPosition = -1
            }
        }

        viewModel.reissueResponse.observe(this) {
            SaveLoginDataUtils.changeToken(this, viewModel.accessToken, viewModel.refreshToken)

            when(it){
                SearchViewModel.Function.PostFeedLike.name -> {
                    viewModel.postFollow(viewModel.postFollowId)
                }
                SearchViewModel.Function.DeleteFeedLike.name -> {
                    viewModel.deleteFeedLike(viewModel.deleteFeedLikeId)
                }
                SearchViewModel.Function.PostFollow.name -> {
                    viewModel.postFollow(viewModel.postFollowId)
                }
                SearchViewModel.Function.DeleteFollow.name -> {
                    viewModel.deleteFollow(viewModel.deleteFollowId)
                }
            }
        }

        /*   오류 관련 Observer   */
        viewModel.errorMessage.observe(this) {
            ToastMessageUtils.showToast(this, getString(it))
        }

        viewModel.followErrorLiveData.observe(this) {
            errorMessageHandler(this, ToastMessageUtils.UserInterActionType.FOLLOW.name, it)

        }

        viewModel.postFollowError.observe(this) {
            if(feedAdapter.postFollowPosition != -1) {
                errorMessageHandler(this, ToastMessageUtils.UserInterActionType.FOLLOW_POST.name, it)

                feedAdapter.postFollowPosition = -1
            }
        }

        viewModel.deleteFollowError.observe(this) {
            if(feedAdapter.deleteFollowPosition != -1) {
                errorMessageHandler(this, ToastMessageUtils.UserInterActionType.FOLLOW_DELETE.name, it)

                feedAdapter.deleteFollowPosition = -1
            }
        }
    }


    /*   Adapter CallBack 관련   */
    override fun onPostFeedLike(feedId: Int) {
        if(!viewModel.postLikeLoading) {
            viewModel.postFeedLike(feedId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            feedAdapter.postLikePosition = -1
        }
    }

    override fun onDeleteFeedLike(feedId: Int) {
        if(!viewModel.deleteLikeLoading) {
            viewModel.deleteFeedLike(feedId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            feedAdapter.deleteLikePosition = -1
        }
    }

    override fun onPostFollow(userId: String) {
        if(!viewModel.postFollowLoading) {
            viewModel.postFollow(userId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            feedAdapter.postFollowPosition = -1
        }
    }

    override fun onDeleteFollow(userId: String) {
        if(!viewModel.deleteFollowLoading) {
            viewModel.deleteFollow(userId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            feedAdapter.deleteFollowPosition = -1
        }
    }

    override fun onItemClick(position: Int) {
        var subList = viewModel.allFeedInfo.subList(position, viewModel.allFeedInfo.size)
        if(subList.size > 10) {
            subList = subList.subList(0, 10)
        }


        val intent = Intent(this, FeedDetailActivity::class.java).apply {
            putExtra("feedInfoListJson", Gson().toJson(subList))

            putExtra("type", FeedDetailType.INTENT_NORMAL.code)
            putExtra("query", "")

            putExtra("nextPage", 0)
            putExtra("followPage", true)
            putExtra("lastPage", false)
        }

        this.startActivity(intent)
    }

    override fun onShareClick(feedInfo: VectoService.FeedInfo) {
        ShareFeedUtil.shareFeed(this, feedInfo)
    }

}