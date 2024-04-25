package com.vecto_example.vecto.ui.mypage.myfeed

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.FragmentMypagePostBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.mypage.myfeed.adapter.MyFeedAdapter
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModel
import com.vecto_example.vecto.ui.userinfo.UserInfoViewModelFactory
import com.vecto_example.vecto.utils.FeedDetailType
import com.vecto_example.vecto.utils.LoadImageUtils

class MypageFeedFragment : Fragment(), MyFeedAdapter.OnFeedActionListener {
    private lateinit var binding: FragmentMypagePostBinding

    private val viewModel: UserInfoViewModel by viewModels {
        UserInfoViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
    }

    private lateinit var myFeedAdapter: MyFeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypagePostBinding.inflate(inflater, container, false)

        LoadImageUtils.loadProfileImage(requireContext(), binding.ProfileImage)

        binding.UserNameText.text = Auth._nickName.value

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        initRecyclerView()
        initObservers()
        getFeed()

        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {

            if(!viewModel.checkLoading()){

                viewModel.initSetting()
                clearNoneImage()

                getFeed()
            }

            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun initUI() {
        LoadImageUtils.loadProfileImage(requireContext(), binding.ProfileImage)

        binding.UserNameText.text = Auth._nickName.value
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initRecyclerView() {
        myFeedAdapter = MyFeedAdapter()
        myFeedAdapter.feedActionListener = this

        val myFeedRecyclerView = binding.MyFeedRecyclerView
        myFeedRecyclerView.adapter = myFeedAdapter

        myFeedRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
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
        viewModel.feedInfoLiveData.observe(viewLifecycleOwner) {
            //새로운 feed 정보를 받았을 때의 처리
            if(viewModel.firstFlag){
                myFeedAdapter.feedInfo = viewModel.allFeedInfo
                myFeedAdapter.lastSize = viewModel.allFeedInfo.size

                myFeedAdapter.notifyDataSetChanged()
                viewModel.firstFlag = false
            } else {
                myFeedAdapter.addFeedInfoData()
            }

            if(viewModel.allFeedInfo.isEmpty()){
                setNoneImage()
            } else {
                clearNoneImage()
            }
        }

        /*   게시글 좋아요   */
        viewModel.postFeedLikeResult.observe(viewLifecycleOwner) { postFeedLikeResult ->
            postFeedLikeResult.onSuccess {
                myFeedAdapter.postFeedLikeSuccess()
            }.onFailure {
                Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myFeedAdapter.actionPosition = -1
        }

        viewModel.deleteFeedLikeResult.observe(viewLifecycleOwner) { deleteFeedLikeResult ->
            deleteFeedLikeResult.onSuccess {
                myFeedAdapter.deleteFeedLikeSuccess()
            }.onFailure {
                Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myFeedAdapter.actionPosition = -1
        }

        /*   게시글 삭제   */
        viewModel.deleteFeedResult.observe(viewLifecycleOwner) { deleteFeedResult ->
            deleteFeedResult.onSuccess {
                myFeedAdapter.deleteFeedSuccess()
                Toast.makeText(requireContext(), "게시글 삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

            myFeedAdapter.actionPosition = -1
        }

        /*   로딩 관련 Observer   */
        viewModel.isLoadingCenter.observe(viewLifecycleOwner) {
            if(it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else
                binding.progressBarCenter.visibility = View.GONE
        }
        viewModel.isLoadingBottom.observe(viewLifecycleOwner) {
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.GONE
        }

        /*   오류 관련 Observer   */
        viewModel.feedErrorLiveData.observe(viewLifecycleOwner) {
            if(it == "FAIL") {
                Toast.makeText(requireContext(), "게시글 불러오기에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else if(it == "ERROR") {
                Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteLike(feedID: Int) {
        if(!viewModel.checkLoading())
            viewModel.deleteFeedLike(feedID)
        else
            Toast.makeText(requireContext(), "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteFeed(feedID: Int) {
        if(!viewModel.checkLoading())
            viewModel.deleteFeed(feedID)
        else
            Toast.makeText(requireContext(), "이전 작업을 처리중 입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
    }

    override fun onItemViewClick(position: Int) {
        var subList = viewModel.allFeedInfo.subList(position, viewModel.allFeedInfo.size)
        if(subList.size > 10) {
            subList = subList.subList(0, 10)
        }

        val feedInfoWithFollowList = subList.map {
            VectoService.FeedInfoWithFollow(feedInfo = it, isFollowing = false)  // 모든 isFollowing 값을 false로 설정
        }

        val intent = Intent(requireContext(), FeedDetailActivity::class.java).apply {
            putExtra("feedInfoListJson", Gson().toJson(feedInfoWithFollowList))
            putExtra("type", FeedDetailType.INTENT_USERINFO.code)
            putExtra("query", "")
            putExtra("nextPage", viewModel.nextPage)
            putExtra("followPage", viewModel.followPage)
            putExtra("lastPage", viewModel.lastPage)
        }

        if(!viewModel.checkLoading())
            requireContext().startActivity(intent)
        else
            Toast.makeText(requireContext(), "이전 작업을 처리중 입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
    }

}