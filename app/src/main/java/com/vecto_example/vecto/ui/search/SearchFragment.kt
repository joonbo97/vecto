package com.vecto_example.vecto.ui.search

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
import com.vecto_example.vecto.NotificationActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.databinding.FragmentSearchBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.search.adapter.MysearchpostAdapter

class SearchFragment : Fragment(){
    /*   다른 사용자의 게시글을 확인 할 수 있는 Search Fragment   */

    private lateinit var binding: FragmentSearchBinding
    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(SearchRepository(VectoService.create()))
    }
    private lateinit var mysearchpostAdapter: MysearchpostAdapter

    private var query = ""
    private var queryFlag = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        initRecyclerView()
        initObservers()
        initListeners()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            if(!checkLoading()){//로딩중이 아니라면

                viewModel.initSetting()
                clearRecyclerView()
                clearNoneImage()

                getFeed()
            }

            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun initListeners() {
        /*   리스너 초기화 함수   */

        //알림 아이콘 클릭 이벤트
        binding.AlarmIconImage.setOnClickListener {
            val intent = Intent(context, NotificationActivity::class.java)
            startActivity(intent)
        }

        //검색 아이콘 클릭 이벤트
        binding.SearchIconImage.setOnClickListener {
            if(binding.editTextID.text.isEmpty())
            {
                Toast.makeText(requireContext(), "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            clearRecyclerView()
            clearNoneImage()
            viewModel.initSetting()

            /*   검색 상태 설정   */
            query = binding.editTextID.text.toString()
            mysearchpostAdapter.query = query
            queryFlag = true

            /*   게시글 요청   */
            getFeed()

            Toast.makeText(requireContext(), "${binding.editTextID.text}에 대한 결과입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initObservers() {
        /*   알림 아이콘 관련 Observer   */
        Auth.showFlag.observe(viewLifecycleOwner) {
            if(Auth.showFlag.value == true && Auth.loginFlag.value == true)//확인 안한 알림이 있을 경우
            {
                binding.AlarmIconImage.setImageResource(R.drawable.alarmon_icon)
            }
            else//확인 안한 알림이 없을 경우
            {
                binding.AlarmIconImage.setImageResource(R.drawable.alarmoff_icon)
            }
        }

        /*   로그인 관련 Observer   */
        Auth.loginFlag.observe(viewLifecycleOwner) {

            if(Auth.loginFlag.value != viewModel.originLoginFlag) {
                Log.d("LOGINFLAG", "LOGINFLAG IS CHANGED: ${Auth.loginFlag.value}")
                clearRecyclerView()
                clearNoneImage()
                viewModel.initSetting()
                queryFlag = false
                getFeed()   //로그인 상태 변경시 게시글 다시 불러옴
                viewModel.originLoginFlag = Auth.loginFlag.value!!
            }
        }

        /*   게시글 관련 Observer   */
        viewModel.feedInfoLiveData.observe(viewLifecycleOwner) {
            //새로운 feed 정보를 받았을 때의 처리
            mysearchpostAdapter.pageNo = viewModel.nextPage //다음 page 정보
            viewModel.feedInfoLiveData.value?.let { mysearchpostAdapter.addFeedInfoData(it) }   //새로 받은 게시글 정보 추가
        }

        viewModel.feedIdsLiveData.observe(viewLifecycleOwner) {
            viewModel.feedIdsLiveData.value?.let { mysearchpostAdapter.addFeedIdData(it.feedIds) }

            if(queryFlag && viewModel.allFeedIds.isEmpty() && viewModel.feedIdsLiveData.value?.feedIds.isNullOrEmpty()){
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

    }

    private fun initRecyclerView() {
        /*   Recycler 초기화 함수   */
        mysearchpostAdapter = MysearchpostAdapter(requireContext())

        clearRecyclerView()

        mysearchpostAdapter.addFeedInfoData(viewModel.allFeedInfo)
        mysearchpostAdapter.addFeedIdData(viewModel.allFeedIds)

        Log.d("ADAPTERSIZE", "${mysearchpostAdapter.feedInfo.size}")


        val searchRecyclerView = binding.SearchRecyclerView
        searchRecyclerView.adapter = mysearchpostAdapter
        searchRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        searchRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            //RecyclerView 하단에 도달 하면, 새로운 Page 글을 불러옴
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1)) {
                    if(viewModel.isLoadingBottom.value == false && viewModel.isLoadingCenter.value == false)
                    {
                        getFeed()
                    }

                }

            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearRecyclerView() {
        mysearchpostAdapter.feedID.clear()
        mysearchpostAdapter.feedInfo.clear()
        mysearchpostAdapter.notifyDataSetChanged()
        Log.d("CLEAER TEST", "${mysearchpostAdapter.feedInfo.size}")
    }

    private fun clearNoneImage() {
        binding.NoneImage.visibility = View.GONE
        binding.NoneText.visibility = View.GONE
        Log.d("NONE GONE", "NONE IMAGE IS GONE")
    }

    private fun setNoneImage() {
        binding.NoneImage.visibility = View.VISIBLE
        binding.NoneText.visibility = View.VISIBLE
    }

    private fun checkLoading(): Boolean{
        //로딩중이 아니라면 false, 로딩중이라면 true
        return !(viewModel.isLoadingBottom.value == false && viewModel.isLoadingCenter.value == false)
    }

    private fun getFeed() {
        //게시글 요청 함수
        if(queryFlag) { //검색 요청인 경우
            viewModel.fetchSearchFeedResults(query)
            Log.d("getFeed", "Query")
        }
        else{
            if(Auth.loginFlag.value == false) {   //로그인 X인 경우
                viewModel.fetchFeedResults()
                Log.d("getFeed", "Normal")
            }
            else{
                viewModel.fetchPersonalFeedResults()
                Log.d("getFeed", "Personal")
            }
        }
    }
}