package com.example.vecto.ui_bottom

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vecto.MainActivity
import com.example.vecto.R
import com.example.vecto.data.Auth
import com.example.vecto.data.LocationData
import com.example.vecto.data.LocationDatabase
import com.example.vecto.data.VisitData
import com.example.vecto.data.VisitDatabase
import com.example.vecto.databinding.FragmentWriteBinding
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Locale

class WriteFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentWriteBinding
    private lateinit var mapImage: ImageView
    private lateinit var mapFragmentContainerView: FragmentContainerView

    private var mapSnapshot = mutableListOf<Bitmap>()

    private lateinit var myimageAdapter: MyimageAdapter

    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    lateinit var cropResultLauncher: ActivityResultLauncher<Intent>
    lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>
    private lateinit var selectedVisitData: VisitData
    private lateinit var selectedPathData: MutableList<LocationData>

    //overlay 관련
    private val visitMarkers = mutableListOf<Marker>()
    private val pathOverlays = mutableListOf<PathOverlay>()
    private val circleOverlays = mutableListOf<CircleOverlay>()

    private var imageCnt = 0

    private var imageUri = mutableListOf<Uri>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWriteBinding.inflate(inflater, container, false)

        mapImage = binding.MapImage
        mapFragmentContainerView = binding.naverMapWrite

        myimageAdapter = MyimageAdapter(requireContext())
        myimageAdapter.setOnItemRemovedListener(object : MyimageAdapter.OnItemRemovedListener {
            override fun onItemRemoved() {
                binding.PhotoIconText.text = "${myimageAdapter.itemCount}/10"

                imageUri = myimageAdapter.imageUri
            }
        })
        val writeRecyclerView = binding.WriteRecyclerView
        writeRecyclerView.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelSize(R.dimen.image_margin)))
        writeRecyclerView.adapter = myimageAdapter
        writeRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.PhotoBoxImage.setOnClickListener {
            openGallery()
        }

        binding.LocationBoxImage.setOnClickListener {
            getCourse()
        }

        binding.WriteDoneButton.setOnClickListener {
            //TODO 형식 확인. 이름, 경로

            binding.progressBar.visibility = View.VISIBLE
            binding.constraintProgress.visibility = View.VISIBLE
            takeSnapForMap(VectoService.PostData(binding.EditTitle.text.toString(), binding.EditContent.text.toString(), LocalDateTime.now().withNano(0).toString(), null, locationDataList, visitDataList, null))
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
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryResultLauncher.launch(intent) // 갤러리 결과를 galleryResultLauncher로 받음
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

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImages = result.data?.clipData

                val totalImage =
                    if (selectedImages != null)
                        myimageAdapter.itemCount + selectedImages.itemCount
                    else
                        myimageAdapter.itemCount + 1

                if (totalImage > 10) {
                    Toast.makeText(requireContext(), "최대 10개의 이미지만 선택 가능합니다.", Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }


                if (selectedImages != null) {
                    for (i in 0 until selectedImages.itemCount) {
                        val selectedImageUri = selectedImages.getItemAt(selectedImages.itemCount - 1 - i).uri
                        startCrop(selectedImageUri) // 각 이미지를 UCrop으로 전달
                    }
                } else if (result.data?.data != null) {
                    val imageUri = result.data?.data
                    startCrop(imageUri!!) // 이미지를 UCrop으로 전달
                }
            }
        }

        cropResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {


                val resultUri = UCrop.getOutput(result.data!!)
                addImage(resultUri!!)
                myimageAdapter.notifyDataSetChanged()
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("UCrop", "Crop error: $cropError")
            }
        }
    }


    private fun addImage(newImageUri: Uri) {
        if (myimageAdapter.itemCount >= 10) {
            Toast.makeText(requireContext(), "최대 10개의 이미지만 추가 가능합니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 이미지 압축
        val compressedBytes = compressImage(newImageUri)
        val compressedUri = saveCompressedImage(compressedBytes)

        imageUri.add(compressedUri)
        myimageAdapter.imageUri.add(compressedUri)


        // 가장 앞쪽의 ImageView에 새 이미지를 셋팅
        binding.PhotoIconText.text = "${myimageAdapter.itemCount}/10"
    }


    private fun saveCompressedImage(compressedBytes: ByteArray): Uri {
        val filename = "compressed_${System.currentTimeMillis()}.jpeg"
        val file = File(requireContext().cacheDir, filename)
        file.outputStream().use { it.write(compressedBytes) }
        return Uri.fromFile(file)
    }

    private fun compressImage(uri: Uri): ByteArray {
        val outStream = ByteArrayOutputStream()
        val inputStream = requireContext().contentResolver.openInputStream(uri)

        if (inputStream == null) {
            Log.e("ImageError", "Failed to open InputStream for the provided Uri: $uri")
            return byteArrayOf()  // empty array
        }

        val original = BitmapFactory.decodeStream(inputStream)
        original?.compress(Bitmap.CompressFormat.JPEG, 50, outStream)

        return outStream.toByteArray()
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

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
        naverMap.uiSettings.isZoomControlEnabled = false
        naverMap.uiSettings.isScrollGesturesEnabled = false
        naverMap.uiSettings.isScaleBarEnabled = false
        naverMap.uiSettings.isLogoClickEnabled = false
        naverMap.uiSettings.isTiltGesturesEnabled = false
        naverMap.uiSettings.isZoomGesturesEnabled = false

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
                mapFragmentContainerView.visibility = View.VISIBLE
                deleteOverlay()

                //선택한 날짜의 방문지의 처음과 끝까지의 경로
                locationDataList = LocationDatabase(requireContext()).getBetweenLocationData(visitDataList.first().datetime, visitDataList.last().datetime)

                addPathOverlayForLoacation(locationDataList)

                if(visitDataList.size == 1)
                    moveCameraForVisit(visitDataList[0])
                else
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

    private fun deleteOverlay() {
        pathOverlays.forEach{ it.map = null}
        pathOverlays.clear()

        visitMarkers.forEach { it.map = null }
        visitMarkers.clear()
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
        call.enqueue(object : Callback<VectoService.VectoResponse<Int>>{
            override fun onResponse(call: Call<VectoService.VectoResponse<Int>>, response: Response<VectoService.VectoResponse<Int>>) {
                if(response.isSuccessful){
                    Log.d("UPLOAD", "성공: ${response.body()}")
                    response.body()
                    binding.progressBar.visibility = View.GONE
                    binding.constraintProgress.visibility = View.GONE
                    binding.EditContent.setText("")
                    binding.EditTitle.setText("")
                    (activity as? MainActivity)?.binding?.navView?.selectedItemId = R.id.SearchFragment

                }
                else{
                    Log.d("UPLOAD", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    Log.d("UPLOAD", "MAP Image Size :${writeData.mapimage?.size}}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<Int>>, t: Throwable) {
                Log.d("UPLOAD", "실패 ${t.message.toString()}" )
            }

        })
    }

    private fun uploadImageToServer(writeData: VectoService.PostData) {
        val imageParts: MutableList<MultipartBody.Part> = mutableListOf()

        if (imageUri.isNotEmpty()) {//업로드 할 이미지가 있으면
            for (uri in imageUri) {
                val file = File(uri.path!!)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                imageParts.add(imagePart)
            }

            val vectoService = VectoService.create()
            val call = vectoService.uploadImages("Bearer ${Auth.token}", imageParts)
            call.enqueue(object : Callback<VectoService.VectoResponse<VectoService.ImageResponse>> {
                override fun onResponse(
                    call: Call<VectoService.VectoResponse<VectoService.ImageResponse>>,
                    response: Response<VectoService.VectoResponse<VectoService.ImageResponse>>
                ) {
                    if (response.isSuccessful) {
                        Log.d("UPLOAD_IMAGE", "성공: ${response.body()}")
                        val imageUrls = response.body()

                        uploadPost(writeData.copy(image = imageUrls?.result?.url?.toMutableList()))
                    } else {
                        Log.d("UPLOAD_IMAGE", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<VectoService.VectoResponse<VectoService.ImageResponse>>, t: Throwable) {
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

    private fun changelayout(w: Int, h: Int){
        val params = mapFragmentContainerView.layoutParams

        val density = resources.displayMetrics.density
        val width = (w * density)
        val height = (h * density)

        params.width = width.toInt() // 원하는 가로길이
        params.height = height.toInt() // 원하는 세로 길이

        mapFragmentContainerView.layoutParams = params
    }

    private fun takeSnapForMap(writeData: VectoService.PostData){
        mapImage.visibility = View.INVISIBLE
        mapFragmentContainerView.visibility = View.VISIBLE

        mapSnapshot.clear()


        changelayout(340, 340)
        if(visitDataList.size == 1)
            moveCameraForVisit(visitDataList[0])
        else
            moveCameraForPath(locationDataList)

        Handler(Looper.getMainLooper()).postDelayed({
            naverMap.takeSnapshot {
                mapSnapshot.add(it)
            }
        }, 900)

        Handler(Looper.getMainLooper()).postDelayed({
            changelayout(340, 170)
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            if(visitDataList.size == 1)
                moveCameraForVisit(visitDataList[0])
            else
                moveCameraForPath(locationDataList)
        }, 1100)

        Handler(Looper.getMainLooper()).postDelayed({
            naverMap.takeSnapshot {
                mapSnapshot.add(it)
            }
        }, 2000)

        Handler(Looper.getMainLooper()).postDelayed({
            mapImage.visibility = View.INVISIBLE
            mapFragmentContainerView.visibility = View.INVISIBLE

            uploadMapImage(writeData)
        }, 2200)


    }

    private fun uploadMapImage(writeData: VectoService.PostData) {
        val vectoService = VectoService.create()

        val imageParts = mutableListOf<MultipartBody.Part>()
        for (bitmap in mapSnapshot) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val body = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0)

            val part = MultipartBody.Part.createFormData("image", "mapImage.jpg", body)
            imageParts.add(part)
        }

        val call = vectoService.uploadImages("Bearer ${Auth.token}",imageParts)
        call.enqueue(object : Callback<VectoService.VectoResponse<VectoService.ImageResponse>>{
            override fun onResponse(call: Call<VectoService.VectoResponse<VectoService.ImageResponse>>, response: Response<VectoService.VectoResponse<VectoService.ImageResponse>>) {
                if(response.isSuccessful){
                    Log.d("UPLOADMAP", "성공: ${response.body()}")

                    writeData.mapimage = mutableListOf()
                    response.body()?.result?.url?.let {
                        writeData.mapimage?.addAll(it)
                    }
                    Log.d("UPLOADMAP","SAVE MAP URL : ${writeData.mapimage?.size}")
                    uploadImageToServer(writeData)
                }
                else{
                    Log.d("UPLOADMAP", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<VectoService.ImageResponse>>, t: Throwable) {
                Log.d("UPLOADMAP", "실패 ${t.message.toString()}" )
            }

        })
    }

    private fun moveCameraForVisit(visit: VisitData){
        val targetLatLng = LatLng(visit.lat_set, visit.lng_set)

        naverMap.moveCamera(CameraUpdate.scrollTo(targetLatLng))
        naverMap.moveCamera(CameraUpdate.zoomTo(18.0))
    }
}