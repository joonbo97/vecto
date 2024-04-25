package com.vecto_example.vecto.ui.mypage.likefeed

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
import com.vecto_example.vecto.databinding.FragmentMypageLikepostBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.search.adapter.FeedAdapter
import com.vecto_example.vecto.ui.search.adapter.MySearchFeedAdapter
import com.vecto_example.vecto.utils.FeedDetailType
import com.vecto_example.vecto.utils.LoadImageUtils

class MypageLikeFeedFragment : Fragment(), FeedAdapter.OnFeedActionListener {
    lateinit var binding: FragmentMypageLikepostBinding

    private val viewModel: MypageLikeFeedViewModel by viewModels {
        MypageLikeFeedViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
    }

    private lateinit var feedAdapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageLikepostBinding.inflate(inflater, container, false)

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

    @SuppressLint("NotifyDataSetChanged")
    private fun initObservers() {
        /*   게시글 관련 Observer   */
        viewModel.feedInfoLiveData.observe(viewLifecycleOwner) {
            if(viewModel.firstFlag) {
                feedAdapter.feedInfoWithFollow = viewModel.allFeedInfo
                feedAdapter.lastSize = viewModel.allFeedInfo.size

                feedAdapter.notifyDataSetChanged()
                viewModel.firstFlag = false
            } else {
                feedAdapter.addFeedData()
            }

            if(viewModel.allFeedInfo.isEmpty()){
                setNoneImage()
            } else {
                clearNoneImage()
            }
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

        /*   게시물 상호 작용 관련 Observer   */

        /*   게시글 좋아요   */
        viewModel.postFeedLikeResult.observe(viewLifecycleOwner) { postFeedLikeResult ->
            if(feedAdapter.actionPosition != -1)
            {
                postFeedLikeResult.onSuccess {
                    feedAdapter.postFeedLikeSuccess()
                }.onFailure {
                    Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFeedLikeResult.observe(viewLifecycleOwner) { deleteFeedLikeResult ->
            if(feedAdapter.actionPosition != -1) {
                deleteFeedLikeResult.onSuccess {
                    feedAdapter.deleteFeedLikeSuccess()
                }.onFailure {
                    Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        //팔로우
        viewModel.postFollowResult.observe(viewLifecycleOwner) {
            if(feedAdapter.actionPosition != -1) {
                if (it) {
                    feedAdapter.postFollowSuccess()
                    Toast.makeText(requireContext(), "${viewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName} 님을 팔로우하기 시작했습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "이미 ${viewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName} 님을 팔로우 중입니다.", Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFollowResult.observe(viewLifecycleOwner) {
            if(feedAdapter.actionPosition != -1) {
                if (it) {
                    feedAdapter.deleteFollowSuccess()
                    Toast.makeText(requireContext(), "${viewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName} 님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "이미 ${viewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName} 님을 팔로우하지 않습니다.", Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        /*   오류 관련 Observer   */
        viewModel.feedErrorLiveData.observe(viewLifecycleOwner) {
            if(it == "FAIL") {
                Toast.makeText(requireContext(), "게시글 불러오기에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else if(it == "ERROR") {
                Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.followErrorLiveData.observe(viewLifecycleOwner) {
            if(it == "FAIL") {
                Toast.makeText(requireContext(), "팔로우 정보 불러오기에 실패하였습니다.", Toast.LENGTH_SHORT).show()
            } else if(it == "ERROR") {
                Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }

        }

        viewModel.postFollowError.observe(viewLifecycleOwner) {
            if(feedAdapter.actionPosition != -1) {
                if (it == "FAIL") {
                    Toast.makeText(requireContext(), "팔로우 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFollowError.observe(viewLifecycleOwner) {
            if(feedAdapter.actionPosition != -1) {
                if (it == "FAIL") {
                    Toast.makeText(requireContext(), "팔로우 취소 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                feedAdapter.actionPosition = -1
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initRecyclerView() {
        feedAdapter = FeedAdapter()
        feedAdapter.feedActionListener = this

        val likePostRecyclerView = binding.LikePostRecyclerView
        likePostRecyclerView.adapter = feedAdapter

        likePostRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        likePostRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1)) {
                    if(!viewModel.checkLoading()){
                        getFeed()
                    }
                }
            }
        })

        if(viewModel.allFeedInfo.isNotEmpty()){
            feedAdapter.feedInfoWithFollow = viewModel.allFeedInfo
            feedAdapter.notifyDataSetChanged()
        }
    }

    private fun getFeed() {
        viewModel.getLikeFeedList()
        Log.d("getFeed", "Like Feed")
    }

    /*   UI 설정   */
    private fun initUI() {
        LoadImageUtils.loadProfileImage(requireContext(), binding.ProfileImage)

        binding.UserNameText.text = Auth._nickName.value
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
        if(!viewModel.checkLoading()) {
            viewModel.postFeedLike(feedId)
        } else {
            Toast.makeText(requireContext(), "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFeedLike(feedId: Int) {
        if(!viewModel.checkLoading()) {
            viewModel.deleteFeedLike(feedId)
        } else {
            Toast.makeText(requireContext(), "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
        }
    }

    override fun onPostFollow(userId: String) {
        if(!viewModel.checkLoading()) {
            viewModel.postFollow(userId)
        } else {
            Toast.makeText(requireContext(), "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFollow(userId: String) {
        if(!viewModel.checkLoading()) {
            viewModel.deleteFollow(userId)
        } else {
            Toast.makeText(requireContext(), "이전 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            feedAdapter.actionPosition = -1
        }
    }

    override fun onItemClick(position: Int) {
        var subList = viewModel.allFeedInfo.subList(position, viewModel.allFeedInfo.size)
        if(subList.size > 10) {
            subList = subList.subList(0, 10)
        }

        val intent = Intent(requireContext(), FeedDetailActivity::class.java).apply {
            putExtra("feedInfoListJson", Gson().toJson(subList))
            putExtra("type", FeedDetailType.INTENT_LIKE.code)
            putExtra("query", "")
            putExtra("nextPage", viewModel.nextPage)
            putExtra("followPage", viewModel.followPage)
            putExtra("lastPage", viewModel.lastPage)
        }

        if(!viewModel.checkLoading())
            this.startActivity(intent)
        else
            Toast.makeText(requireContext(), "이전 작업을 처리중 입니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
    }
}