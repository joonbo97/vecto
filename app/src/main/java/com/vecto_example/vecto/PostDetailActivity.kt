package com.vecto_example.vecto

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.PointF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.data.LocationData
import com.vecto_example.vecto.data.VisitData
import com.vecto_example.vecto.retrofit.VectoService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.databinding.ActivityPostDetailBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.max

class PostDetailActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var myPostDetailAdapter: MyPostDetailAdapter

    //map설정 관련
    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    private val visitMarkers = mutableListOf<Marker>()
    private val pathOverlays = mutableListOf<PathOverlay>()

    private var pageList = mutableListOf<Int>()
    private var responseData = mutableListOf<VectoService.PostResponse>()
    private var responsePageData = mutableListOf<Int>()

    var cnt = 0

    var query = ""
    var pageNo = 0

    var lastY = 0f


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initMap()

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


    private fun addOverlayForPost(feedInfo: VectoService.PostResponse) {
        deleteOverlay()

        for(i in 0 until feedInfo.visit.size)
            addVisitMarker(feedInfo.visit[i])

        addPathOverlayForLoacation(feedInfo.location.toMutableList())

        if(feedInfo.visit.size == 1)
        {
            moveCameraForVisit(feedInfo.visit[0])
        }
        else
        {
            moveCameraForPath(feedInfo.location.toMutableList())
        }

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

        myPostDetailAdapter = MyPostDetailAdapter(this)
        val postDetailRecyclerView = binding.PostDetailRecyclerView
        postDetailRecyclerView.adapter = myPostDetailAdapter
        postDetailRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        postDetailRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager


                // 현재 보이는 아이템들의 위치 확인
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // 아이템의 정보를 가져와서 처리합니다.
                for (position in firstVisibleItemPosition..lastVisibleItemPosition) {
                    val feedInfo = myPostDetailAdapter.feedInfo[position]
                    addOverlayForPost(feedInfo)
                }

                if (!recyclerView.canScrollVertically(1)) {
                    if(pageNo != -1)
                    {
                        pageNo++
                        if(query.isEmpty())
                            getPostList()
                        else
                            getSearchPostList(query)
                    }
                }
            }
        })


        // Intent에서 JSON 문자열을 가져와 리스트로 변환
        val feedInfo = intent.getStringExtra("feedInfoListJson")
        val feedID = intent.getStringExtra("feedIDListJson")
        val position = intent.getIntExtra("position", -1)
        pageNo = intent.getIntExtra("pageNo", -1)
        val intentQuery = intent.getStringExtra("query")
        if(!intentQuery.isNullOrEmpty()){
            query = intentQuery
        }


        // JSON 문자열을 객체 리스트로 변환
        val typeOfFeedInfoList = object : TypeToken<List<VectoService.PostResponse>>() {}.type
        val feedInfoList = Gson().fromJson<List<VectoService.PostResponse>>(feedInfo, typeOfFeedInfoList)

        val typeOfFeedIDList = object : TypeToken<List<Int>>() {}.type
        val feedIDList = Gson().fromJson<List<Int>>(feedID, typeOfFeedIDList)

        // 어댑터에 데이터 설정
        myPostDetailAdapter.feedInfo.addAll(feedInfoList)
        myPostDetailAdapter.feedID.addAll(feedIDList)
        myPostDetailAdapter.notifyDataSetChanged()

        if(position != -1)
            (binding.PostDetailRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)

    }

    private fun getPostList() {
        val vectoService = VectoService.create()

        val call = vectoService.getFeedList(pageNo)
        call.enqueue(object : Callback<VectoService.VectoResponse<List<Int>>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<List<Int>>>, response: Response<VectoService.VectoResponse<List<Int>>>) {
                if(response.isSuccessful){
                    Log.d("POSTID", "성공: ${response.body()}")

                    cnt = 0
                    responseData.clear()
                    responsePageData.clear()

                    if(response.body()?.result?.isEmpty() == true)
                    {
                        pageNo = -1
                    }
                    else
                    {
                        pageList = response.body()?.result!!.toMutableList()

                        for(item in response.body()!!.result!!){
                            getPostInfo(item)
                        }

                    }
                }
                else{
                    Log.d("POSTID", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<List<Int>>>, t: Throwable) {
                Log.d("POSTID", "실패")
            }

        })
    }

    private fun getPostInfo(feedid: Int) {
        val vectoService = VectoService.create()

        val call: Call<VectoService.VectoResponse<VectoService.PostResponse>>

        if(Auth.loginFlag.value == true)
        {
            call = vectoService.getFeedInfo("Bearer ${Auth.token}", feedid)
        }
        else
        {
            call = vectoService.getFeedInfo(feedid)
        }

        call.enqueue(object : Callback<VectoService.VectoResponse<VectoService.PostResponse>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<VectoService.PostResponse>>, response: Response<VectoService.VectoResponse<VectoService.PostResponse>>) {
                if(response.isSuccessful){
                    Log.d("POSTINFO", "성공: ${response.body()}")

                    val result = response.body()!!.result


                    responseData.add(result!!)
                    responsePageData.add(feedid)
                    cnt++

                    if(cnt == pageList.size)//마지막 항목일 경우
                    {

                        var idxcnt = 0


                        while(cnt != 0) {
                            for (i in 0 until pageList.size) {
                                if (pageList[idxcnt] == responsePageData[i]) {
                                    myPostDetailAdapter.feedInfo.add(responseData[i])
                                    myPostDetailAdapter.feedID.add(responsePageData[i])
                                    cnt--
                                    break
                                }
                            }

                            idxcnt++
                        }

                        myPostDetailAdapter.notifyDataSetChanged()
                    }
                }
                else{
                    Log.d("POSTINFO", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<VectoService.PostResponse>>, t: Throwable) {
                Log.d("POSTINFO", "실패")
            }

        })
    }

    private fun getSearchPostList(q: String) {
        val vectoService = VectoService.create()

        val call = vectoService.getSearchFeedList(pageNo, q)
        call.enqueue(object : Callback<VectoService.VectoResponse<List<Int>>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<List<Int>>>, response: Response<VectoService.VectoResponse<List<Int>>>) {
                if(response.isSuccessful){
                    Log.d("SEARCHPOSTID", "성공: ${response.body()}")

                    cnt = 0
                    responseData.clear()
                    responsePageData.clear()

                    if(response.body()?.result?.isEmpty() == true)
                    {
                        pageNo = -1
                    }
                    else
                    {
                        pageList = response.body()?.result!!.toMutableList()

                        for(item in response.body()!!.result!!){
                            getPostInfo(item)
                        }

                    }
                }
                else{
                    Log.d("SEARCHPOSTID", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<List<Int>>>, t: Throwable) {
                Log.d("SEARCHPOSTID", "실패")
            }

        })
    }


    private fun addVisitMarker(visitData: VisitData){
        val visitMarker = Marker()

        visitMarker.icon = OverlayImage.fromResource(getMarkerIcon())

        if(visitData.name.isNotEmpty()) {
            visitMarker.position = LatLng(visitData.lat_set, visitData.lng_set)
        }
        else {
            visitMarker.position = LatLng(visitData.lat, visitData.lng)
        }

        visitMarker.map = naverMap

        visitMarkers.add(visitMarker)
    }

    private fun addPathOverlayForLoacation(pathPoints: MutableList<LocationData>){
        val pathLatLng = mutableListOf<LatLng>()

        for(i in 0 until pathPoints.size) {
            pathLatLng.add(LatLng(pathPoints[i].lat, pathPoints[i].lng))
        }

        addPathOverlay(pathLatLng)
    }

    private fun addPathOverlay(pathPoints: MutableList<LatLng>){
        val pathOverlay = PathOverlay()

        if(pathPoints.size > 1) {
            pathOverlay.coords = pathPoints
            pathOverlay.width = 20
            pathOverlay.color = ContextCompat.getColor(this, R.color.vecto_pathcolor)
            pathOverlay.outlineColor = ContextCompat.getColor(this, R.color.vecto_pathcolor)
            pathOverlay.patternImage = OverlayImage.fromResource(R.drawable.pathoverlay_pattern)
            pathOverlay.patternInterval = 50
            pathOverlay.map = naverMap
            pathOverlays.add(pathOverlay)
        }
    }

    private fun deleteOverlay() {
        pathOverlays.forEach{ it.map = null}
        pathOverlays.clear()

        visitMarkers.forEach { it.map = null }
        visitMarkers.clear()
    }

    private fun getMarkerIcon(): Int{
        when(visitMarkers.size){
            0 -> return R.drawable.marker_number_1
            1 -> return R.drawable.marker_number_2
            2 -> return R.drawable.marker_number_3
            3 -> return R.drawable.marker_number_4
            4 -> return R.drawable.marker_number_5
            5 -> return R.drawable.marker_number_6
            6 -> return R.drawable.marker_number_7
            7 -> return R.drawable.marker_number_8
            8 -> return R.drawable.marker_number_9
            else -> return R.drawable.marker_image
        }

    }

    private fun moveCameraForPath(pathPoints: MutableList<LocationData>){
        if(pathPoints.isNotEmpty()) {
            val minLat = pathPoints.minOf { it.lat }
            val maxLat = pathPoints.maxOf { it.lat }
            val minLng = pathPoints.minOf { it.lng }
            val maxLng = pathPoints.maxOf { it.lng }

            val bounds = LatLngBounds(LatLng(minLat , minLng), LatLng(maxLat, maxLng))
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 150, 200, 150, 150))
            val Offset = PointF(0.0f, (-50).toFloat())
            naverMap.moveCamera(CameraUpdate.scrollBy(Offset))

        }
    }

    private fun moveCameraForVisit(visit: VisitData){
        val targetLatLng = LatLng(visit.lat_set, visit.lng_set)
        val Offset = PointF(0.0f, (-50).toFloat())

        naverMap.moveCamera(CameraUpdate.scrollTo(targetLatLng))
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        naverMap.moveCamera(CameraUpdate.scrollBy(Offset))
    }

}