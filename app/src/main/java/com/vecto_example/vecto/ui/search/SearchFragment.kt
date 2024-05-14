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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.vecto_example.vecto.ui.notification.NotificationActivity
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.NotificationRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.FragmentSearchBinding
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.detail.FeedDetailActivity
import com.vecto_example.vecto.ui.login.LoginViewModel
import com.vecto_example.vecto.ui.login.LoginViewModelFactory
import com.vecto_example.vecto.ui.main.MainActivity
import com.vecto_example.vecto.ui.notification.NotificationViewModel
import com.vecto_example.vecto.ui.notification.NotificationViewModelFactory
import com.vecto_example.vecto.ui.search.adapter.FeedAdapter
import com.vecto_example.vecto.utils.FeedDetailType
import com.vecto_example.vecto.utils.RequestLoginUtils
import com.vecto_example.vecto.utils.ShareFeedUtil
import com.vecto_example.vecto.utils.ToastMessageUtils
import com.vecto_example.vecto.utils.ToastMessageUtils.errorMessageHandler

class SearchFragment : Fragment(), MainActivity.ScrollToTop, FeedAdapter.OnFeedActionListener {
    private lateinit var binding: FragmentSearchBinding

    private val searchViewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
    }
    private val notificationViewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory(NotificationRepository(VectoService.create()))
    }

    private lateinit var loginViewModel: LoginViewModel

    private lateinit var feedAdapter: FeedAdapter

    private lateinit var notificationReceiver: BroadcastReceiver

    private lateinit var callBack: OnBackPressedCallback

    private var query = ""

    private var backPressedTime: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginViewModel = ViewModelProvider(requireActivity(), LoginViewModelFactory(UserRepository(VectoService.create())))[LoginViewModel::class.java]

        initUI()
        initRecyclerView()
        initObservers()
        initListeners()
        initReceiver()
    }

    override fun onResume() {
        super.onResume()

        initUI()

        if(feedAdapter.feedInfoWithFollow.size != searchViewModel.allFeedInfo.size){
            feedAdapter.feedInfoWithFollow = searchViewModel.allFeedInfo
            feedAdapter.addFeedData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(notificationReceiver)
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

    private fun initListeners() {
        /*   리스너 초기화 함수   */

        //로고 클릭 이벤트
        binding.VectoTitleImage.setOnClickListener {
            clearNoneImage()

            searchViewModel.initSetting()

            searchViewModel.queryFlag = false

            getFeed()
        }

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
            startSearchRequest()
        }

        binding.editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                startSearchRequest()

                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.editTextSearch.windowToken, 0)

                true
            } else {
                false
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
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

            loginFlagChange()
        }

        loginViewModel.isLoginFinished.observe(viewLifecycleOwner){
            loginFlagChange()
        }

        /*   게시글 관련 Observer   */
        searchViewModel.feedInfoLiveData.observe(viewLifecycleOwner) {
            if(searchViewModel.firstFlag) {
                feedAdapter.feedInfoWithFollow = searchViewModel.allFeedInfo
                feedAdapter.lastSize = searchViewModel.allFeedInfo.size

                feedAdapter.notifyDataSetChanged()
                searchViewModel.firstFlag = false
            }
            else {
                feedAdapter.addFeedData()
            }

            if(searchViewModel.queryFlag && searchViewModel.allFeedInfo.isEmpty() && !searchViewModel.checkLoading()){
                setNoneImage()
            } else {
                clearNoneImage()
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

        /*   게시물 상호 작용 관련 Observer   */

        /*   게시글 좋아요   */
        searchViewModel.postFeedLikeResult.observe(viewLifecycleOwner) { postFeedLikeResult ->
            if(feedAdapter.actionPosition != -1)
            {
                postFeedLikeResult.onSuccess {
                    feedAdapter.postFeedLikeSuccess()
                }.onFailure {
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.APIErrorToastMessage))
                }

                feedAdapter.actionPosition = -1
            }
        }

        searchViewModel.deleteFeedLikeResult.observe(viewLifecycleOwner) { deleteFeedLikeResult ->
            if(feedAdapter.actionPosition != -1) {
                deleteFeedLikeResult.onSuccess {
                    feedAdapter.deleteFeedLikeSuccess()
                }.onFailure {
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.APIErrorToastMessage))
                }

                feedAdapter.actionPosition = -1
            }
        }

        //팔로우
        searchViewModel.postFollowResult.observe(viewLifecycleOwner) {
            if(feedAdapter.actionPosition != -1) {
                if (it) {
                    feedAdapter.postFollowSuccess()
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.post_follow_success, searchViewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName))
                } else {
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.post_follow_already, searchViewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName))
                }

                feedAdapter.actionPosition = -1
            }
        }

        searchViewModel.deleteFollowResult.observe(viewLifecycleOwner) {
            if(feedAdapter.actionPosition != -1) {
                if (it) {
                    feedAdapter.deleteFollowSuccess()
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.delete_follow_success, searchViewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName))
                } else {
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.delete_follow_already, searchViewModel.allFeedInfo[feedAdapter.actionPosition].feedInfo.nickName))
                }

                feedAdapter.actionPosition = -1
            }
        }

        /*   오류 관련 Observer   */
        searchViewModel.feedErrorLiveData.observe(viewLifecycleOwner) {
            errorMessageHandler(requireContext(), ToastMessageUtils.ValueType.FEED.name, it)
        }

        searchViewModel.followErrorLiveData.observe(viewLifecycleOwner) {
            errorMessageHandler(requireContext(), ToastMessageUtils.ValueType.FOLLOW.name, it)
        }

        searchViewModel.postFollowError.observe(viewLifecycleOwner) {
            if(feedAdapter.actionPosition != -1) {
                errorMessageHandler(requireContext(), ToastMessageUtils.ValueType.FOLLOW_POST.name, it)
                feedAdapter.actionPosition = -1
            }
        }

        searchViewModel.deleteFollowError.observe(viewLifecycleOwner) {
            if(feedAdapter.actionPosition != -1) {
                errorMessageHandler(requireContext(), ToastMessageUtils.ValueType.FOLLOW_DELETE.name, it)
                feedAdapter.actionPosition = -1
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initRecyclerView() {
        /*   Recycler 초기화 함수   */
        feedAdapter = FeedAdapter()
        feedAdapter.feedActionListener = this

        val searchRecyclerView = binding.SearchRecyclerView
        searchRecyclerView.adapter = feedAdapter

        searchRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        searchRecyclerView.itemAnimator = null
        searchRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            //RecyclerView 하단에 도달 하면, 새로운 Page 글을 불러옴
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1)) {
                    Log.d("getFeed_REQUEST", "By Scroll")
                    getFeed()
                }

            }
        })

        if(searchViewModel.allFeedInfo.isNotEmpty()){
            feedAdapter.feedInfoWithFollow = searchViewModel.allFeedInfo
            feedAdapter.notifyDataSetChanged()
        }

        val swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            if(!searchViewModel.checkLoading()){//로딩중이 아니라면

                searchViewModel.initSetting()

                Log.d("getFeed_REQUEST", "By Refresh")
                getFeed()
            }

            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun getFeed() {
        if(!searchViewModel.checkLoading()){//로딩중이 아니라면

            //게시글 요청 함수
            if(searchViewModel.queryFlag) { //검색 요청인 경우
                searchViewModel.getFeedList(query)
                Log.d("getFeed", "Query")
            }
            else{
                if(Auth.loginFlag.value == false) {   //로그인 X인 경우
                    searchViewModel.getFeedList("Normal")
                    Log.d("getFeed", "Normal")
                }
                else{
                    searchViewModel.getFeedList("Personal")
                    Log.d("getFeed", "Personal")
                }
            }
        }
    }

    private fun loginFlagChange() {
        if(Auth.loginFlag.value != searchViewModel.originLoginFlag && loginViewModel.isLoginFinished.value == true) {
            if(searchViewModel.originLoginFlag == null)
                searchViewModel.originLoginFlag = false

            clearNoneImage()

            searchViewModel.initSetting()

            searchViewModel.queryFlag = false

            Log.d("getFeed_REQUEST", "By loginFlag Change")
            getFeed()   //로그인 상태 변경시 게시글 다시 불러옴

            searchViewModel.originLoginFlag = Auth.loginFlag.value
        }
    }

    override fun scrollToTop() {
        binding.SearchRecyclerView.smoothScrollToPosition(0)
    }

    //검색 작업 수행
    private fun startSearchRequest() {
        if(binding.editTextSearch.text.isEmpty())
        {
            ToastMessageUtils.showToast(requireContext(), getString(R.string.get_search_empty_query))
        } else {
            clearNoneImage()
            searchViewModel.initSetting()

            /*   검색 상태 설정   */
            query = binding.editTextSearch.text.toString()
            searchViewModel.queryFlag = true

            /*   게시글 요청   */
            Log.d("getFeed_REQUEST", "By Search")
            getFeed()
            ToastMessageUtils.showToast(requireContext(), getString(R.string.get_search_success, query))
        }
    }

    /*   UI 설정   */

    private fun initUI() {
        if(Auth.loginFlag.value == true){
            notificationViewModel.getNewNotificationFlag()
        }
    }

    // 검색 결과 None 이미지
    private fun clearNoneImage() {
        binding.NoneImage.visibility = View.GONE
        binding.NoneText.visibility = View.GONE
        Log.d("NONE GONE", "NONE IMAGE IS GONE")
    }

    private fun setNoneImage() {
        binding.NoneImage.visibility = View.VISIBLE
        binding.NoneText.visibility = View.VISIBLE
    }



    /*   Adapter CallBack 관련   */
    override fun onPostFeedLike(feedId: Int) {
        if(!searchViewModel.checkLoading()) {
            searchViewModel.postFeedLike(feedId)
        } else {
            ToastMessageUtils.showToast(requireContext(), getString(R.string.task_duplication))
            feedAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFeedLike(feedId: Int) {
        if(!searchViewModel.checkLoading()) {
            searchViewModel.deleteFeedLike(feedId)
        } else {
            ToastMessageUtils.showToast(requireContext(), getString(R.string.task_duplication))
            feedAdapter.actionPosition = -1
        }
    }

    override fun onPostFollow(userId: String) {
        if(!searchViewModel.checkLoading()) {
            searchViewModel.postFollow(userId)
        } else {
            ToastMessageUtils.showToast(requireContext(), getString(R.string.task_duplication))
            feedAdapter.actionPosition = -1
        }
    }

    override fun onDeleteFollow(userId: String) {
        if(!searchViewModel.checkLoading()) {
            searchViewModel.deleteFollow(userId)
        } else {
            ToastMessageUtils.showToast(requireContext(), getString(R.string.task_duplication))
            feedAdapter.actionPosition = -1
        }
    }

    override fun onItemClick(position: Int) {
        var subList = searchViewModel.allFeedInfo.subList(position, searchViewModel.allFeedInfo.size)
        if(subList.size > 10) {
            subList = subList.subList(0, 10)
        }

        if(!searchViewModel.checkLoading()) {
            val intent = Intent(requireContext(), FeedDetailActivity::class.java).apply {
                putExtra("feedInfoListJson", Gson().toJson(subList))
                if(searchViewModel.queryFlag) {
                    putExtra("type", FeedDetailType.INTENT_QUERY.code)
                    putExtra("query", query)
                }
                else {
                    putExtra("type", FeedDetailType.INTENT_NORMAL.code)
                    putExtra("query", "")
                }
                putExtra("nextPage", searchViewModel.nextPage)
                putExtra("followPage", searchViewModel.followPage)
                putExtra("lastPage", searchViewModel.lastPage)
            }

            this.startActivity(intent)
        }
        else
            ToastMessageUtils.showToast(requireContext(), getString(R.string.task_duplication))
    }

    override fun onShareClick(feedInfo: VectoService.FeedInfo) {
        ShareFeedUtil.shareFeed(requireContext(), feedInfo)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        callBack = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backPressedTime <= 2000) {
                    requireActivity().finish()
                } else {
                    backPressedTime = System.currentTimeMillis()
                    ToastMessageUtils.showToast(requireContext(), getString(R.string.back_pressed))
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this, callBack)
    }

    override fun onDetach() {
        super.onDetach()

        callBack.remove()
    }



}