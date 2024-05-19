package com.vecto_example.vecto.ui.editfeed

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.vecto_example.vecto.ui.main.MainActivity
import com.vecto_example.vecto.ui.editfeed.adapter.MyEditImageAdapter
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.LocationDatabase
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDatabase
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.databinding.ActivityEditFeedBinding
import com.vecto_example.vecto.dialog.CalendarDialog
import com.vecto_example.vecto.dialog.WriteBottomDialog
import com.vecto_example.vecto.dialog.WriteNameEmptyDialog
import com.vecto_example.vecto.retrofit.NaverSearchApiService
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.decoration.SpacesItemDecoration
import com.vecto_example.vecto.ui.write.WriteRepository
import com.vecto_example.vecto.ui.write.WriteViewModel
import com.vecto_example.vecto.ui.write.WriteViewModelFactory
import com.vecto_example.vecto.utils.DateTimeUtils
import com.vecto_example.vecto.utils.MapMarkerManager
import com.vecto_example.vecto.utils.MapOverlayManager
import com.vecto_example.vecto.utils.RequestLoginUtils
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ToastMessageUtils
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

class EditFeedActivity : AppCompatActivity(), OnMapReadyCallback, CalendarDialog.OnDateSelectedListener {
    lateinit var binding: ActivityEditFeedBinding

    private val writeViewModel: WriteViewModel by viewModels {
        WriteViewModelFactory(WriteRepository(VectoService.create(), NaverSearchApiService.create()), TokenRepository(VectoService.create()))
    }

    private lateinit var myEditImageAdapter: MyEditImageAdapter

    private lateinit var mapView: MapFragment
    private lateinit var naverMap: NaverMap

    //Overlay, Marker 관리
    private lateinit var mapMarkerManager: MapMarkerManager
    private lateinit var mapOverlayManager: MapOverlayManager

    //gallery, crop 결과
    private lateinit var cropResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryResultLauncher: ActivityResultLauncher<Intent>

    private var currentImageIndex = 0 // 현재 크롭 중 인덱스
    private var selectedImageUris = mutableListOf<Uri>() // 선택된 이미지 URI 목록

    private var mapSnapshot = mutableListOf<Bitmap>()
    private var imageUrl = mutableListOf<String>()

    private var uploadStarted = false

    private var title = ""
    private var content = ""
    private var feedId = -1

    private var originalWidth = 0
    private var originalHeight = 0

    private var backPressedTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        originalWidth = binding.naverMapEditPost.layoutParams.width
        originalHeight = binding.naverMapEditPost.layoutParams.height

        val typeofFeedInfo = object : TypeToken<VectoService.FeedInfo>() {}.type
        val feedInfo = Gson().fromJson<VectoService.FeedInfo>(intent.getStringExtra("feedInfoJson"), typeofFeedInfo)
        writeViewModel.locationDataList.addAll(feedInfo.location.toMutableList())
        writeViewModel.visitDataList.addAll(feedInfo.visit.toMutableList())
        imageUrl = feedInfo.image.toMutableList()
        title = feedInfo.title
        content = feedInfo.content

        writeViewModel.reverseGeocode()

        feedId = intent.getIntExtra("feedId", -1)

        if(feedId != -1) {
            initUI()

            initRecyclerView(feedInfo)
            initListeners()
            initObservers()

            setGalleryResult()
            setCropResult()

        } else {
            ToastMessageUtils.showToast(this, getString(R.string.get_feed_fail))
        }

