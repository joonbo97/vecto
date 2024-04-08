package com.vecto_example.vecto.ui.search

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.ui.notification.NotificationActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.NotificationRepository
import com.vecto_example.vecto.databinding.FragmentSearchBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.notification.NotificationViewModel
import com.vecto_example.vecto.ui.notification.NotificationViewModelFactory
import com.vecto_example.vecto.ui.search.adapter.MysearchpostAdapter
import com.vecto_example.vecto.utils.RequestLoginUtils

class SearchFragment : Fragment(){
    /*   다른 사용자의 게시글을 확인 할 수 있는 Search Fragment   */

    private lateinit var binding: FragmentSearchBinding
    private val searchViewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(FeedRepository(VectoService.create()))
    }
    private val notificationViewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory(NotificationRepository(VectoService.create()))
    }
    private lateinit var mysearchpostAdapter: MysearchpostAdapter
    private lateinit var notificationReceiver: BroadcastReceiver

    private var query = ""
    private var queryFlag = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        initRecyclerView()
        initObservers()
        initListeners()
        initReceiver()

        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            if(!searchViewModel.checkLoading()){//로딩중이 아니라면

                searchViewModel.initSetting()
                clearRecyclerView()
                clearNoneImage()

                getFeed()
            }

            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun initReceiver() {
        /*   Receiver 초기화 함수   */

        notificationReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                notificationViewModel.getNewNotificationFlag()
            }
        }

        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                notificationReceiver,
                IntentFilter("NEW_NOTIFICATION")
            )
        }
    }

    private fun initUI() {
        /*   UI 초기화 함수   */
        if(Auth.loginFlag.value == true){
            notificationViewModel.getNewNotificationFlag()
        }
    }

    private fun initListeners() {
        /*   리스너 초기화 함수   */

        //알림 아이콘 클릭 이벤트
        binding.AlarmIconImage.setOnClickListener {
            if(Auth.loginFlag.value == false)
            {
                RequestLoginUtils.requestLogin(requireContext())
                return@setOnClickListener
            }

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
            searchViewModel.initSetting()

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
        notificationViewModel.newNotificationFlag.observe(viewLifecycleOwner) {
            it.onSuccess { newNotificationFlag->
                if(newNotificationFlag){    //새로운 알림이 있을 경우
                    Log.d("SEARCH_INIT_UI", "SUCCESS_TRUE")
                    binding.AlarmIconImage.setImageResource(R.drawable.alarmon_icon)
                }
                else{   //새로운 알림이 없는 경우
                    Log.d("SEARCH_INIT_UI", "SUCCESS_FALSE")
                    binding.AlarmIconImage.setImageResource(R.drawable.alarmoff_icon)
                }
            }
                .onFailure {//실패한 경우
                    Log.d("SEARCH_INIT_UI", "FAIL")
                    binding.AlarmIconImage.setImageResource(R.drawable.alarmoff_icon)
                }
        }

        /*   로그인 관련 Observer   */
        Auth.loginFlag.observe(viewLifecycleOwner) {
            initUI()

            if(Auth.loginFlag.value != searchViewModel.originLoginFlag) {
                Log.d("LOGINFLAG", "LOGINFLAG IS CHANGED: ${Auth.loginFlag.value}")
                clearRecyclerView()
                clearNoneImage()
                searchViewModel.initSetting()
                queryFlag = false
                getFeed()   //로그인 상태 변경시 게시글 다시 불러옴
                searchViewModel.originLoginFlag = Auth.loginFlag.value!!
            }
        }

        /*   게시글 관련 Observer   */
        searchViewModel.feedInfoLiveData.observe(viewLifecycleOwner) {
            //새로운 feed 정보를 받았을 때의 처리
            mysearchpostAdapter.pageNo = searchViewModel.nextPage //다음 page 정보
            searchViewModel.feedInfoLiveData.value?.let { mysearchpostAdapter.addFeedInfoData(it) }   //새로 받은 게시글 정보 추가

            Log.d("Pagination", "Current allFeedIds size: ${searchViewModel.allFeedIds.size}")
            Log.d("Pagination", "Current FeedIds size: ${searchViewModel.feedIdsLiveData.value?.feedIds?.size}")


            /*if(searchViewModel.feedInfoLiveData.value != null && !searchViewModel.isDataLoaded && searchViewModel.feedIdsLiveData.value != null){
                searchViewModel.allFeedIds.addAll(searchViewModel.feedIdsLiveData.value!!.feedIds)
                searchViewModel.allFeedInfo.addAll(searchViewModel.feedInfoLiveData.value!!)
                searchViewModel.isDataLoaded = true
            }*/

            Log.d("Pagination", "New data added. Updated allFeedIds size: ${searchViewModel.allFeedIds.size}")

        }

        searchViewModel.feedIdsLiveData.observe(viewLifecycleOwner) {
            searchViewModel.feedIdsLiveData.value?.let { mysearchpostAdapter.addFeedIdData(it.feedIds) }

            if(queryFlag && searchViewModel.allFeedIds.isEmpty() && searchViewModel.feedIdsLiveData.value?.feedIds.isNullOrEmpty()){
                setNoneImage()
            }

        }

        /*   로딩 관련 Observer   */
        searchViewModel.isLoadingCenter.observe(viewLifecycleOwner) {
            if(it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else
                binding.progressBarCenter.visibility = View.GONE
        }
        searchViewModel.isLoadingBottom.observe(viewLifecycleOwner) {
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.GONE
        }

    }

    private fun initRecyclerView() {
        /*   Recycler 초기화 함수   */
        mysearchpostAdapter = MysearchpostAdapter(requireContext())

        val searchRecyclerView = binding.SearchRecyclerView
        searchRecyclerView.adapter = mysearchpostAdapter
        searchRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        searchRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            //RecyclerView 하단에 도달 하면, 새로운 Page 글을 불러옴
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1)) {
                    if(!searchViewModel.checkLoading())
                    {
                        getFeed()
                    }

                }

            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearRecyclerView() {
        Log.d("CLEAR_RECYCLERVIEW", "CLEAR")

        mysearchpostAdapter.feedID.clear()
        mysearchpostAdapter.feedInfo.clear()
        mysearchpostAdapter.notifyDataSetChanged()
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

    private fun getFeed() {
        //게시글 요청 함수
        if(queryFlag) { //검색 요청인 경우
            searchViewModel.fetchSearchFeedResults(query)
            Log.d("getFeed", "Query")
        }
        else{
            if(Auth.loginFlag.value == false) {   //로그인 X인 경우
                searchViewModel.fetchFeedResults()
                Log.d("getFeed", "Normal")
            }
            else{
                searchViewModel.fetchPersonalFeedResults()
                Log.d("getFeed", "Personal")
            }
        }
    }

    override fun onResume() {
        super.onResume()

        initUI()

        /*clearRecyclerView()
        mysearchpostAdapter.addFeedInfoData(searchViewModel.allFeedInfo)
        mysearchpostAdapter.addFeedIdData(searchViewModel.allFeedIds)*/

        Log.d("RESUME", "adapter size ID: ${mysearchpostAdapter.feedID.size}, INFO: ${mysearchpostAdapter.feedInfo.size}")
    }

    override fun onDestroyView() {
        super.onDestroyView()

        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(notificationReceiver)
        }
    }
}