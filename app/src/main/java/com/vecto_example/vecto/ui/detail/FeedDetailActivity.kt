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
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
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
import com.vecto_example.vecto.data.model.PathData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.repository.FeedRepository
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityFeedDetailBinding
import com.vecto_example.vecto.utils.MapMarkerManager
import com.vecto_example.vecto.utils.MapOverlayManager
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ShareFeedUtil
import com.vecto_example.vecto.utils.ToastMessageUtils
import com.vecto_example.vecto.utils.ToastMessageUtils.errorMessageHandler
import kotlinx.coroutines.launch

class FeedDetailActivity : AppCompatActivity(), OnMapReadyCallback, MyFeedDetailAdapter.OnFeedActionListener {
    private lateinit var binding: ActivityFeedDetailBinding
    private lateinit var mapMarkerManager: MapMarkerManager
    private lateinit var mapOverlayManager: MapOverlayManager

    private val viewModel: FeedDetailViewModel by viewModels {
        FeedDetailViewModelFactory(FeedRepository(VectoService.create()), UserRepository(VectoService.create()), TokenRepository(VectoService.create()))
    }

    private lateinit var myFeedDetailAdapter: MyFeedDetailAdapter

    //map 설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    private var offset = 500
    private var lastPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFeedDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initObservers()
        viewModel.startLoading()
        initMap()

        initSlideLayout()

