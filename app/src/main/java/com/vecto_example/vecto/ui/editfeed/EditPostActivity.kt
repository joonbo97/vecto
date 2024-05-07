package com.vecto_example.vecto.ui.editfeed

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
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
import android.widget.Toast
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
import com.vecto_example.vecto.databinding.ActivityEditPostBinding
import com.vecto_example.vecto.dialog.CalendarDialog
import com.vecto_example.vecto.dialog.LoginRequestDialog
import com.vecto_example.vecto.dialog.WriteBottomDialog
import com.vecto_example.vecto.dialog.WriteNameEmptyDialog
import com.vecto_example.vecto.retrofit.NaverSearchApiService
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.ui.login.LoginActivity
import com.vecto_example.vecto.ui.decoration.SpacesItemDecoration
import com.vecto_example.vecto.ui.write.WriteRepository
import com.vecto_example.vecto.ui.write.WriteViewModel
import com.vecto_example.vecto.ui.write.WriteViewModelFactory
import com.vecto_example.vecto.utils.DateTimeUtils
import com.vecto_example.vecto.utils.MapMarkerManager
import com.vecto_example.vecto.utils.MapOverlayManager
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

class EditPostActivity : AppCompatActivity(), OnMapReadyCallback, CalendarDialog.OnDateSelectedListener {
    lateinit var binding: ActivityEditPostBinding

    private val writeViewModel: WriteViewModel by viewModels {
        WriteViewModelFactory(WriteRepository(VectoService.create(), NaverSearchApiService.create()))
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

    private lateinit var locationDataList: MutableList<LocationData>
    private lateinit var visitDataList: MutableList<VisitData>

    private var mapSnapshot = mutableListOf<Bitmap>()
    private var imageUri = mutableListOf<Uri>()
    private var imageUrl = mutableListOf<String>()

    private var uploadStarted = false

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

        writeViewModel.reverseGeocode(visitDataList)

        feedId = intent.getIntExtra("feedId", -1)

        if(feedId != -1) {
            initUI()

            initMap()
            initRecyclerView(feedInfo)
            initListeners()
            initObservers()

            setGalleryResult()
            setCropResult()

        } else {
            Toast.makeText(this, "게시글 정보 불러오기에 실패했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
        }
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
            } else {
                binding.DeleteButton.visibility = View.GONE
                binding.naverMapEditPost.visibility = View.INVISIBLE
                visitDataList.clear()
                locationDataList.clear()
                mapOverlayManager.deleteOverlay()
            }
        }

        writeViewModel.mapImageUrls.observe(this){
            val allImageUrl: List<String> = imageUrl + (writeViewModel.imageUrls.value ?: emptyList())

            Log.d("EditPost", "VISIT DATA SIZE : ${writeViewModel.visitDataForWriteList.size}")
            Log.d("EditPost", "ORIGINAL IMAGE SIZE: ${imageUrl.size}")
            Log.d("EditPost", "NEW IMAGE SIZE: ${writeViewModel.imageUrls.value?.size}")

            if(imageUri.isEmpty() && uploadStarted){   //업로드 할 Normal Image 가 없는 경우
                Log.d("EditPost", "업로드 할 이미지가 없고 지도 이미지 업로드가 완료되었습니다.")

                uploadData(allImageUrl)
            } else if(writeViewModel.normalImageDone.value == true && uploadStarted) { //업로드 할 Normal Image 가 이미 완료된 경우
                Log.d("EditPost", "업로드 할 이미지가 완료되었고 지도 이미지 업로드가 완료되었습니다.")

                uploadData(allImageUrl)
            }
        }

        writeViewModel.imageUrls.observe(this){
            val allImageUrl: List<String> = imageUrl + (writeViewModel.imageUrls.value ?: emptyList())

            Log.d("EditPost", "VISIT DATA SIZE : ${writeViewModel.visitDataForWriteList.size}")
            Log.d("EditPost", "ORIGINAL IMAGE SIZE: ${imageUrl.size}")
            Log.d("EditPost", "NEW IMAGE SIZE: ${writeViewModel.imageUrls.value?.size}")

            if(writeViewModel.mapImageDone.value == true && uploadStarted){  //Normal Image 와 지도 이미지 모두 완료된 경우
                Log.d("EditPost", "업로드 할 이미지가 있고 지도 이미지 업로드는 완료되었습니다.")

                uploadData(allImageUrl)
            }

        }

        writeViewModel.updateFeedResult.observe(this){
            if(it == "SUCCESS"){
                Toast.makeText(this, "게시글 수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()

                finish()
            }
        }

        writeViewModel.errorLiveData.observe(this){
            sendFailMessage(it, this, "errorLiveData")
        }

        writeViewModel.feedErrorLiveData.observe(this){
            sendFailMessage(it, this, "feedErrorLiveData")
        }

        writeViewModel.imageErrorLiveData.observe(this){
            sendFailMessage(it, this, "imageErrorLiveData")
        }

    }

