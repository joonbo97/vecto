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
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityFeedDetailBinding
import com.vecto_example.vecto.utils.MapMarkerManager
import com.vecto_example.vecto.utils.MapOverlayManager

class FeedDetailActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityFeedDetailBinding
    private lateinit var mapMarkerManager: MapMarkerManager
    private lateinit var mapOverlayManager: MapOverlayManager

    private val viewModel: FeedDetailViewModel by viewModels {
        FeedDetailViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()))
    }

    private lateinit var myFeedDetailAdapter: MyFeedDetailAdapter

    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    private var offset = 500
    private var lastPosition = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFeedDetailBinding.inflate(layoutInflater)
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
        var lastY = 0f

        binding.slide.setOnTouchListener { view, event ->
            val layoutParams = binding.constraintLayout2.layoutParams as ConstraintLayout.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newY = event.rawY
                    val deltaY = newY - lastY
                    var newHeight = layoutParams.height - deltaY.toInt()

                    // 상단 마진과 하단 마진을 고려하여 새로운 높이를 조정합니다.
                    newHeight = newHeight.coerceAtLeast(topMargin)
                    newHeight = newHeight.coerceAtMost(screenHeight - bottomMargin)

                    layoutParams.height = newHeight
                    offset = (newHeight / resources.displayMetrics.density).toInt()
                    binding.constraintLayout2.layoutParams = layoutParams
                    binding.constraintLayout2.requestLayout()

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
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
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

        myFeedDetailAdapter = MyFeedDetailAdapter()
        binding.PostDetailRecyclerView.adapter = myFeedDetailAdapter

        setViewModelData()

        binding.PostDetailRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.PostDetailRecyclerView.itemAnimator = null
        binding.PostDetailRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager


                // 현재 보이는 아이템들의 위치 확인
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // 중앙 아이템 인덱스 계산
                val centerPosition = (firstVisibleItemPosition + lastVisibleItemPosition) / 2



                if(lastVisibleItemPosition == myFeedDetailAdapter.feedInfoWithFollow.lastIndex){    //마지막 아이템
                    if(lastPosition != lastVisibleItemPosition) {
                        val feedInfo = myFeedDetailAdapter.feedInfoWithFollow[lastVisibleItemPosition]
                        mapOverlayManager.addOverlayForPost(feedInfo.feedInfo)

                        if(feedInfo.feedInfo.visit.size == 1)
                            mapOverlayManager.moveCameraForVisitOffset(feedInfo.feedInfo.visit.first(), offset)
                        else
                            mapOverlayManager.moveCameraForPathOffset(feedInfo.feedInfo.location.toMutableList(), offset)

                        lastPosition = lastVisibleItemPosition
                    }
                }
                else {
                    // 중앙 아이템의 정보를 가져와서 처리
                    if (centerPosition >= 0 && centerPosition < myFeedDetailAdapter.feedInfoWithFollow.size) {
                        if(lastPosition != centerPosition){
                            val feedInfo = myFeedDetailAdapter.feedInfoWithFollow[centerPosition]
                                mapOverlayManager.addOverlayForPost(feedInfo.feedInfo) // 중앙 아이템 강조

                                if(feedInfo.feedInfo.visit.size == 1)
                                    mapOverlayManager.moveCameraForVisitOffset(feedInfo.feedInfo.visit.first(), offset)
                                else
                                    mapOverlayManager.moveCameraForPathOffset(feedInfo.feedInfo.location.toMutableList(), offset)

                                lastPosition = centerPosition
                        }
                    }
                }



                if (!recyclerView.canScrollVertically(1)) {
                    if(!viewModel.checkLoading() && myFeedDetailAdapter.lastSize == viewModel.allFeedInfo.size) {
                        Log.d("FeedDetailActivity", "getFeed")
                        getFeed()
                    }
                }
            }
        })


    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setViewModelData() {
        // 인텐트로부터 값 가져오기
        val feedInfoWithFollowIntent = intent.getStringExtra("feedInfoListJson")
        val type = intent.getStringExtra("type")
        val query = intent.getStringExtra("query")
        val nextPage = intent.getIntExtra("nextPage", 0)
        val followPage = intent.getBooleanExtra("followPage", false)
        val lastPage = intent.getBooleanExtra("lastPage", false)

        // JSON 문자열을 객체 리스트로 변환
        val typeOfFeedInfoList = object : TypeToken<List<VectoService.FeedInfoWithFollow>>() {}.type
        val feedInfoWithFollow = Gson().fromJson<List<VectoService.FeedInfoWithFollow>>(feedInfoWithFollowIntent, typeOfFeedInfoList)

        /*   ViewModel 데이터 설정   */
        if(type != null)
            viewModel.type = type
        if(query != null)
            viewModel.query = query
        viewModel.nextPage = nextPage
        viewModel.followPage = followPage
        viewModel.lastPage = lastPage
        viewModel.userId = feedInfoWithFollow[0].feedInfo.userId
        viewModel.allFeedInfo.addAll(feedInfoWithFollow)

        myFeedDetailAdapter.feedInfoWithFollow = viewModel.allFeedInfo
        myFeedDetailAdapter.lastSize = viewModel.allFeedInfo.size
        myFeedDetailAdapter.notifyDataSetChanged()

        Log.d("ASD123", "${myFeedDetailAdapter.lastSize}, ${viewModel.allFeedInfo.size}")

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initObservers() {
        /*   로그인 관련 Observer   */
        Auth.loginFlag.observe(this) {
            if(Auth.loginFlag.value != viewModel.originLoginFlag) {
                if(viewModel.originLoginFlag == null){
                    viewModel.originLoginFlag = Auth.loginFlag.value
                } else {
                    viewModel.initSetting()
                    getFeed()   //로그인 상태 변경시 게시글 다시 불러옴
                    viewModel.originLoginFlag = Auth.loginFlag.value
                }
            }
        }

        /*   게시글 관련 Observer   */
        viewModel.feedInfoLiveData.observe(this) {
            Log.d("ASD", "${myFeedDetailAdapter.lastSize}, ${viewModel.allFeedInfo.size}")

            if(viewModel.firstFlag) {
                myFeedDetailAdapter.feedInfoWithFollow = viewModel.allFeedInfo
                myFeedDetailAdapter.lastSize = viewModel.allFeedInfo.size

                myFeedDetailAdapter.notifyDataSetChanged()
                viewModel.firstFlag = false
            }
            else
                myFeedDetailAdapter.addFeedData()
        }

        /*   로딩 관련 Observer   */
        viewModel.isLoadingCenter.observe(this) {
            if(it)
                binding.progressBarCenter.visibility = View.VISIBLE
            else {
                binding.progressBarCenter.visibility = View.GONE
            }
        }
        viewModel.isLoadingBottom.observe(this) {
            if(it)
                binding.progressBar.visibility = View.VISIBLE
            else
                binding.progressBar.visibility = View.GONE
        }

        /*   게시물 상호 작용 관련 Observer   */

        /*   게시글 좋아요   */
        viewModel.postFeedLikeResult.observe(this) { postFeedLikeResult ->
            if(myFeedDetailAdapter.actionPosition != -1)
            {
                postFeedLikeResult.onSuccess {
                    myFeedDetailAdapter.postFeedLikeSuccess()
                }.onFailure {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                myFeedDetailAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFeedLikeResult.observe(this) { deleteFeedLikeResult ->
            if(myFeedDetailAdapter.actionPosition != -1) {
                deleteFeedLikeResult.onSuccess {
                    myFeedDetailAdapter.deleteFeedLikeSuccess()
                }.onFailure {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                myFeedDetailAdapter.actionPosition = -1
            }
        }

        //팔로우
        viewModel.postFollowResult.observe(this) {
            if(myFeedDetailAdapter.actionPosition != -1) {
                if (it) {
                    myFeedDetailAdapter.postFollowSuccess()
                    Toast.makeText(this, "${viewModel.allFeedInfo[myFeedDetailAdapter.actionPosition].feedInfo.nickName} 님을 팔로우하기 시작했습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미 ${viewModel.allFeedInfo[myFeedDetailAdapter.actionPosition].feedInfo.nickName} 님을 팔로우 중입니다.", Toast.LENGTH_SHORT).show()
                }

                myFeedDetailAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFollowResult.observe(this) {
            if(myFeedDetailAdapter.actionPosition != -1) {
                if (it) {
                    myFeedDetailAdapter.deleteFollowSuccess()
                    Toast.makeText(this, "${viewModel.allFeedInfo[myFeedDetailAdapter.actionPosition].feedInfo.nickName} 님 팔로우를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "이미 ${viewModel.allFeedInfo[myFeedDetailAdapter.actionPosition].feedInfo.nickName} 님을 팔로우하지 않습니다.", Toast.LENGTH_SHORT).show()
                }

                myFeedDetailAdapter.actionPosition = -1
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
            if(myFeedDetailAdapter.actionPosition != -1) {
                if (it == "FAIL") {
                    Toast.makeText(this, "팔로우 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                myFeedDetailAdapter.actionPosition = -1
            }
        }

        viewModel.deleteFollowError.observe(this) {
            if(myFeedDetailAdapter.actionPosition != -1) {
                if (it == "FAIL") {
                    Toast.makeText(this, "팔로우 취소 요청에 실패하였습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }

                myFeedDetailAdapter.actionPosition = -1
            }
        }
    }

    private fun getFeed() {
        //게시글 요청 함수
        viewModel.getFeedList()
    }

}