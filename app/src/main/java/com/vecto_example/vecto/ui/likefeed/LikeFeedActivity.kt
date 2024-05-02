package com.vecto_example.vecto.ui.likefeed

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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
                clearNoneImage()

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
            } else {
                feedAdapter.addFeedData()
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
            if(feedAdapter.actionPosition != -1)
            {
                postFeedLikeResult.onSuccess {
                    feedAdapter.postFeedLikeSuccess()
                }.onFailure {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        likeFeedViewModel.deleteFeedLikeResult.observe(this) { deleteFeedLikeResult ->
            if(feedAdapter.actionPosition != -1) {
                deleteFeedLikeResult.onSuccess {
                    feedAdapter.deleteFeedLikeSuccess()
                }.onFailure {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        //팔로우
        likeFeedViewModel.postFollowResult.observe(this) {
            if(feedAdapter.actionPosition != -1) {
                if (it) {
                    feedAdapter.postFollowSuccess()
                    Toast.makeText(this, "${likeFeedViewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName} 님을 팔로우하기 시작했습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미 ${likeFeedViewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName} 님을 팔로우 중입니다.", Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        likeFeedViewModel.deleteFollowResult.observe(this) {
            if(feedAdapter.actionPosition != -1) {
                if (it) {
                    feedAdapter.deleteFollowSuccess()
                    Toast.makeText(this, "${likeFeedViewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName} 님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미 ${likeFeedViewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName} 님을 팔로우하지 않습니다.", Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        /*   오류 관련 Observer   */
        likeFeedViewModel.feedErrorLiveData.observe(this) {
            if(it == "FAIL") {
                Toast.makeText(this, "게시글 불러오기에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else if(it == "ERROR") {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }

        likeFeedViewModel.followErrorLiveData.observe(this) {
            if(it == "FAIL") {
                Toast.makeText(this, "팔로우 정보 불러오기에 실패하였습니다.", Toast.LENGTH_SHORT).show()
            } else if(it == "ERROR") {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

        }

        likeFeedViewModel.postFollowError.observe(this) {
            if(feedAdapter.actionPosition != -1) {
                if (it == "FAIL") {
                    Toast.makeText(this, "팔로우 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        likeFeedViewModel.deleteFollowError.observe(this) {
            if(feedAdapter.actionPosition != -1) {
                if (it == "FAIL") {
                    Toast.makeText(this, "팔로우 취소 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initRecyclerView() {
        feedAdapter = FeedAdapter()
        feedAdapter.feedActionListener = this

        val likePostRecyclerView = binding.LikeFeedRecyclerView
        likePostRecyclerView.adapter = feedAdapter

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
        Log.d("NONE SET", "NONE IMAGE SET")
    }


    private fun clearNoneImage() {
        binding.NoneImage.visibility = View.GONE
        binding.NoneText.visibility = View.GONE
        Log.d("NONE GONE", "NONE IMAGE IS GONE")
    }

    /*   Adapter CallBack 관련   */
    override fun onPostFeedLike(feedId: Int) {
        if(!likeFeedViewModel.checkLoading()) {
            likeFeedViewModel.postFeedLike(feedId)
        } else {
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFeedLike(feedId: Int) {
        if(!likeFeedViewModel.checkLoading()) {
            likeFeedViewModel.deleteFeedLike(feedId)
        } else {
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
        }
    }

    override fun onPostFollow(userId: String) {
        if(!likeFeedViewModel.checkLoading()) {
            likeFeedViewModel.postFollow(userId)
        } else {
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFollow(userId: String) {
        if(!likeFeedViewModel.checkLoading()) {
            likeFeedViewModel.deleteFollow(userId)
        } else {
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
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

        if(!likeFeedViewModel.checkLoading())
            this.startActivity(intent)
        else
            Toast.makeText(this, "이전 작업을 처리중 입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
    }
}