    private fun uploadData(allImageUrl: List<String>) {
        writeViewModel.updateFeed(
            VectoService.UpdateFeedRequest(
                feedId,
                binding.EditTitle.text.toString(),
                binding.EditContent.text.toString(),
                allImageUrl.toMutableList(),
                locationDataList,
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
                showDatePickerDialog()
            }
        }

        binding.DeleteButton.setOnClickListener {
            writeViewModel.deleteCourseData()
        }

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
    private fun initRecyclerView(feedInfo: VectoService.FeedInfoResponse) {
        myEditImageAdapter = MyEditImageAdapter(this)
        myEditImageAdapter.imageUrl.addAll(feedInfo.image)
        myEditImageAdapter.notifyDataSetChanged()
        myEditImageAdapter.setOnItemRemovedListener(object :
            MyEditImageAdapter.OnItemRemovedListener {
            override fun onItemRemoved() {
                binding.PhotoIconText.text = "${myEditImageAdapter.itemCount}/10"

                imageUri = myEditImageAdapter.imageUri
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

                mapOverlayManager.deleteOverlay()

                //선택한 날짜의 방문지의 처음과 끝까지의 경로
                locationDataList = LocationDatabase(this).getBetweenLocationData(selectedItems.first().datetime, selectedItems.last().datetime)

                mapOverlayManager.addPathOverlayForLocation(locationDataList)

                if(selectedItems.size == 1)
                    mapOverlayManager.moveCameraForVisitUpload(selectedItems[0])
                else
                    mapOverlayManager.moveCameraForPathUpload(locationDataList)
                val locationDataForPath = mutableListOf<LocationData>()

                //location 첫 좌표 넣어줌.
                locationDataForPath.add(LocationData(selectedItems[0].datetime, selectedItems[0].lat_set, selectedItems[0].lng_set))

                for (visitdatalist in selectedItems){
                    mapMarkerManager.addVisitMarkerBasic(visitdatalist)
                }

                visitDataList = selectedItems.toMutableList()

                writeViewModel.reverseGeocode(visitDataList)
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
        if(visitDataList.size == 1)
            mapOverlayManager.moveCameraForVisitUpload(visitDataList[0])
        else
            mapOverlayManager.moveCameraForPathUpload(locationDataList)

        Handler(Looper.getMainLooper()).postDelayed({
            naverMap.takeSnapshot {
                mapSnapshot.add(it)
            }
        }, 900)

        Handler(Looper.getMainLooper()).postDelayed({
            changeMapSize(340, 170)
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            if(visitDataList.size == 1)
                mapOverlayManager.moveCameraForVisitUpload(visitDataList[0])
            else
                mapOverlayManager.moveCameraForPathUpload(locationDataList)
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

            if (imageUri.isNotEmpty()) {//업로드 할 이미지가 있으면
                for (uri in imageUri) {
                    val file = File(uri.path!!)
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    imageParts.add(imagePart)
                }

                writeViewModel.uploadImages("NORMAL", imageParts)
            }

        }, 2200)

    }

    private fun sendFailMessage(message: String, context: Context, type: String) {
        if(message == "FAIL"){
            when(type){
                "errorLiveData" -> {
                    Toast.makeText(context, "방문지 역지오코딩에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
                "feedErrorLiveData" -> {
                    Toast.makeText(context, "게시글 수정에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
                "imageErrorLiveData" -> {
                    Toast.makeText(context, "이미지 업로드에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        else if(message == "ERROR"){
            Toast.makeText(context, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDateSelected(date: String) {
        val previousDate = DateTimeUtils.getPreviousDate(date)

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
            mapOverlayManager.deleteOverlay()
        }
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

        mapMarkerManager = MapMarkerManager(this, naverMap)
        mapOverlayManager = MapOverlayManager(this, mapMarkerManager, naverMap)

        if(locationDataList.isNotEmpty() && visitDataList.isNotEmpty()) {//원본 경로 설정
            mapOverlayManager.addPathOverlayForLocation(locationDataList)

            if (visitDataList.size == 1)
                mapOverlayManager.moveCameraForVisitUpload(visitDataList[0])
            else
                mapOverlayManager.moveCameraForPathUpload(locationDataList)
            val locationDataForPath = mutableListOf<LocationData>()

            locationDataForPath.add(
                LocationData(
                    visitDataList[0].datetime,
                    visitDataList[0].lat_set,
                    visitDataList[0].lng_set
                )
            )

            for (visitdatalist in visitDataList) {
                mapMarkerManager.addVisitMarkerBasic(visitdatalist)
            }
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
                val selectedImages = result.data?.clipData

                val totalImage =
                    if (selectedImages != null)
                        myEditImageAdapter.itemCount + selectedImages.itemCount
                    else
                        myEditImageAdapter.itemCount + 1

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
                myEditImageAdapter.notifyDataSetChanged()
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("UCrop", "Crop error: $cropError")
            }
        }
    }

    private fun addImage(newImageUri: Uri) {
        Log.d("ASDASD2", "ADDIMAGE 호출")

        if (myEditImageAdapter.itemCount > 10) {
            Toast.makeText(this, "최대 10개의 이미지만 추가 가능합니다.", Toast.LENGTH_LONG).show()
            return
        }

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

        myEditImageAdapter.imageUri.add(Uri.fromFile(file))
        imageUri = myEditImageAdapter.imageUri

    }
}