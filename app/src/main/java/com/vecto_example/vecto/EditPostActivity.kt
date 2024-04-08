package com.vecto_example.vecto

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.LocationDatabase
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDataForWite
import com.vecto_example.vecto.data.model.VisitDatabase
import com.vecto_example.vecto.databinding.ActivityEditPostBinding
import com.vecto_example.vecto.dialog.CalendarDialog
import com.vecto_example.vecto.dialog.LoginRequestDialog
import com.vecto_example.vecto.dialog.WriteBottomDialog
import com.vecto_example.vecto.dialog.WriteNameEmptyDialog
import com.vecto_example.vecto.retrofit.NaverSearchApiService
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.login.LoginActivity
import com.vecto_example.vecto.ui_bottom.SpacesItemDecoration
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
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Locale

class EditPostActivity : AppCompatActivity(), OnMapReadyCallback,
    CalendarDialog.OnDateSelectedListener {
    lateinit var binding: ActivityEditPostBinding
    private lateinit var mapImage: ImageView
    private lateinit var mapFragmentContainerView: FragmentContainerView

    private var mapSnapshot = mutableListOf<Bitmap>()

    private lateinit var myeditimageAdapter: MyeditimageAdapter

    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    lateinit var cropResultLauncher: ActivityResultLauncher<Intent>
    lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>

    lateinit var visitDataForWriteList: MutableList<VisitDataForWite>

    //overlay 관련
    private val visitMarkers = mutableListOf<Marker>()
    private val pathOverlays = mutableListOf<PathOverlay>()

    private var imageUri = mutableListOf<Uri>()
    private var imageUrl = mutableListOf<String>()


    lateinit var address: MutableList<String>

    private var title = ""
    private var content = ""

    private var feedId = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val typeofFeedInfo = object : TypeToken<VectoService.FeedInfoResponse>() {}.type
        val feedInfo = Gson().fromJson<VectoService.FeedInfoResponse>(intent.getStringExtra("feedInfoJson"), typeofFeedInfo)
        locationDataList = feedInfo.location.toMutableList()
        visitDataList = feedInfo.visit.toMutableList()
        imageUrl = feedInfo.image.toMutableList()
        title = feedInfo.title
        content = feedInfo.content
        imageUrl = feedInfo.image.toMutableList()

        feedId = intent.getIntExtra("feedId", -1)

        mapImage = binding.MapImage

        binding.LocationBoxImage.setOnClickListener {
            initMap()
            showDatePickerDialog()
        }


        binding.EditTitle.setText(title)
        binding.EditContent.setText(content)

        mapFragmentContainerView = binding.naverMapEditPost

        mapFragmentContainerView.visibility = View.VISIBLE

        binding.PhotoIconText.text = "${imageUrl.size}/10"


        binding.DeleteButton.visibility = View.VISIBLE
        binding.DeleteButton.setOnClickListener {
            mapFragmentContainerView.visibility = View.INVISIBLE
            binding.DeleteButton.visibility = View.INVISIBLE
            visitDataList.clear()
            locationDataList.clear()

        }
        initMap()

        myeditimageAdapter = MyeditimageAdapter(this)
        myeditimageAdapter.imageUrl.addAll(feedInfo.image)
        myeditimageAdapter.notifyDataSetChanged()
        myeditimageAdapter.setOnItemRemovedListener(object : MyeditimageAdapter.OnItemRemovedListener {
            override fun onItemRemoved() {
                binding.PhotoIconText.text = "${myeditimageAdapter.itemCount}/10"

                imageUri = myeditimageAdapter.imageUri
                imageUrl = myeditimageAdapter.imageUrl
            }
        })

        val writeRecyclerView = binding.EditPostRecyclerView
        writeRecyclerView.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelSize(R.dimen.image_margin)))
        writeRecyclerView.adapter = myeditimageAdapter
        writeRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)


        binding.WriteDoneButton.setOnClickListener {
            if(Auth.loginFlag.value == false)
            {
                val loginRequestDialog = LoginRequestDialog(this)
                loginRequestDialog.showDialog()
                loginRequestDialog.onOkButtonClickListener = {
                    val intent = Intent(this, LoginActivity::class.java)
                    this.startActivity(intent)
                }

                return@setOnClickListener
            }

            if (binding.EditTitle.text.isEmpty())
                Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            else if (visitDataList.size == 0 || locationDataList.size == 0)
                Toast.makeText(this, "경로를 선택해주세요.", Toast.LENGTH_SHORT).show()
            else {
                binding.progressBar.visibility = View.VISIBLE
                binding.constraintProgress.visibility = View.VISIBLE
                takeSnapForMap(
                    VectoService.PostData(
                        binding.EditTitle.text.toString(),
                        binding.EditContent.text.toString(),
                        LocalDateTime.now().withNano(0).toString(),
                        null,
                        locationDataList,
                        visitDataList,
                        null
                    )
                )
            }

        }

        binding.PhotoBoxImage.setOnClickListener {
            openGallery()
        }

        galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImages = result.data?.clipData

                val totalImage =
                    if (selectedImages != null)
                        myeditimageAdapter.itemCount + selectedImages.itemCount
                    else
                        myeditimageAdapter.itemCount + 1

                if (totalImage > 10) {
                    Toast.makeText(this, "최대 10개의 이미지만 선택 가능합니다.", Toast.LENGTH_LONG).show()
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
                myeditimageAdapter.notifyDataSetChanged()
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("UCrop", "Crop error: $cropError")
            }
        }

        binding.BackButton.setOnClickListener {
            finish()
        }

    }

    private fun initMap() {
        mapView = supportFragmentManager.findFragmentById(R.id.naver_map_EditPost) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.naver_map_EditPost, it).commit()
            }
        mapView.getMapAsync(this)
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

        if(locationDataList.isNotEmpty() && visitDataList.isNotEmpty()) {//원본 경로 설정
            addPathOverlayForLoacation(locationDataList)

            if (visitDataList.size == 1)
                moveCameraForVisit(visitDataList[0])
            else
                moveCameraForPath(locationDataList)
            val locationDataforPath = mutableListOf<LocationData>()

            locationDataforPath.add(
                LocationData(
                    visitDataList[0].datetime,
                    visitDataList[0].lat_set,
                    visitDataList[0].lng_set
                )
            )

            for (visitdatalist in visitDataList) {
                addVisitMarker(visitdatalist)
            }
        }
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(this.cacheDir, destinationFileName))
        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(uCropOptions())
            .withAspectRatio(1f, 1f) // 1:1 비율로 자르기
            .getIntent(this)
        cropResultLauncher.launch(uCropIntent)
    }


    private fun addImage(newImageUri: Uri) {
        Log.d("ASDASD2", "ADDIMAGE 호출")

        if (myeditimageAdapter.itemCount > 10) {
            Toast.makeText(this, "최대 10개의 이미지만 추가 가능합니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 이미지 압축
        val compressedBytes = compressImage(newImageUri)
        saveCompressedImage(compressedBytes)


        // 가장 앞쪽의 ImageView에 새 이미지를 셋팅
        binding.PhotoIconText.text = "${myeditimageAdapter.itemCount}/10"
    }


    private fun saveCompressedImage(compressedBytes: ByteArray) {
        val filename = "compressed_${System.currentTimeMillis()}.jpeg"
        val file = File(this.cacheDir, filename)
        file.outputStream().use { it.write(compressedBytes) }

        /*imageUri.add(Uri.fromFile(file))
        Log.d("ASDASD2", "saveCompressedImage, ${myeditimageAdapter.imageUri.size}")*/
        myeditimageAdapter.imageUri.add(Uri.fromFile(file))
        Log.d("ASDASD2", "saveCompressedImage, ${myeditimageAdapter.imageUri.size}")

        imageUri = myeditimageAdapter.imageUri

    }

    private fun compressImage(uri: Uri): ByteArray {
        val outStream = ByteArrayOutputStream()
        val inputStream = this.contentResolver.openInputStream(uri)

        if (inputStream == null) {
            Log.e("ImageError", "Failed to open InputStream for the provided Uri: $uri")
            return byteArrayOf()  // empty array
        }

        val original = BitmapFactory.decodeStream(inputStream)
        original?.compress(Bitmap.CompressFormat.JPEG, 60, outStream)

        return outStream.toByteArray()
    }

    private fun uCropOptions(): UCrop.Options {
        val options = UCrop.Options()
        // 하단 컨트롤 숨기기
        options.setHideBottomControls(true)
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.black))
        options.setToolbarColor(ContextCompat.getColor(this, R.color.black))
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.white))

        // 격자 선 숨기기
        options.setCropGridRowCount(0)
        options.setCropGridColumnCount(0)
        return options
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryResultLauncher.launch(intent) // 갤러리 결과를 galleryResultLauncher로 받음
    }



    private fun showDatePickerDialog(){

        val calendarDialog = CalendarDialog(this)
        calendarDialog.onDateSelectedListener = this
        calendarDialog.showDialog()
    }

    private fun selectVisit(selectedDate: String) = //특정 날짜에 해당하는 방문지를 선택하는 함수
        if(visitDataList.any{ it.name.isEmpty() })//이름항목이 하나라도 비어있을 경우
        {
            val writeNameEmptyDialog = WriteNameEmptyDialog(this)
            writeNameEmptyDialog.showDialog()
            writeNameEmptyDialog.onOkButtonClickListener = {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("editCourse", selectedDate)
                this.startActivity(intent)
            }
        }
        else//완성된 경우
        {
            val writeBottomDialog = WriteBottomDialog(this)
            writeBottomDialog.showDialog(visitDataList) { selectedItems -> //구간을 선택함 (모든 정보 완료)

                mapFragmentContainerView.visibility = View.VISIBLE
                deleteOverlay()

                //선택한 날짜의 방문지의 처음과 끝까지의 경로
                locationDataList = LocationDatabase(this).getBetweenLocationData(selectedItems.first().datetime, selectedItems.last().datetime)

                addPathOverlayForLoacation(locationDataList)

                if(selectedItems.size == 1)
                    moveCameraForVisit(selectedItems[0])
                else
                    moveCameraForPath(locationDataList)
                val locationDataforPath = mutableListOf<LocationData>()

                //location 첫 좌표 넣어줌.
                locationDataforPath.add(LocationData(selectedItems[0].datetime, selectedItems[0].lat_set, selectedItems[0].lng_set))

                for (visitdatalist in selectedItems){
                    addVisitMarker(visitdatalist)
                }

                visitDataList = selectedItems.toMutableList()

                address = MutableList(visitDataList.size) {""}
                visitDataForWriteList = MutableList(visitDataList.size){ VisitDataForWite("", "", 0.0, 0.0, 0.0, 0.0, 0, "", "") }


                for(i in 0 until visitDataList.size)
                {
                    val naverSearchApiService = NaverSearchApiService.create()
                    val call = naverSearchApiService.reverseGeocode("${visitDataList[i].lng_set},${visitDataList[i].lat_set}", "legalcode", "json")
                    call.enqueue(object : Callback<NaverSearchApiService.ReverseGeocodeResponse> {
                        override fun onResponse(call: Call<NaverSearchApiService.ReverseGeocodeResponse>, response: Response<NaverSearchApiService.ReverseGeocodeResponse>) {
                            if (response.isSuccessful) {
                                if(response.body()?.results != null) {

                                    /*XX 시 XX 구 XX 동을 전달받는 API*/

                                    if(response.body()!!.results[0].region?.area1?.name?.isEmpty() == false){
                                        address[i] += response.body()!!.results[0].region?.area1?.name.toString()
                                        if(response.body()!!.results[0].region?.area2?.name?.isEmpty() == false){
                                            address[i] += (" " + response.body()!!.results[0].region?.area2?.name)
                                            if(response.body()!!.results[0].region?.area3?.name?.isEmpty() == false)
                                                address[i] += (" " + response.body()!!.results[0].region?.area3?.name)
                                        }
                                    }


                                    //address 정보가 포함된 VisitData
                                    visitDataForWriteList[i] = VisitDataForWite(visitDataList[i].datetime, visitDataList[i].endtime, visitDataList[i].lat, visitDataList[i].lng, visitDataList[i].lat_set, visitDataList[i].lng_set, visitDataList[i].staytime, visitDataList[i].name, address[i])
                                }

                                Log.d("NAVERAPI", "성공: ${response.body()}")
                            } else {
                                Log.d("NAVERAPI", "성공했으나 서버 오류: ${response.errorBody()?.string()}")
                            }
                        }

                        override fun onFailure(call: Call<NaverSearchApiService.ReverseGeocodeResponse>, t: Throwable) {

                        }

                    })

                }
            }
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

    private fun uploadPost(writeData: VectoService.PostData) {
        if(feedId == -1)
        {
            Toast.makeText(this@EditPostActivity, "올바르지 않은 게시글입니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.constraintProgress.visibility = View.GONE
            return
        }

        val vectoService = VectoService.create()

        val call = vectoService.updatePost("Bearer ${Auth.token}", VectoService.UpdatePostRequest(
            feedId,
            writeData.title,
            writeData.content,
            writeData.image,
            writeData.location,
            visitDataForWriteList,
            writeData.mapimage
        ))

        Log.d("visitDataForWriteList", visitDataForWriteList.size.toString())
        call.enqueue(object : Callback<VectoService.VectoResponse<String>> {
            override fun onResponse(call: Call<VectoService.VectoResponse<String>>, response: Response<VectoService.VectoResponse<String>>) {
                if(response.isSuccessful){
                    Log.d("UPLOAD", "성공: ${response.body()}")
                    response.body()
                    binding.progressBar.visibility = View.GONE
                    binding.constraintProgress.visibility = View.GONE
                    binding.EditContent.setText("")
                    binding.EditTitle.setText("")
                    Toast.makeText(this@EditPostActivity, "수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                else{
                    Log.d("UPLOAD", "성공했으나 서버 오류 ${response.errorBody()?.string()}")
                    Log.d("UPLOAD", "MAP Image Size :${writeData.mapimage?.size}}")
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse<String>>, t: Throwable) {
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

                        imageUrls?.result?.url?.let { imageUrl.addAll(it) }

                        uploadPost(writeData.copy(image = imageUrl))
                    } else {
                        Toast.makeText(this@EditPostActivity, "이미지의 크기를 줄여주세요.", Toast.LENGTH_SHORT).show()
                        Log.d("UPLOAD_IMAGE", "성공했으나 서버 오류 ${response.errorBody()?.string()}")

                        binding.progressBar.visibility = View.GONE
                        binding.constraintProgress.visibility = View.GONE
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
            uploadPost(writeData.copy(image = imageUrl))
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
        call.enqueue(object : Callback<VectoService.VectoResponse<VectoService.ImageResponse>> {
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

    override fun onDateSelected(date: String) {
        val previousDate = getPreviousDate(date)

        val filteredData = VisitDatabase(this).getAllVisitData().filter { visitData ->
            val visitDate = visitData.datetime.substring(0, 10)
            val endDate = visitData.endtime.substring(0, 10)
            visitDate == previousDate && endDate == date
        }

        visitDataList = VisitDatabase(this).getAllVisitData().filter {
            it.datetime.startsWith(date)
        }.toMutableList()

        //종료 시간이 선택 날짜인 방문지 추가
        if(filteredData.isNotEmpty())
            visitDataList.add(0, filteredData[0])

        if(visitDataList.isNotEmpty()){
            //방문 장소가 있을 경우

            selectVisit(date)
        }
        else{
            //방문 장소 없을 경우
            deleteOverlay()
        }
    }
}