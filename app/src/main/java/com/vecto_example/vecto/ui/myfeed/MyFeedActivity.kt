package com.vecto_example.vecto.ui.myfeed

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityMyFeedBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.myfeed.adapter.MyFeedAdapter
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModel
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModelFactory
import com.vecto_example.vecto.utils.FeedDetailType
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ShareFeedUtil
import com.vecto_example.vecto.utils.ToastMessageUtils
import com.vecto_example.vecto.utils.ToastMessageUtils.errorMessageHandler
import kotlinx.coroutines.launch

class MyFeedActivity : AppCompatActivity(), MyFeedAdapter.OnFeedActionListener {
    private lateinit var binding: ActivityMyFeedBinding

    private val viewModel: UserInfoViewModel by viewModels {
        UserInfoViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()), TokenRepository(VectoService.create()))
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
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
            }

            myFeedAdapter.postLikePosition = -1
        }

        viewModel.deleteFeedLikeResult.observe(this) { deleteFeedLikeResult ->
            deleteFeedLikeResult.onSuccess {
                myFeedAdapter.deleteFeedLikeSuccess()
            }.onFailure {
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
            }

            myFeedAdapter.deleteLikePosition = -1
        }

        /*   게시글 삭제   */
        viewModel.deleteFeedResult.observe(this) { deleteFeedResult ->
            deleteFeedResult.onSuccess {
                myFeedAdapter.deleteFeedSuccess()
                ToastMessageUtils.showToast(this, getString(R.string.delete_feed_success))
            }.onFailure {
                ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
            }

            myFeedAdapter.deleteFeedPosition = -1
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

        lifecycleScope.launch {
            viewModel.reissueResponse.collect {
                SaveLoginDataUtils.changeToken(this@MyFeedActivity, it.userToken.accessToken, it.userToken.refreshToken)

                when(it.function){
                    UserInfoViewModel.Function.FetchUserFeedResults.name -> {
                        getFeed()
                    }
                    UserInfoViewModel.Function.PostFeedLike.name -> {
                        viewModel.postFeedLike(viewModel.postFeedLikeId)
                    }
                    UserInfoViewModel.Function.DeleteFeedLike.name -> {
                        viewModel.deleteFeedLike(viewModel.deleteFeedLikeId)
                    }
                    UserInfoViewModel.Function.DeleteFeed.name -> {
                        viewModel.deleteFeed(viewModel.deleteFeedId)
                    }
                }
            }
        }



        /*   오류 관련 Observer   */
        viewModel.errorMessage.observe(this) {
            ToastMessageUtils.showToast(this, getString(it))

            if(it == R.string.expired_login) {
                SaveLoginDataUtils.deleteData(this)
                finish()
            }
        }
    }

    private fun getFeed() {
        viewModel.fetchUserFeedResults(Auth.userId.value.toString())
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
        if(!viewModel.postLikeLoading)
            viewModel.postFeedLike(feedID)
        else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            myFeedAdapter.postLikePosition = -1
        }
    }

    override fun onDeleteLike(feedID: Int) {
        if(!viewModel.deleteLikeLoading)
            viewModel.deleteFeedLike(feedID)
        else{
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            myFeedAdapter.deleteLikePosition = -1
        }
    }

    override fun onDeleteFeed(feedID: Int) {
        if(!viewModel.checkLoading())
            viewModel.deleteFeed(feedID)
        else{
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            myFeedAdapter.deleteFeedPosition = -1
        }
    }

    override fun onItemViewClick(position: Int) {
        if(position < 0  || position > viewModel.allFeedInfo.lastIndex)
            return

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
            putExtra("nextFeedId", viewModel.nextFeedId)
            putExtra("followPage", viewModel.followPage)
            putExtra("lastPage", viewModel.lastPage)
        }

        startActivity(intent)
    }

    override fun onShareClick(feedInfo: VectoService.FeedInfo) {
        ShareFeedUtil.shareFeed(this, feedInfo)
    }
}