        binding.BackButton.setOnClickListener {
            finish()
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSlideLayout() {
        val topMargin = dpToPx(250f, this)
        val bottomMargin = dpToPx(250f, this)
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

        initRecyclerView()
    }

    private fun initRecyclerView() {
        myFeedDetailAdapter = MyFeedDetailAdapter()
        myFeedDetailAdapter.feedActionListener = this

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
                    setOverlayAndCamera(lastVisibleItemPosition)
                }
                else {
                    // 중앙 아이템의 정보를 가져와서 처리
                    if (centerPosition >= 0 && centerPosition < myFeedDetailAdapter.feedInfoWithFollow.size) {
                        setOverlayAndCamera(centerPosition)
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

    private fun setOverlayAndCamera(position: Int) {
        if(lastPosition != position) {

            val feedInfo = myFeedDetailAdapter.feedInfoWithFollow[position]
            mapOverlayManager.addOverlayForPost(feedInfo.feedInfo)

            if(feedInfo.feedInfo.visit.size == 1)
                mapOverlayManager.moveCameraForVisitOffset(feedInfo.feedInfo.visit.first(), offset)
            else
                mapOverlayManager.moveCameraForPathOffset(feedInfo.feedInfo.location.toMutableList(), dpToPx((offset + 20).toFloat(), this))

            lastPosition = position
        }
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

        viewModel.endLoading()
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
            if(myFeedDetailAdapter.postLikePosition != -1)
            {
                postFeedLikeResult.onSuccess {
                    myFeedDetailAdapter.postFeedLikeSuccess()
                }.onFailure {
                    ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
                }

                myFeedDetailAdapter.postLikePosition = -1
            }
        }

        viewModel.deleteFeedLikeResult.observe(this) { deleteFeedLikeResult ->
            if(myFeedDetailAdapter.deleteLikePosition != -1) {
                deleteFeedLikeResult.onSuccess {
                    myFeedDetailAdapter.deleteFeedLikeSuccess()
                }.onFailure {
                    ToastMessageUtils.showToast(this, getString(R.string.APIErrorToastMessage))
                }

                myFeedDetailAdapter.deleteLikePosition = -1
            }
        }

        //팔로우
        viewModel.postFollowResult.observe(this) {
            if(myFeedDetailAdapter.postFollowPosition != -1) {
                if (it) {
                    myFeedDetailAdapter.postFollowSuccess()
                    ToastMessageUtils.showToast(this, getString(R.string.post_follow_success, viewModel.allFeedInfo[myFeedDetailAdapter.postFollowPosition].feedInfo.nickName))
                } else {
                    ToastMessageUtils.showToast(this, getString(R.string.post_follow_already, viewModel.allFeedInfo[myFeedDetailAdapter.postFollowPosition].feedInfo.nickName))
                }

                myFeedDetailAdapter.postFollowPosition = -1
            }
        }

        viewModel.deleteFollowResult.observe(this) {
            if(myFeedDetailAdapter.deleteFollowPosition != -1) {
                if (it) {
                    myFeedDetailAdapter.deleteFollowSuccess()
                    ToastMessageUtils.showToast(this, getString(R.string.delete_follow_success, viewModel.allFeedInfo[myFeedDetailAdapter.deleteFollowPosition].feedInfo.nickName))
                } else {
                    ToastMessageUtils.showToast(this, getString(R.string.delete_follow_already, viewModel.allFeedInfo[myFeedDetailAdapter.deleteFollowPosition].feedInfo.nickName))
                }

                myFeedDetailAdapter.deleteFollowPosition = -1
            }
        }

        lifecycleScope.launch {
            viewModel.reissueResponse.collect {
                SaveLoginDataUtils.changeToken(this@FeedDetailActivity, it.userToken.accessToken, it.userToken.refreshToken)

                when(it.function){
                    FeedDetailViewModel.Function.GetFeedList.name -> {
                        getFeed()
                    }
                    FeedDetailViewModel.Function.PostFeedLike.name -> {
                        viewModel.postFeedLike(viewModel.postFeedLikeId)
                    }
                    FeedDetailViewModel.Function.DeleteFeedLike.name -> {
                        viewModel.deleteFeedLike(viewModel.deleteFeedLikeId)
                    }
                    FeedDetailViewModel.Function.PostFollow.name -> {
                        viewModel.postFollow(viewModel.postFollowId)
                    }
                    FeedDetailViewModel.Function.DeleteFollow.name -> {
                        viewModel.deleteFollow(viewModel.deleteFollowId)
                    }
                    FeedDetailViewModel.Function.CheckFollow.name -> {
                        viewModel.checkFollow(viewModel.newFeedInfoWithFollow, viewModel.feedPageResponse)
                    }
                }
            }
        }


        /*   오류 관련 Observer   */
        viewModel.errorMessage.observe(this){
            ToastMessageUtils.showToast(this, getString(it))

            if(it == R.string.expired_login)
                SaveLoginDataUtils.deleteData(this)
        }

        viewModel.postFollowError.observe(this) {
            if(myFeedDetailAdapter.postFollowPosition != -1) {
                errorMessageHandler(this, ToastMessageUtils.UserInterActionType.FOLLOW_POST.name, it)
                myFeedDetailAdapter.postFollowPosition = -1
            }
        }

        viewModel.deleteFollowError.observe(this) {
            if(myFeedDetailAdapter.deleteFollowPosition != -1) {
                errorMessageHandler(this, ToastMessageUtils.UserInterActionType.FOLLOW_DELETE.name, it)
                myFeedDetailAdapter.deleteFollowPosition = -1
            }
        }
    }

    private fun getFeed() {
        //게시글 요청 함수
        viewModel.getFeedList()
    }

    override fun onPostFeedLike(feedId: Int) {
        if(!viewModel.postLikeLoading) {
            viewModel.postFeedLike(feedId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            myFeedDetailAdapter.postLikePosition = -1
        }
    }

    override fun onDeleteFeedLike(feedId: Int) {
        if(!viewModel.deleteLikeLoading) {
            viewModel.deleteFeedLike(feedId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            myFeedDetailAdapter.deleteLikePosition = -1
        }
    }

    override fun onPostFollow(userId: String) {
        if(!viewModel.postFollowLoading) {
            viewModel.postFollow(userId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            myFeedDetailAdapter.postFollowPosition = -1
        }
    }

    override fun onDeleteFollow(userId: String) {
        if(!viewModel.deleteFollowLoading) {
            viewModel.deleteFollow(userId)
        } else {
            ToastMessageUtils.showToast(this, getString(R.string.task_duplication))
            myFeedDetailAdapter.deleteFollowPosition = -1
        }
    }

    override fun onTitleClick(position: Int) {
        mapOverlayManager.deletePathOverlay()
        mapOverlayManager.addPathOverlayForLocation(myFeedDetailAdapter.feedInfoWithFollow[position].feedInfo.location.toMutableList())

        if(myFeedDetailAdapter.feedInfoWithFollow[position].feedInfo.visit.size == 1)
            mapOverlayManager.moveCameraForVisitOffset(myFeedDetailAdapter.feedInfoWithFollow[position].feedInfo.visit.first(), offset)
        else
            mapOverlayManager.moveCameraForPathOffset(myFeedDetailAdapter.feedInfoWithFollow[position].feedInfo.location.toMutableList(), dpToPx((offset + 20).toFloat(), this))
    }

    override fun onShareClick(feedInfoWithFollow: VectoService.FeedInfoWithFollow) {
        ShareFeedUtil.shareFeed(this, feedInfoWithFollow.feedInfo)
    }

    override fun onVisitItemClick(visitData: VisitData, itemPosition: Int) {
        mapMarkerManager.showNumberMarker(itemPosition)

        mapOverlayManager.moveCameraForVisitOffset(visitData, offset)
    }

    override fun onPathItemClick(pathDataList: MutableList<PathData>, itemPosition: Int) {
        mapOverlayManager.moveCameraForPathOffsetWithAnimation(pathDataList[itemPosition].coordinates, dpToPx((offset + 20).toFloat(), this))
        mapOverlayManager.setHighLightOverlay(pathDataList, itemPosition)
    }
}