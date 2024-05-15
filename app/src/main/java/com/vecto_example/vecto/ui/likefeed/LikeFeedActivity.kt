package com.vecto_example.vecto.ui.likefeed

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityLikeFeedBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.search.adapter.FeedAdapter
import com.vecto_example.vecto.utils.FeedDetailType
import com.vecto_example.vecto.utils.ShareFeedUtil
import com.vecto_example.vecto.utils.ToastMessageUtils
import com.vecto_example.vecto.utils.ToastMessageUtils.errorMessageHandler

class LikeFeedActivity : AppCompatActivity(), FeedAdapter.OnFeedActionListener {
    private lateinit var binding: ActivityLikeFeedBinding

    private val likeFeedViewModel: LikeFeedViewModel by viewModels {
        LikeFeedViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
    }

    private lateinit var feedAdapter: FeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLikeFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
        initRecyclerView()
        initObservers()
        getFeed()

        binding.swipeRefreshLayout.setOnRefreshListener {

            if(!likeFeedViewModel.checkLoading()){
                likeFeedViewModel.initSetting()
                getFeed()
            }

            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun initListener() {
        /*   리스너 초기화 함수   */

        //뒤로 가기 버튼
        binding.BackButton.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initObservers() {
        /*   게시글 관련 Observer   */
        likeFeedViewModel.feedInfoLiveData.observe(this) {
            if(likeFeedViewModel.firstFlag) {
                feedAdapter.feedInfoWithFollow = likeFeedViewModel.allFeedInfo
                feedAdapter.lastSize = likeFeedViewModel.allFeedInfo.size

                feedAdapter.notifyDataSetChanged()
                likeFeedViewModel.firstFlag = false

                likeFeedViewModel.endLoading()
            } else {
                feedAdapter.addFeedData()

                likeFeedViewModel.endLoading()
            }


            if(likeFeedViewModel.allFeedInfo.isEmpty()){
                setNoneImage()
            } else {
                clearNoneImage()
            }
        }

        /*   로딩 관련 Observer   */
        likeFeedViewModel.isLoadingCenter.observe(this) {
            if(it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else
                binding.progressBarCenter.visibility = View.GONE
        }
        likeFeedViewModel.isLoadingBottom.observe(this) {
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.GONE
        }

        /*   게시물 상호 작용 관련 Observer   */

        /*   게시글 좋아요   */
        likeFeedViewModel.postFeedLikeResult.observe(this) { postFeedLikeResult ->
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

        likeFeedViewModel.deleteFeedLikeResult.observe(this) { deleteFeedLikeResult ->
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
        likeFeedViewModel.postFollowResult.observe(this) {
            if(feedAdapter.postFollowPosition != -1) {
                if (it) {
                    feedAdapter.postFollowSuccess()
                    ToastMessageUtils.showToast(this, getString(R.string.post_follow_success, likeFeedViewModel.allFeedInfo[feedAdapter.postFollowPosition].feedInfo.nickName))
                } else {
                    ToastMessageUtils.showToast(this, getString(R.string.post_follow_already, likeFeedViewModel.allFeedInfo[feedAdapter.postFollowPosition].feedInfo.nickName))
                }

                feedAdapter.postFollowPosition = -1
            }
        }

        likeFeedViewModel.deleteFollowResult.observe(this) {
            if(feedAdapter.deleteFollowPosition != -1) {
                if (it) {
                    feedAdapter.deleteFollowSuccess()
                    ToastMessageUtils.showToast(this, getString(R.string.delete_follow_success, likeFeedViewModel.allFeedInfo[feedAdapter.deleteFollowPosition].feedInfo.nickName))
                } else {
                    ToastMessageUtils.showToast(this, getString(R.string.delete_follow_already, likeFeedViewModel.allFeedInfo[feedAdapter.deleteFollowPosition].feedInfo.nickName))
                }

                feedAdapter.deleteFollowPosition = -1
            }
        }

        /*   오류 관련 Observer   */
        likeFeedViewModel.feedErrorLiveData.observe(this) {
            errorMessageHandler(this, ToastMessageUtils.UserInterActionType.FEED.name, it)
        }

        likeFeedViewModel.followErrorLiveData.observe(this) {
            errorMessageHandler(this, ToastMessageUtils.UserInterActionType.FOLLOW.name, it)

        }

        likeFeedViewModel.postFollowError.observe(this) {
            if(feedAdapter.postFollowPosition != -1) {
                errorMessageHandler(this, ToastMessageUtils.UserInterActionType.FOLLOW_POST.name, it)

                feedAdapter.postFollowPosition = -1
            }
        }

        likeFeedViewModel.deleteFollowError.observe(this) {
            if(feedAdapter.deleteFollowPosition != -1) {
                errorMessageHandler(this, ToastMessageUtils.UserInterActionType.FOLLOW_DELETE.name, it)

                feedAdapter.deleteFollowPosition = -1
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initRecyclerView() {
        feedAdapter = FeedAdapter()
        feedAdapter.feedActionListener = this

        val likePostRecyclerView = binding.LikeFeedRecyclerView
        likePostRecyclerView.adapter = feedAdapter
        likePostRecyclerView.itemAnimator = null

        likePostRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        likePostRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1)) {
                    if(!likeFeedViewModel.checkLoading()){
                        getFeed()
                    }
                }
            }
        })

        if(likeFeedViewModel.allFeedInfo.isNotEmpty()){
            feedAdapter.feedInfoWithFollow = likeFeedViewModel.allFeedInfo
            feedAdapter.notifyDataSetChanged()
        }
    }

    private fun getFeed() {
        likeFeedViewModel.getLikeFeedList()
        Log.d("getFeed", "Like Feed")
    }

    //좋아요 게시물 None 이미지
    private fun setNoneImage() {
        binding.NoneImage.visibility = View.VISIBLE
        binding.NoneText.visibility = View.VISIBLE
    }


    private fun clearNoneImage() {
        binding.NoneImage.visibility = View.GONE
        binding.NoneText.visibility = View.GONE
    }

    /*   Adapter CallBack 관련   */
    override fun onPostFeedLike(feedId: Int) {
        if(!likeFeedViewModel.postLikeLoading) {
            likeFeedViewModel.postFeedLike(feedId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            feedAdapter.postLikePosition = -1
        }
    }

    override fun onDeleteFeedLike(feedId: Int) {
        if(!likeFeedViewModel.deleteLikeLoading) {
            likeFeedViewModel.deleteFeedLike(feedId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            feedAdapter.deleteLikePosition = -1
        }
    }

    override fun onPostFollow(userId: String) {
        if(!likeFeedViewModel.postFollowLoading) {
            likeFeedViewModel.postFollow(userId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            feedAdapter.postFollowPosition = -1
        }
    }

    override fun onDeleteFollow(userId: String) {
        if(!likeFeedViewModel.deleteFollowLoading) {
            likeFeedViewModel.deleteFollow(userId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            feedAdapter.deleteFollowPosition = -1
        }
    }

    override fun onItemClick(position: Int) {
        var subList = likeFeedViewModel.allFeedInfo.subList(position, likeFeedViewModel.allFeedInfo.size)
        if(subList.size > 10) {
            subList = subList.subList(0, 10)
        }

        val intent = Intent(this, FeedDetailActivity::class.java).apply {
            putExtra("feedInfoListJson", Gson().toJson(subList))
            putExtra("type", FeedDetailType.INTENT_LIKE.code)
            putExtra("query", "")
            putExtra("nextPage", likeFeedViewModel.nextPage)
            putExtra("followPage", likeFeedViewModel.followPage)
            putExtra("lastPage", likeFeedViewModel.lastPage)
        }

        this.startActivity(intent)
    }

    override fun onShareClick(feedInfo: VectoService.FeedInfo) {
        ShareFeedUtil.shareFeed(this, feedInfo)
    }
}