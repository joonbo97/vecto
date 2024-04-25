package com.vecto_example.vecto.ui.detail

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.retrofit.VectoService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.vecto_example.vecto.ui.detail.adapter.MyFeedDetailAdapter
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.databinding.ActivityPostDetailBinding
import com.vecto_example.vecto.utils.MapMarkerManager
import com.vecto_example.vecto.utils.MapOverlayManager

class FeedDetailActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var mapMarkerManager: MapMarkerManager
    private lateinit var mapOverlayManager: MapOverlayManager

    private val viewModel: FeedDetailViewModel by viewModels {
        FeedDetailViewModelFactory(FeedRepository(VectoService.create()))
    }

    private lateinit var myFeedDetailAdapter: MyFeedDetailAdapter

    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    private var likePostFlag = false

    var userId = ""
    var query = ""
    var pageNo = 0

    var lastY = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initMap()
        initObservers()
        initSlideLayout()

        binding.BackButton.setOnClickListener {
            finish()
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSlideLayout() {
        val topMargin = dpToPx(150f, this) // 상단에서 최소 150dp
        val bottomMargin = dpToPx(100f, this) // 하단에서 최소 100dp
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        binding.slide.setOnTouchListener { view, event ->
            val layoutParams = binding.naverMapDetail.layoutParams as ConstraintLayout.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newY = event.rawY
                    val deltaY = newY - lastY
                    var newHeight = layoutParams.height + deltaY.toInt()

                    // 상단 마진과 하단 마진을 고려하여 새로운 높이를 조정합니다.
                    newHeight = newHeight.coerceAtLeast(topMargin)
                    newHeight = newHeight.coerceAtMost(screenHeight - bottomMargin)

                    layoutParams.height = newHeight
                    binding.naverMapDetail.layoutParams = layoutParams
                    binding.naverMapDetail.requestLayout()

                    lastY = newY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun dpToPx(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }


    private fun initMap(){
        mapView = supportFragmentManager.findFragmentById(R.id.naver_map_detail) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.naver_map_detail, it).commit()
            }
        mapView.getMapAsync(this)
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        naverMap.uiSettings.isZoomControlEnabled = false

        mapMarkerManager = MapMarkerManager(this, naverMap)
        mapOverlayManager = MapOverlayManager(this, mapMarkerManager, naverMap)

        myFeedDetailAdapter = MyFeedDetailAdapter(this)
        val postDetailRecyclerView = binding.PostDetailRecyclerView
        postDetailRecyclerView.adapter = myFeedDetailAdapter
        postDetailRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        postDetailRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager


                // 현재 보이는 아이템들의 위치 확인
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // 중앙 아이템 인덱스 계산
                val centerPosition = (firstVisibleItemPosition + lastVisibleItemPosition) / 2

                if(layoutManager.findLastVisibleItemPosition() == myFeedDetailAdapter.feedInfo.lastIndex){
                    val feedInfo = myFeedDetailAdapter.feedInfo[lastVisibleItemPosition]
                    mapOverlayManager.addOverlayForPost(feedInfo) // 중앙 아이템 강조
                }
                else {
                    // 중앙 아이템의 정보를 가져와서 처리
                    if (centerPosition >= 0 && centerPosition < myFeedDetailAdapter.feedInfo.size) {
                        val feedInfo = myFeedDetailAdapter.feedInfo[centerPosition]
                        mapOverlayManager.addOverlayForPost(feedInfo) // 중앙 아이템 강조
                    }
                }

                if (!recyclerView.canScrollVertically(1)) {
                    getFeed()
                }
            }
        })


        // Intent에서 JSON 문자열을 가져와 리스트로 변환
        //val feedInfoWithFollow = intent.getStringExtra("feedInfoListJson")
        val position = intent.getIntExtra("position", -1)
        pageNo = intent.getIntExtra("pageNo", -1)
        viewModel.nextPage = pageNo
        val intentQuery = intent.getStringExtra("query")
        likePostFlag = intent.getBooleanExtra("likePostFlag", false)

        // 인텐트로부터 값 가져오기
        val feedInfoWithFollowIntent = intent.getStringExtra("feedInfoListJson")
        val type = intent.getStringExtra("type")
        val nextPage = intent.getIntExtra("nextPage", 0)
        val followPage = intent.getBooleanExtra("followPage", false)
        val lastPage = intent.getBooleanExtra("lastPage", false)

        if(!intentQuery.isNullOrEmpty()){
            query = intentQuery
        }
        /*if(!intentUserId.isNullOrEmpty()){
            userId = intentUserId
        }*/


        // JSON 문자열을 객체 리스트로 변환
        val typeOfFeedInfoList = object : TypeToken<List<VectoService.FeedInfoWithFollow>>() {}.type
        val feedInfoWithFollow = Gson().fromJson<List<VectoService.FeedInfoWithFollow>>(feedInfoWithFollowIntent, typeOfFeedInfoList)

        // Adapter 데이터 설정
        //myFeedDetailAdapter.addFeedInfoData(feedInfoList)
        myFeedDetailAdapter.notifyDataSetChanged()

        if(position != -1)
            (binding.PostDetailRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
    }

    private fun initObservers() {
        /*   로그인 관련 Observer   */
        Auth.loginFlag.observe(this) {
            if(Auth.loginFlag.value != viewModel.originLoginFlag) {
                Log.d("LOGINFLAG", "LOGINFLAG IS CHANGED: ${Auth.loginFlag.value}")
                clearRecyclerView()
                viewModel.initSetting()
                getFeed()   //로그인 상태 변경시 게시글 다시 불러옴
                viewModel.originLoginFlag = Auth.loginFlag.value!!
            }
        }

        /*   게시글 관련 Observer   */
        viewModel.feedInfoLiveData.observe(this) {
            //새로운 feed 정보를 받았을 때의 처리
            pageNo = viewModel.nextPage //다음 page 정보
            viewModel.feedInfoLiveData.value?.let { myFeedDetailAdapter.addFeedInfoData(it.feeds) }   //새로 받은 게시글 정보 추가
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

    @SuppressLint("NotifyDataSetChanged")
    private fun clearRecyclerView() {
        if(::myFeedDetailAdapter.isInitialized) {
            myFeedDetailAdapter.feedInfo.clear()
            myFeedDetailAdapter.notifyDataSetChanged()
            Log.d("CLEAR TEST", "RecyclerView is Cleared")
        }
    }

    private fun getFeed() {
        //게시글 요청 함수
        if(likePostFlag){
            viewModel.fetchLikeFeedResults()
            Log.d("getFeed", "Like")
        }
        else if(query.isEmpty() && userId.isEmpty()) {
            if(Auth.loginFlag.value == false) {   //로그인 X인 경우
                viewModel.fetchFeedResults()
                Log.d("getFeed", "Normal")
            }
            else{
                viewModel.fetchPersonalFeedResults()
                Log.d("getFeed", "Personal")
            }
        }
        else if(userId.isNotEmpty()) {
            viewModel.fetchUserFeedResults(userId)
            Log.d("getFeed", "User")
        }
        else if(query.isNotEmpty()){
            viewModel.fetchSearchFeedResults(query)
            Log.d("getFeed", "Query")
        }
    }

}