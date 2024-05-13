package com.vecto_example.vecto.ui.onefeed

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityOneFeedBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.comment.CommentActivity
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.search.SearchViewModel
import com.vecto_example.vecto.ui.search.SearchViewModelFactory
import com.vecto_example.vecto.ui.search.adapter.FeedAdapter
import com.vecto_example.vecto.utils.FeedDetailType
import com.vecto_example.vecto.utils.ShareFeedUtil

class OneFeedActivity : AppCompatActivity(), FeedAdapter.OnFeedActionListener {
    private lateinit var binding: ActivityOneFeedBinding

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
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
            Toast.makeText(this, "게시글 정보가 올바르지 않습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()

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

        viewModel.deleteFeedLikeResult.observe(this) { deleteFeedLikeResult ->
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
        viewModel.postFollowResult.observe(this) {
            if(feedAdapter.actionPosition != -1) {
                if (it) {
                    feedAdapter.postFollowSuccess()
                    Toast.makeText(this, "${feedAdapter.feedInfoWithFollow[0].feedInfo.nickName} 님을 팔로우하기 시작했습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미 ${feedAdapter.feedInfoWithFollow[0].feedInfo.nickName} 님을 팔로우 중입니다.", Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFollowResult.observe(this) {
            if(feedAdapter.actionPosition != -1) {
                if (it) {
                    feedAdapter.deleteFollowSuccess()
                    Toast.makeText(this, "${feedAdapter.feedInfoWithFollow[0].feedInfo.nickName} 님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미 ${feedAdapter.feedInfoWithFollow[0].feedInfo.nickName} 님을 팔로우하지 않습니다.", Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        /*   오류 관련 Observer   */
        viewModel.feedErrorLiveData.observe(this) {
            if(it == "FAIL") {
                Toast.makeText(this, "게시글 불러오기에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else if(it == "ERROR") {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.followErrorLiveData.observe(this) {
            if(it == "FAIL") {
                Toast.makeText(this, "팔로우 정보 불러오기에 실패하였습니다.", Toast.LENGTH_SHORT).show()
            } else if(it == "ERROR") {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

        }

        viewModel.postFollowError.observe(this) {
            if(feedAdapter.actionPosition != -1) {
                if (it == "FAIL") {
                    Toast.makeText(this, "팔로우 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFollowError.observe(this) {
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


    /*   Adapter CallBack 관련   */
    override fun onPostFeedLike(feedId: Int) {
        if(!viewModel.checkLoading()) {
            viewModel.postFeedLike(feedId)
        } else {
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFeedLike(feedId: Int) {
        if(!viewModel.checkLoading()) {
            viewModel.deleteFeedLike(feedId)
        } else {
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
        }
    }

    override fun onPostFollow(userId: String) {
        if(!viewModel.checkLoading()) {
            viewModel.postFollow(userId)
        } else {
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFollow(userId: String) {
        if(!viewModel.checkLoading()) {
            viewModel.deleteFollow(userId)
        } else {
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
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

        if(!viewModel.checkLoading())
            this.startActivity(intent)
        else
            Toast.makeText(this, "이전 작업을 처리중 입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
    }

    override fun onShareClick(feedInfo: VectoService.FeedInfo) {
        ShareFeedUtil.shareFeed(this, feedInfo)
    }

}