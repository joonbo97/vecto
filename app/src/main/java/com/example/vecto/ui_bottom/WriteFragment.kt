package com.example.vecto.ui_bottom

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vecto.R
import com.example.vecto.VerticalOverlapItemDecoration
import com.example.vecto.data.Auth
import com.example.vecto.data.LocationData
import com.example.vecto.data.LocationDatabase
import com.example.vecto.data.PathData
import com.example.vecto.data.VisitData
import com.example.vecto.data.VisitDatabase
import com.example.vecto.databinding.FragmentWriteBinding
import com.example.vecto.retrofit.TMapAPIService
import com.example.vecto.retrofit.VectoService
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Locale

class WriteFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentWriteBinding

    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>
    private lateinit var selectedVisitData: VisitData
    private lateinit var selectedPathData: MutableList<LocationData>

    //overlay 관련
    private val visitMarkers = mutableListOf<Marker>()
    private val pathOverlays = mutableListOf<PathOverlay>()
    private val circleOverlays = mutableListOf<CircleOverlay>()

    private lateinit var imageViews: List<ImageView>
    private var imageCnt = 0

    private var imageUri = mutableListOf<Uri>()

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri = result.data?.data
            startCrop(selectedImageUri!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWriteBinding.inflate(inflater, container, false)

        imageViews = listOf(binding.UcropImage0, binding.UcropImage1, binding.UcropImage2, binding.UcropImage3, binding.UcropImage4, binding.UcropImage5, binding.UcropImage6
                ,binding.UcropImage7, binding.UcropImage8, binding.UcropImage9)

        binding.PhotoBoxImage.setOnClickListener {
            openGallery()
        }

        binding.LocationBoxImage.setOnClickListener {
            getCourse()
        }

        binding.WriteDoneButton.setOnClickListener {
            //TODO 형식 확인. 이름, 경로



            uploadImageToServer(VectoService.PostData(binding.EditTitle.text.toString(), binding.EditContent.text.toString(), LocalDateTime.now().withNano(0).toString(), null, locationDataList, visitDataList))
        }

        return binding.root
    }
    private fun getCourse() {
        //TODO 내 경로를 가져오는 작업
        initMap()
    }

    private fun initMap() {
        mapView = childFragmentManager.findFragmentById(R.id.naver_map_Write) as MapFragment?
            ?: MapFragment.newInstance().also {
                childFragmentManager.beginTransaction().add(R.id.naver_map_Write, it).commit()
            }
        mapView.getMapAsync(this)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, destinationFileName))
        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(uCropOptions())
            .withAspectRatio(1f, 1f) // 1:1 비율로 자르기
            .getIntent(requireContext())
        cropResultLauncher.launch(uCropIntent)
    }


    private fun uCropOptions(): UCrop.Options {
        val options = UCrop.Options()
        // 하단 컨트롤 숨기기
        options.setHideBottomControls(true)
        options.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.black))
        options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.black))
        options.setToolbarWidgetColor(ContextCompat.getColor(requireContext(), R.color.white))

        // 격자 선 숨기기
        options.setCropGridRowCount(0)
        options.setCropGridColumnCount(0)
        return options
    }

    private val cropResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val croppedImageUri = UCrop.getOutput(result.data!!)
            addImage(croppedImageUri!!)
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Log.e("UCrop", "Crop error: $cropError")
        }
    }



    private fun addImage(newImageUri: Uri) {
        imageCnt++
        // 이미지를 오른쪽으로 밀어냄
        for (i in imageViews.size - 2 downTo 0) {
            val currentDrawable = imageViews[i].drawable
            if (currentDrawable != null) {
                imageViews[i + 1].setImageDrawable(currentDrawable)
            }
        }

        imageUri.add(newImageUri)

        // 가장 앞쪽의 ImageView에 새 이미지를 셋팅
        imageViews[0].setImageURI(newImageUri)
        binding.PhotoIconText.text = "$imageCnt/10"
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        naverMap.uiSettings.isZoomControlEnabled = false
        naverMap.uiSettings.isScrollGesturesEnabled = false
        naverMap.uiSettings.isScaleBarEnabled = false
        naverMap.uiSettings.isLogoClickEnabled = false
        naverMap.uiSettings.isTiltGesturesEnabled = false
        naverMap.locationOverlay.isVisible = false

        showDatePickerDialog()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showDatePickerDialog(){
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->

            val selectedDate = if((selectedMonth > 8) && (selectedDay > 9)) {
                "$selectedYear-${selectedMonth + 1}-$selectedDay"
            } else if(selectedMonth > 8) {
                "$selectedYear-${selectedMonth + 1}-0$selectedDay"
            } else if(selectedDay > 9) {
                "$selectedYear-0${selectedMonth + 1}-$selectedDay"
            } else{
                "$selectedYear-0${selectedMonth + 1}-0$selectedDay"
            }

            val previousDate = getPreviousDate(selectedDate)

            val filteredData = VisitDatabase(requireContext()).getAllVisitData().filter { visitData ->
                val visitDate = visitData.datetime.substring(0, 10)
                val endDate = visitData.endtime.substring(0, 10)
                visitDate == previousDate && endDate == selectedDate
            }

            visitDataList = VisitDatabase(requireContext()).getAllVisitData().filter {
                it.datetime.startsWith(selectedDate)
            }.toMutableList()

            //종료 시간이 선택 날짜인 방문지 추가
            if(filteredData.isNotEmpty())
                visitDataList.add(0, filteredData[0])

            if(visitDataList.isNotEmpty()){
                //방문 장소가 있을 경우
                binding.naverMapWrite.visibility = View.VISIBLE
                deleteOverlay()

                //선택한 날짜의 방문지의 처음과 끝까지의 경로
                locationDataList = LocationDatabase(requireContext()).getBetweenLocationData(visitDataList.first().datetime, visitDataList.last().datetime)

                addPathOverlayForLoacation(locationDataList)
                moveCameraForPath(locationDataList)


                val locationDataforPath = mutableListOf<LocationData>()

                //location 첫 좌표 넣어줌.
                locationDataforPath.add(LocationData(visitDataList[0].datetime, visitDataList[0].lat_set, visitDataList[0].lng_set))

                for (visitdatalist in visitDataList){
                    addVisitMarker(visitdatalist)
                }
            }
            else{
                //방문 장소 없을 경우
                deleteOverlay()
            }

        }, year, month, day).show()
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
            pathOverlay.color = ContextCompat.getColor(requireContext(), R.color.vecto_pathcolor)
            pathOverlay.patternImage = OverlayImage.fromResource(R.drawable.pathoverlay_pattern)
            pathOverlay.patternInterval = 50
            pathOverlay.map = naverMap
            pathOverlays.add(pathOverlay)
        }
    }

    private fun addVisitMarker(visitData: VisitData){
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

    private fun addPlaceMarker(poi: TMapAPIService.Poi){
        val visitMarker = Marker()

        visitMarker.position = LatLng(poi.frontLat, poi.frontLon)
        visitMarker.subCaptionText = poi.name
        visitMarker.map = naverMap

        visitMarkers.add(visitMarker)
    }

    private fun addCircleOverlay(visitData: VisitData){
        val circleOverlay = CircleOverlay()
        circleOverlay.center = LatLng(visitData.lat, visitData.lng)
        circleOverlay.radius = 50.0 // 반지름을 50m로 설정

        if(visitData.name.isEmpty()) {
            circleOverlay.color = Color.argb(20, 255, 0, 0) // 원의 색상 설정
        }
        else {
            circleOverlay.color = Color.argb(20, 0, 255, 0) // 원의 색상 설정
        }

        circleOverlay.map = naverMap

        circleOverlays.add(circleOverlay)
    }

    private fun addCircleOverlayForMerge(visitData: VisitData){
        val circleOverlay = CircleOverlay()
        circleOverlay.center = LatLng(visitData.lat, visitData.lng)
        circleOverlay.radius = 100.0 // 반지름을 50m로 설정

        if(visitData.name.isEmpty()) {
            circleOverlay.color = Color.argb(20, 255, 255, 0) // 원의 색상 설정
        }
        else {
            circleOverlay.color = Color.argb(20, 255, 255, 0) // 원의 색상 설정
        }

        circleOverlay.map = naverMap

        circleOverlays.add(circleOverlay)
    }

    private fun deleteOverlay() {
        pathOverlays.forEach{ it.map = null}
        pathOverlays.clear()

        visitMarkers.forEach { it.map = null }
        visitMarkers.clear()

        circleOverlays.forEach{ it.map = null }
        circleOverlays.clear()
    }



    private fun getPreviousDate(selectedDate: String): String {
        val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateObj = selectedDateFormat.parse(selectedDate)
        val calendar = Calendar.getInstance()
        calendar.time = selectedDateObj!!
        calendar.add(Calendar.DAY_OF_MONTH, -1)


        return selectedDateFormat.format(calendar.time)
    }


    /*Camera 관련 함수*/
    /*____________________________________________________________________________________________*/
    private fun moveCameraForPath(pathPoints: MutableList<LocationData>){
        if(pathPoints.isNotEmpty()) {
            val minLat = pathPoints.minOf { it.lat }
            val maxLat = pathPoints.maxOf { it.lat }
            val minLng = pathPoints.minOf { it.lng }
            val maxLng = pathPoints.maxOf { it.lng }

            val bounds = LatLngBounds(LatLng(minLat, minLng), LatLng(maxLat, maxLng))
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 50, 150, 50, 50))

        }
    }

    private fun uploadImage(){
        //TODO
    }

    private fun uploadPost(writeData: VectoService.PostData) {
        val vectoService = VectoService.create()

        val call = vectoService.addPost("Bearer ${Auth.token}", writeData)
        call.enqueue(object : Callback<String>{
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response.isSuccessful){
                    Log.d("UPLOAD", "성공: ${response.body()}")
                    response.body()
                }
                else{
                    Log.d("UPLOAD", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("UPLOAD", "실패")
            }

        })
    }

    private fun uploadImageToServer(writeData: VectoService.PostData) {
        val imageParts: MutableList<MultipartBody.Part> = mutableListOf()

        if (imageUri.isNotEmpty()) {
            for (uri in imageUri) {
                val file = File(uri.path!!)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("images", file.name, requestFile)
                imageParts.add(imagePart)
            }

            val vectoService = VectoService.create()
            val call = vectoService.uploadImages("Bearer ${Auth.token}", imageParts)
            call.enqueue(object : Callback<List<String>> {
                override fun onResponse(
                    call: Call<List<String>>,
                    response: Response<List<String>>
                ) {
                    if (response.isSuccessful) {
                        Log.d("UPLOAD_IMAGE", "성공: ${response.body()}")
                        val imageUrls = response.body()

                        /*imageUrls?.let {
                            for (url in it) {

                            }
                        }*/

                        uploadPost(
                            VectoService.PostData(
                                writeData.title,
                                writeData.content,
                                writeData.uploadtime,
                                imageUrls?.toMutableList(),
                                writeData.location,
                                writeData.visit
                            )
                        )
                    } else {
                        Log.d("UPLOAD_IMAGE", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<List<String>>, t: Throwable) {
                    // 네트워크 오류 또는 기타 문제가 발생했을 때의 처리 코드를 여기에 작성하세요.
                    Log.d("UPLOAD_IMAGE", "실패")
                }
            })
        }
        else
        {
            uploadPost(writeData)
        }
    }
}