        // OnBackPressedCallback 등록
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backPressedTime <= 2000) {
                    finish()
                } else {
                    backPressedTime = System.currentTimeMillis()
                    ToastMessageUtils.showToast(this@EditFeedActivity, getString(R.string.back_pressed_edit))
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun initObservers() {
        //로딩
        writeViewModel.isLoading.observe(this){
            if(it){
                binding.progressBar.visibility = View.VISIBLE
                binding.constraintProgress.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.constraintProgress.visibility = View.GONE
            }
        }

        writeViewModel.isCourseDataLoaded.observe(this){
            if(it){ //데이터가 불러와져 있으면
                binding.DeleteButton.visibility = View.VISIBLE
                binding.naverMapEditPost.visibility = View.VISIBLE

                initMap()

                binding.LocationBoxImage.isClickable = false
            } else {
                binding.DeleteButton.visibility = View.GONE
                binding.naverMapEditPost.visibility = View.INVISIBLE

                binding.LocationBoxImage.isClickable = true

                writeViewModel.visitDataList.clear()
                writeViewModel.locationDataList.clear()

                mapOverlayManager.deleteOverlay()
            }
        }

        writeViewModel.mapImageUrls.observe(this){
            val allImageUrl: List<String> = imageUrl + (writeViewModel.imageUrls.value ?: emptyList())

            if(myEditImageAdapter.imageUri.isEmpty() && uploadStarted){   //업로드 할 Normal Image 가 없는 경우
                Log.d("EditPost", "업로드 할 이미지가 없고 지도 이미지 업로드가 완료되었습니다.")

                uploadData(allImageUrl)
            } else if(writeViewModel.normalImageDone.value == true && uploadStarted) { //업로드 할 Normal Image 가 이미 완료된 경우
                Log.d("EditPost", "업로드 할 이미지가 완료되었고 지도 이미지 업로드가 완료되었습니다.")

                uploadData(allImageUrl)
            }
        }

        writeViewModel.imageUrls.observe(this){
            val allImageUrl: List<String> = imageUrl + (writeViewModel.imageUrls.value ?: emptyList())

            if(writeViewModel.mapImageDone.value == true && uploadStarted){  //Normal Image 와 지도 이미지 모두 완료된 경우
                Log.d("EditPost", "업로드 할 이미지가 있고 지도 이미지 업로드는 완료되었습니다.")

                uploadData(allImageUrl)
            }

        }

        writeViewModel.updateFeedResult.observe(this){
            if(it == "SUCCESS"){
                ToastMessageUtils.showToast(this, getString(R.string.update_feed_success))

                finish()
            }
        }

        writeViewModel.reissueResponse.observe(this){
            when(it){
                WriteViewModel.Function.UploadMapImages.name -> {
                    writeViewModel.uploadImages(WriteViewModel.ImageType.MAP.name, writeViewModel.mapImagePart)
                }
                WriteViewModel.Function.UploadNormalImages.name -> {
                    writeViewModel.uploadImages(WriteViewModel.ImageType.NORMAL.name, writeViewModel.normalImagePart)
                }
                WriteViewModel.Function.UpdateFeed.name -> {
                    writeViewModel.updateFeed(writeViewModel.updateFeedRequest)
                }
            }
        }

        writeViewModel.errorMessage.observe(this){
            ToastMessageUtils.showToast(this, getString(it))

            if(it == R.string.expired_login) {
                SaveLoginDataUtils.deleteData(this)
                finish()
            }

            uploadStarted = false
            writeViewModel.failUpload()
            changeMapSize(originalWidth, originalHeight)
        }

    }

    private fun uploadData(allImageUrl: List<String>) {
        writeViewModel.updateFeed(
            VectoService.UpdateFeedRequest(
                feedId,
                binding.EditTitle.text.toString(),
                binding.EditContent.text.toString(),
                allImageUrl.toMutableList(),
                writeViewModel.locationDataList,
                writeViewModel.visitDataForWriteList,
                writeViewModel.mapImageUrls.value?.toMutableList()
            ))
    }

    private fun initUI() {
        binding.EditTitle.setText(title)
        binding.EditContent.setText(content)

        binding.PhotoIconText.text = "${imageUrl.size}/10"
    }

    private fun initListeners() {
        binding.LocationBoxImage.setOnClickListener {
            if(writeViewModel.isCourseDataLoaded.value == false) {
                initMap()
            }
        }

        binding.DeleteButton.setOnClickListener {
            writeViewModel.deleteCourseData()
        }

        binding.WriteDoneButton.setOnClickListener {
            if(Auth.loginFlag.value == false) {
                RequestLoginUtils.requestLogin(this)
                return@setOnClickListener
            }

            if(!writeViewModel.isAllVisitDataValid()){  //방문지 유효성 검사
                ToastMessageUtils.showToast(this, getString(R.string.feed_visit_error))

                return@setOnClickListener
            }

            if (binding.EditTitle.text.isEmpty())
                ToastMessageUtils.showToast(this, getString(R.string.feed_title_empty))
            else if (writeViewModel.visitDataList.size == 0 || writeViewModel.locationDataList.size == 0)
                ToastMessageUtils.showToast(this, getString(R.string.feed_visit_empty))
            else {
                writeViewModel.startLoading()

                uploadStarted = true
                takeSnapForMap()
            }

        }

        binding.PhotoBoxImage.setOnClickListener {
            openGallery()
        }


        binding.BackButton.setOnClickListener {
            finish()
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun initRecyclerView(feedInfo: VectoService.FeedInfo) {
        myEditImageAdapter = MyEditImageAdapter(this)
        myEditImageAdapter.imageUrl.addAll(feedInfo.image)
        myEditImageAdapter.notifyDataSetChanged()
        myEditImageAdapter.setOnItemRemovedListener(object :
            MyEditImageAdapter.OnItemRemovedListener {
            override fun onItemRemoved() {
                binding.PhotoIconText.text = "${myEditImageAdapter.itemCount}/10"

                imageUrl = myEditImageAdapter.imageUrl
            }
        })

        val writeRecyclerView = binding.EditPostRecyclerView
        writeRecyclerView.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelSize(R.dimen.image_margin)))
        writeRecyclerView.adapter = myEditImageAdapter
        writeRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun initMap() {
        mapView = supportFragmentManager.findFragmentById(R.id.naver_map_EditPost) as MapFragment?
            ?: MapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().add(R.id.naver_map_EditPost, it).commit()
            }
        mapView.getMapAsync(this)
    }



    private fun showDatePickerDialog(){

        val calendarDialog = CalendarDialog(this)
        calendarDialog.onDateSelectedListener = this
        calendarDialog.showDialog()
    }

    private fun selectVisit(selectedDate: String) = //특정 날짜에 해당하는 방문지를 선택하는 함수
        if(writeViewModel.visitDataList.any{ it.name.isEmpty() })//이름항목이 하나라도 비어있을 경우
        {
            val writeNameEmptyDialog = WriteNameEmptyDialog(this)
            writeNameEmptyDialog.showDialog()
            writeNameEmptyDialog.onOkButtonClickListener = {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("editCourse", selectedDate)
                this.startActivity(intent)
            }

            writeViewModel.visitDataList.clear()
        }
        else//완성된 경우
        {
            val writeBottomDialog = WriteBottomDialog(this)
            writeBottomDialog.showDialog(writeViewModel.visitDataList) { selectedItems -> //구간을 선택함 (모든 정보 완료)

                if(selectedItems.size > 9){
                    ToastMessageUtils.showToast(this, getString(R.string.feed_visit_max))
                    writeViewModel.visitDataList.clear()
                    return@showDialog
                }

                //선택한 날짜의 방문지의 처음과 끝까지의 경로
                writeViewModel.locationDataList.addAll(LocationDatabase(this).getBetweenLocationData(selectedItems.first().datetime, selectedItems.last().datetime))

                val locationDataForPath = mutableListOf<LocationData>()

                //location 첫 좌표 넣어줌.
                locationDataForPath.add(LocationData(selectedItems[0].datetime, selectedItems[0].lat_set, selectedItems[0].lng_set))

                writeViewModel.visitDataList.clear()
                writeViewModel.visitDataList.addAll(selectedItems.toMutableList())

                writeViewModel.reverseGeocode()
            }
        }


    private fun changeMapSize(w: Int, h: Int){
        val params = binding.naverMapEditPost.layoutParams

        val density = resources.displayMetrics.density
        val width = (w * density)
        val height = (h * density)

        params.width = width.toInt() // 원하는 가로길이
        params.height = height.toInt() // 원하는 세로 길이

        binding.naverMapEditPost.layoutParams = params
    }

    private fun takeSnapForMap(){
        binding.MapImage.visibility = View.INVISIBLE
        binding.naverMapEditPost.visibility = View.VISIBLE

        mapSnapshot.clear()


        changeMapSize(340, 340)
        if(writeViewModel.visitDataList.size == 1)
            mapOverlayManager.moveCameraForVisitUpload(writeViewModel.visitDataList[0])
        else
            mapOverlayManager.moveCameraForPathUpload(writeViewModel.locationDataList)

        Handler(Looper.getMainLooper()).postDelayed({
            naverMap.takeSnapshot {
                mapSnapshot.add(it)
            }
        }, 900)

        Handler(Looper.getMainLooper()).postDelayed({
            changeMapSize(340, 170)
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            if(writeViewModel.visitDataList.size == 1)
                mapOverlayManager.moveCameraForVisitUpload(writeViewModel.visitDataList[0])
            else
                mapOverlayManager.moveCameraForPathUpload(writeViewModel.locationDataList)
        }, 1100)

        Handler(Looper.getMainLooper()).postDelayed({
            naverMap.takeSnapshot {
                mapSnapshot.add(it)
            }
        }, 2000)

        Handler(Looper.getMainLooper()).postDelayed({
            binding.MapImage.visibility = View.INVISIBLE
            binding.naverMapEditPost.visibility = View.INVISIBLE

            val mapImageParts = mutableListOf<MultipartBody.Part>()

            for (bitmap in mapSnapshot) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val body = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0)

                val part = MultipartBody.Part.createFormData("image", "mapImage.jpg", body)
                mapImageParts.add(part)
            }

            writeViewModel.uploadImages("MAP", mapImageParts)

            val imageParts: MutableList<MultipartBody.Part> = mutableListOf()

            if (myEditImageAdapter.imageUri.isNotEmpty()) {//업로드 할 이미지가 있으면
                for (uri in myEditImageAdapter.imageUri) {
                    val file = File(uri.path!!)
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    imageParts.add(imagePart)
                }

                writeViewModel.uploadImages("NORMAL", imageParts)
            }

        }, 2200)

    }

    override fun onDateSelected(date: String) {
        val previousDate = DateTimeUtils.getPreviousDate(date)

        val filteredData = VisitDatabase(this).getAllVisitData().filter { visitData ->
            val visitDate = visitData.datetime.substring(0, 10)
            val endDate = visitData.endtime.substring(0, 10)
            visitDate == previousDate && endDate == date
        }

        writeViewModel.visitDataList.clear()
        writeViewModel.visitDataList.addAll(VisitDatabase(this).getAllVisitData().filter {
            it.datetime.startsWith(date)
        }.toMutableList())

        //종료 시간이 선택 날짜인 방문지 추가
        if(filteredData.isNotEmpty())
            writeViewModel.visitDataList.add(0, filteredData[0])

        if(writeViewModel.visitDataList.isNotEmpty()){
            //방문 장소가 있을 경우

            selectVisit(date)
        }
        else{
            //방문 장소 없을 경우
            mapOverlayManager.deleteOverlay()
        }
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.uiSettings.isZoomControlEnabled = false
        naverMap.uiSettings.isScrollGesturesEnabled = false
        naverMap.uiSettings.isScaleBarEnabled = false
        naverMap.uiSettings.isLogoClickEnabled = false
        naverMap.uiSettings.isTiltGesturesEnabled = false
        naverMap.uiSettings.isZoomGesturesEnabled = false

        mapMarkerManager = MapMarkerManager(this, naverMap)
        mapOverlayManager = MapOverlayManager(this, mapMarkerManager, naverMap)

        if(writeViewModel.visitDataList.isNotEmpty()){
            mapOverlayManager.deleteOverlay()

            mapOverlayManager.addPathOverlayForLocation(writeViewModel.locationDataList)
            for (visitdatalist in writeViewModel.visitDataList){
                mapMarkerManager.addVisitMarkerBasic(visitdatalist)
            }

            if(writeViewModel.visitDataList.size == 1)
                mapOverlayManager.moveCameraForVisitUpload(writeViewModel.visitDataList[0])
            else
                mapOverlayManager.moveCameraForPathUpload(writeViewModel.locationDataList)
        } else {
            showDatePickerDialog()
        }
    }

    /*   이미지 관련   */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryResultLauncher.launch(intent) // 갤러리 결과를 galleryResultLauncher로 받음
    }

    private fun setGalleryResult() {
        galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUris.clear()
                currentImageIndex = 0 // 인덱스 초기화

                if (myEditImageAdapter.itemCount + result.data?.clipData!!.itemCount > 10) {
                    ToastMessageUtils.showToast(this, getString(R.string.feed_image_max))
                    return@registerForActivityResult
                }

                val clipData = result.data?.clipData
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        selectedImageUris.add(clipData.getItemAt(i).uri)
                    }
                } else {
                    result.data?.data?.let { selectedImageUris.add(it) }
                }
                startNextCrop() // 첫 번째 이미지 크롭 시작
            }
        }
    }

    private fun startNextCrop() {
        if (currentImageIndex < selectedImageUris.size) {
            startCrop(selectedImageUris[currentImageIndex]) // 현재 인덱스의 이미지를 크롭
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

    private fun setCropResult() {
        cropResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                addImage(resultUri!!)
                myEditImageAdapter.notifyItemInserted(currentImageIndex)
                currentImageIndex++ // 다음 이미지를 위해 인덱스 증가
                startNextCrop() // 다음 이미지 크롭 시작
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("UCrop", "Crop error: $cropError")
            }
        }
    }

    private fun addImage(newImageUri: Uri) {
        Log.d("uCrop", "addImage")

        // 이미지 압축
        val compressedBytes = compressImage(newImageUri)
        saveCompressedImage(compressedBytes)


        // 가장 앞쪽의 ImageView에 새 이미지를 셋팅
        binding.PhotoIconText.text = "${myEditImageAdapter.itemCount}/10"
    }

    private fun compressImage(uri: Uri): ByteArray {
        val outStream = ByteArrayOutputStream()
        val inputStream = this.contentResolver.openInputStream(uri)

        if (inputStream == null) {
            Log.e("ImageError", "Failed to open InputStream for the provided Uri: $uri")
            return byteArrayOf()  // empty array
        }

        inputStream.use { stream ->
            val original = BitmapFactory.decodeStream(stream)

            if(original == null){
                Log.e("EditPostActivity", "Failed to decode the image from Uri: $uri")
                return byteArrayOf()
            }

            val resizedImage = Bitmap.createScaledBitmap(original, 1080, 1080, true)

            resizedImage.compress(Bitmap.CompressFormat.JPEG, 80, outStream)

        }

        return outStream.toByteArray()
    }

    private fun saveCompressedImage(compressedBytes: ByteArray) {
        val filename = "compressed_${System.currentTimeMillis()}.jpeg"
        val file = File(this.cacheDir, filename)
        file.outputStream().use { it.write(compressedBytes) }

        Log.d("uCrop", "saveCompressedImage")

        myEditImageAdapter.imageUri.add(Uri.fromFile(file))
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {

        currentFocus?.let { view ->
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    view.clearFocus()
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}