package com.vecto_example.vecto.ui.mypage.myfeed

import android.annotation.SuppressLint
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
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.databinding.FragmentMypagePostBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.mypage.myfeed.adapter.MyFeedAdapter
import com.vecto_example.vecto.utils.LoadImageUtils

class MypageFeedFragment : Fragment(), MyFeedAdapter.OnFeedActionListener {
    private lateinit var binding: FragmentMypagePostBinding
    private val viewModel: MypageFeedViewModel by viewModels {
        MypageFeedViewModelFactory(FeedRepository(VectoService.create()))
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
                clearRecyclerView()
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

    private fun initRecyclerView() {
        myFeedAdapter = MyFeedAdapter(requireContext())
        myFeedAdapter.feedActionListener = this

        clearRecyclerView()

        myFeedAdapter.addFeedInfoData(viewModel.allFeedInfo)
        myFeedAdapter.addFeedIdData(viewModel.allFeedIds)

        val mypostRecyclerView = binding.MypostRecyclerView
        mypostRecyclerView.adapter = myFeedAdapter
        mypostRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        mypostRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
    }

    private fun initObservers() {
        /*   게시글 관련 Observer   */
        viewModel.feedInfoLiveData.observe(viewLifecycleOwner) {
            //새로운 feed 정보를 받았을 때의 처리
            myFeedAdapter.pageNo = viewModel.nextPage //다음 page 정보
            viewModel.feedInfoLiveData.value?.let { myFeedAdapter.addFeedInfoData(it) }   //새로 받은 게시글 정보 추가
        }

        viewModel.feedIdsLiveData.observe(viewLifecycleOwner) {
            viewModel.feedIdsLiveData.value?.let { myFeedAdapter.addFeedIdData(it.feedIds) }

            if(viewModel.allFeedIds.isEmpty() && viewModel.feedIdsLiveData.value?.feedIds.isNullOrEmpty()){
                setNoneImage()
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

        /*   오류 관련 Observer   */
        viewModel.feedErrorLiveData.observe(viewLifecycleOwner) {
            if(it == "FAIL") {
                Toast.makeText(requireContext(), "게시글 불러오기에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            } else if(it == "ERROR") {
                Toast.makeText(requireContext(), getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearRecyclerView() {
        myFeedAdapter.feedID.clear()
        myFeedAdapter.feedInfo.clear()
        myFeedAdapter.notifyDataSetChanged()

    }

    private fun getFeed() {
        viewModel.fetchUserFeedResults()
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
        TODO("Not yet implemented")
    }

    override fun onDeleteLike(feedID: Int) {
        TODO("Not yet implemented")
    }

}