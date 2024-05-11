package com.vecto_example.vecto.ui.myfeed

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
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityMyFeedBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.myfeed.adapter.MyFeedAdapter
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModel
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModelFactory
import com.vecto_example.vecto.utils.FeedDetailType

class MyFeedActivity : AppCompatActivity(), MyFeedAdapter.OnFeedActionListener {
    private lateinit var binding: ActivityMyFeedBinding

    private val viewModel: UserInfoViewModel by viewModels {
        UserInfoViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
    }

    private lateinit var myFeedAdapter: MyFeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
        initRecyclerView()
        initObservers()
        getFeed()

        binding.swipeRefreshLayout.setOnRefreshListener {

            if(!viewModel.checkLoading()){
                viewModel.initSetting()

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
    private fun initRecyclerView() {
        myFeedAdapter = MyFeedAdapter()
        myFeedAdapter.feedActionListener = this

        val myFeedRecyclerView = binding.MyFeedRecyclerView
        myFeedRecyclerView.adapter = myFeedAdapter
        myFeedRecyclerView.itemAnimator = null

        myFeedRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        myFeedRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1)) {
                    if(!viewModel.checkLoading())
                    {
                        getFeed()
                    }
                }
            }
        })

        if(viewModel.allFeedInfo.isNotEmpty()){
            myFeedAdapter.feedInfo = viewModel.allFeedInfo
            myFeedAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initObservers() {
        /*   게시글 관련 Observer   */
        viewModel.feedInfoLiveData.observe(this) {
            //새로운 feed 정보를 받았을 때의 처리
            if(viewModel.firstFlag){
                myFeedAdapter.feedInfo = viewModel.allFeedInfo
                myFeedAdapter.lastSize = viewModel.allFeedInfo.size

                myFeedAdapter.notifyDataSetChanged()
                viewModel.firstFlag = false

                viewModel.endLoading()
            } else {
                myFeedAdapter.addFeedInfoData()

                viewModel.endLoading()
            }

            if(viewModel.allFeedInfo.isEmpty()){
                setNoneImage()
            } else {
                clearNoneImage()
            }
        }

        /*   게시글 좋아요   */
        viewModel.postFeedLikeResult.observe(this) { postFeedLikeResult ->
            postFeedLikeResult.onSuccess {
                myFeedAdapter.postFeedLikeSuccess()
            }.onFailure {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myFeedAdapter.actionPosition = -1
        }

        viewModel.deleteFeedLikeResult.observe(this) { deleteFeedLikeResult ->
            deleteFeedLikeResult.onSuccess {
                myFeedAdapter.deleteFeedLikeSuccess()
            }.onFailure {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myFeedAdapter.actionPosition = -1
        }

        /*   게시글 삭제   */
        viewModel.deleteFeedResult.observe(this) { deleteFeedResult ->
            deleteFeedResult.onSuccess {
                myFeedAdapter.deleteFeedSuccess()
                Toast.makeText(this, "게시글 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myFeedAdapter.actionPosition = -1
        }

        /*   로딩 관련 Observer   */
        viewModel.isLoadingCenter.observe(this) {
            if(it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else
                binding.progressBarCenter.visibility = View.GONE
        }
        viewModel.isLoadingBottom.observe(this) {
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.GONE
        }

        /*   오류 관련 Observer   */
        viewModel.feedErrorLiveData.observe(this) {
            if(it == "FAIL") {
                Toast.makeText(this, "게시글 불러오기에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else if(it == "ERROR") {
                Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFeed() {
        viewModel.fetchUserFeedResults(Auth._userId.value.toString())
        Log.d("getFeed", "My Feed")
    }

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

    override fun onPostLike(feedID: Int) {
        if(!viewModel.checkLoading())
            viewModel.postFeedLike(feedID)
        else
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteLike(feedID: Int) {
        if(!viewModel.checkLoading())
            viewModel.deleteFeedLike(feedID)
        else
            Toast.makeText(this, "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteFeed(feedID: Int) {
        if(!viewModel.checkLoading())
            viewModel.deleteFeed(feedID)
        else
            Toast.makeText(this, "이전 작업을 처리중 입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
    }

    override fun onItemViewClick(position: Int) {
        var subList = viewModel.allFeedInfo.subList(position, viewModel.allFeedInfo.size)
        if(subList.size > 10) {
            subList = subList.subList(0, 10)
        }

        val feedInfoWithFollowList = subList.map {
            VectoService.FeedInfoWithFollow(feedInfo = it, isFollowing = false)  // 모든 isFollowing 값을 false로 설정
        }

        val intent = Intent(this, FeedDetailActivity::class.java).apply {
            putExtra("feedInfoListJson", Gson().toJson(feedInfoWithFollowList))
            putExtra("type", FeedDetailType.INTENT_USERINFO.code)
            putExtra("query", "")
            putExtra("nextPage", viewModel.nextPage)
            putExtra("followPage", viewModel.followPage)
            putExtra("lastPage", viewModel.lastPage)
        }

        if(!viewModel.checkLoading())
            startActivity(intent)
        else
            Toast.makeText(this, "이전 작업을 처리중 입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
    }
}