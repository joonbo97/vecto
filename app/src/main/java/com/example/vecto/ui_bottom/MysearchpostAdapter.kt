package com.example.vecto.ui_bottom

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.vecto.R
import com.example.vecto.data.LocationData
import com.example.vecto.data.VisitData
import com.example.vecto.retrofit.VectoService
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MysearchpostAdapter(private val context: Context) : RecyclerView.Adapter<MysearchpostAdapter.ViewHolder>()
{
    val feedInfo = mutableListOf<VectoService.PostResponse>()
    lateinit var visitdata: List<VisitData>
    lateinit var locationdata: List<LocationData>

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {

        private val titleText: TextView = view.findViewById(R.id.TitleText)
        private val profileImage: ImageView = view.findViewById(R.id.ProfileImage)
        private val nicknameText: TextView = view.findViewById(R.id.NicknameText)
        private val posttimeText: TextView = view.findViewById(R.id.PostTimeText)
        private val followButton: ImageView = view.findViewById(R.id.FollowButton)
        private val followText: TextView = view.findViewById(R.id.ButtonText)
        private val postImage: ImageView = view.findViewById(R.id.Image)

        private val courseTime: TextView = view.findViewById(R.id.TotalTimeText)

        private val likeCount: TextView = view.findViewById(R.id.LikeCountText)
        private val likeIcon: ImageView = view.findViewById(R.id.LikeImage)
        private val commentCount: TextView = view.findViewById(R.id.CommentCountText)
        private val commentIcon: ImageView = view.findViewById(R.id.CommentImage)

        private var naverMap: NaverMap? = null

        private val visitMarkers = mutableListOf<Marker>()
        private val pathOverlays = mutableListOf<PathOverlay>()


        fun bind(feed: VectoService.PostResponse) {
            val largeMapView: MapView = itemView.findViewById(R.id.naver_map_Large)
            val smallMapView: MapView = itemView.findViewById(R.id.naver_map_Small)

            Log.d("FEED", "FeedImage Size: ${feed.image.size}")
            //이미지가 있는지 여부를 확인하여 style을 결정
            if (feed.image.isEmpty()) {
                largeMapView.getMapAsync(largeMapCallback)
                smallMapView.visibility = View.GONE
                postImage.visibility = View.GONE
                largeMapView.visibility = View.VISIBLE
                Log.d("FEED", "Large Map Visibility: ${largeMapView.visibility}")
                Log.d("FEED", "Small Map Visibility: ${smallMapView.visibility}")
            } else {
                smallMapView.getMapAsync(smallMapCallback)
                largeMapView.visibility = View.GONE
                postImage.visibility = View.VISIBLE
                smallMapView.visibility = View.VISIBLE
                Log.d("FEED", "Large Map Visibility: ${largeMapView.visibility}")
                Log.d("FEED", "Small Map Visibility: ${smallMapView.visibility}")
            }
            deleteOverlay()

            visitdata = feed.visit
            locationdata = feed.location


            titleText.text = feed.title

            if(feed.userProfile != null)
            {
                Glide.with(context)
                    .load(feed.userProfile)
                    .placeholder(R.drawable.profile_basic) // 로딩 중 표시될 이미지
                    .error(R.drawable.profile_basic) // 에러 발생 시 표시될 이미지
                    .into(profileImage)
            }

            nicknameText.text = feed.nickName
            posttimeText.text = feed.timeDifference


            //TODO follow 여부에 따라 버튼, 버튼 text 설정 변경

            if(feed.image.isNotEmpty())
            {
                Glide.with(context)
                    .load(feed.image[0])
                    .placeholder(R.drawable.empty_image) // 로딩 중 표시될 이미지
                    .error(R.drawable.empty_image) // 에러 발생 시 표시될 이미지
                    .into(postImage)
            }


            likeCount.text = feed.likecount.toString()
            commentCount.text = feed.commentCount.toString()

            likeIcon.setOnClickListener {

            }

            commentIcon.setOnClickListener {
                //TODO Comment 터치시 댓글창 열어주기
            }

            //TODO 이후에 풀기
            val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

            val date1 = LocalDateTime.parse(feed.visit.first().datetime, FORMAT)
            val date2 = LocalDateTime.parse(feed.visit.last().datetime, FORMAT)

            val minutesPassed = Duration.between(date1, date2).toMinutes().toInt()

            if(minutesPassed < 60)
            {
                courseTime.text = "약 1시간 이내 소요"
            }
            else{
                courseTime.text = "약 ${minutesPassed/60}시간 이내 소요"
            }
        }

        val largeMapCallback = OnMapReadyCallback { naverMap ->
            this@ViewHolder.naverMap = naverMap
            naverMap.uiSettings.isZoomControlEnabled = false
            naverMap.uiSettings.isScrollGesturesEnabled = false
            naverMap.uiSettings.isScaleBarEnabled = false
            naverMap.uiSettings.isLogoClickEnabled = false
            naverMap.uiSettings.isTiltGesturesEnabled = false
            naverMap.uiSettings.isZoomGesturesEnabled = false

            for (visit in visitdata) {
                addVisitMarker(visit, naverMap)
            }

            addPathOverlayForLoacation(locationdata.toMutableList(), naverMap)
            if(visitdata.size == 1)
                moveCameraForVisit(visitdata[0], naverMap)
            else
                moveCameraForPath(locationdata.toMutableList(), naverMap)
        }

        val smallMapCallback = OnMapReadyCallback { naverMap ->
            this@ViewHolder.naverMap = naverMap
            naverMap.uiSettings.isZoomControlEnabled = false
            naverMap.uiSettings.isScrollGesturesEnabled = false
            naverMap.uiSettings.isScaleBarEnabled = false
            naverMap.uiSettings.isLogoClickEnabled = false
            naverMap.uiSettings.isTiltGesturesEnabled = false
            naverMap.uiSettings.isZoomGesturesEnabled = false

            for (visit in visitdata) {
                addVisitMarker(visit, naverMap)
            }

            addPathOverlayForLoacation(locationdata.toMutableList(), naverMap)
            if(visitdata.size == 1)
                moveCameraForVisit(visitdata[0], naverMap)
            else
                moveCameraForPath(locationdata.toMutableList(), naverMap)

            /*naverMap.takeSnapshot { snapshot ->
                postImage.setImageBitmap(snapshot)
            }*/
        }




        private fun addPathOverlayForLoacation(pathPoints: MutableList<LocationData>, naverMap: NaverMap){
            val pathLatLng = mutableListOf<LatLng>()

            for(i in 0 until pathPoints.size) {
                pathLatLng.add(LatLng(pathPoints[i].lat, pathPoints[i].lng))
            }

            addPathOverlay(pathLatLng, naverMap)
        }

        private fun addPathOverlay(pathPoints: MutableList<LatLng>, naverMap: NaverMap){
            val pathOverlay = PathOverlay()

            if(pathPoints.size > 1) {
                pathOverlay.coords = pathPoints
                pathOverlay.width = 20
                pathOverlay.color = ContextCompat.getColor(context, R.color.vecto_pathcolor)
                pathOverlay.patternImage = OverlayImage.fromResource(R.drawable.pathoverlay_pattern)
                pathOverlay.patternInterval = 50
                pathOverlay.map = naverMap
                pathOverlays.add(pathOverlay)
            }
        }

        private fun addVisitMarker(visitData: VisitData, naverMap: NaverMap){
            val visitMarker = Marker()
            if(visitData.name.isNotEmpty())
                visitMarker.icon = OverlayImage.fromResource(R.drawable.marker_image)
            else
                visitMarker.icon = OverlayImage.fromResource(R.drawable.marker_image_off)

            if(visitData.name.isNotEmpty()) {
                visitMarker.position = LatLng(visitData.lat_set, visitData.lng_set)
            }
            else {
                visitMarker.position = LatLng(visitData.lat, visitData.lng)
            }

            visitMarker.map = naverMap

            visitMarkers.add(visitMarker)
        }

        private fun moveCameraForPath(pathPoints: MutableList<LocationData>, naverMap: NaverMap){
            if(pathPoints.isNotEmpty()) {
                val minLat = pathPoints.minOf { it.lat }
                val maxLat = pathPoints.maxOf { it.lat }
                val minLng = pathPoints.minOf { it.lng }
                val maxLng = pathPoints.maxOf { it.lng }

                val bounds = LatLngBounds(LatLng(minLat, minLng), LatLng(maxLat, maxLng))
                naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 20, 150, 20, 20))
            }
        }

        private fun moveCameraForVisit(visit: VisitData, naverMap: NaverMap){
            val targetLatLng = LatLng(visit.lat_set, visit.lng_set)

            naverMap.moveCamera(CameraUpdate.scrollTo(targetLatLng))
            naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        }

        private fun deleteOverlay() {
            pathOverlays.forEach{ it.map = null}
            pathOverlays.clear()

            visitMarkers.forEach { it.map = null }
            visitMarkers.clear()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MysearchpostAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.post_small_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return feedInfo.size
    }

    override fun onBindViewHolder(holder: MysearchpostAdapter.ViewHolder, position: Int) {
        val feed = feedInfo[position]
        holder.bind(feed)
    }

}