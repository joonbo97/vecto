package com.vecto_example.vecto.ui.write

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.vecto_example.vecto.ui.login.LoginActivity
import com.vecto_example.vecto.ui.main.MainActivity
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.LocationDatabase
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDatabase
import com.vecto_example.vecto.dialog.LoginRequestDialog
import com.vecto_example.vecto.dialog.WriteBottomDialog
import com.vecto_example.vecto.dialog.WriteNameEmptyDialog
import com.vecto_example.vecto.retrofit.NaverSearchApiService
import com.vecto_example.vecto.retrofit.VectoService
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.vecto_example.vecto.R
import com.vecto_example.vecto.databinding.FragmentWriteBinding
import com.vecto_example.vecto.dialog.CalendarDialog
import com.vecto_example.vecto.ui.write.adapter.MyImageAdapter
import com.vecto_example.vecto.ui.decoration.SpacesItemDecoration
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
import java.time.LocalDateTime

class WriteFragment : Fragment(), OnMapReadyCallback, CalendarDialog.OnDateSelectedListener {
    private lateinit var binding: FragmentWriteBinding

    private val writeViewModel: WriteViewModel by viewModels {
        WriteViewModelFactory(WriteRepository(VectoService.create(), NaverSearchApiService.create()))
    }

    private lateinit var myImageAdapter: MyImageAdapter

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

    private var uploadStarted = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWriteBinding.inflate(inflater, container, false)

        initRecyclerView()
        initListeners()
        initObservers()

        return binding.root
    }

    private fun initObservers() {
        //로딩
        writeViewModel.isLoading.observe(viewLifecycleOwner){
            if(it){
                binding.progressBar.visibility = View.VISIBLE
                binding.constraintProgress.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.constraintProgress.visibility = View.GONE
            }
        }

        writeViewModel.isCourseDataLoaded.observe(viewLifecycleOwner){
            if(it){ //데이터가 불러와져 있으면
                binding.DeleteButton.visibility = View.VISIBLE
                binding.naverMapWrite.visibility = View.VISIBLE
            } else {
                binding.DeleteButton.visibility = View.GONE
                binding.naverMapWrite.visibility = View.INVISIBLE

                if(::visitDataList.isInitialized)
                    visitDataList.clear()
                if(::locationDataList.isInitialized)
                    locationDataList.clear()
                if(::mapOverlayManager.isInitialized)
                    mapOverlayManager.deleteOverlay()
            }
        }

        writeViewModel.mapImageUrls.observe(viewLifecycleOwner){
            if(imageUri.isEmpty() && uploadStarted){   //업로드 할 Normal Image 가 없는 경우
                writeViewModel.addFeed(
                    VectoService.FeedDataForUpload(
                        binding.EditTitle.text.toString(),
                        binding.EditContent.text.toString(),
                        LocalDateTime.now().withNano(0).toString(),
                        null,
                        locationDataList,
                        writeViewModel.visitDataForWriteList,
                        writeViewModel.mapImageUrls.value?.toMutableList()
                ))
            } else if(writeViewModel.normalImageDone.value == true && uploadStarted) { //업로드 할 Normal Image 가 이미 완료된 경우
                writeViewModel.addFeed(
                    VectoService.FeedDataForUpload(
                        binding.EditTitle.text.toString(),
                        binding.EditContent.text.toString(),
                        LocalDateTime.now().withNano(0).toString(),
                        writeViewModel.imageUrls.value?.toMutableList(),
                        locationDataList,
                        writeViewModel.visitDataForWriteList,
                        writeViewModel.mapImageUrls.value?.toMutableList()
                    ))
            }
        }

        writeViewModel.imageUrls.observe(viewLifecycleOwner){
            if((writeViewModel.mapImageDone.value == true && uploadStarted)){  //Normal Image 와 지도 이미지 모두 완료된 경우
                writeViewModel.addFeed(
                    VectoService.FeedDataForUpload(
                        binding.EditTitle.text.toString(),
                        binding.EditContent.text.toString(),
                        LocalDateTime.now().withNano(0).toString(),
                        writeViewModel.imageUrls.value?.toMutableList(),
                        locationDataList,
                        writeViewModel.visitDataForWriteList,
                        writeViewModel.mapImageUrls.value?.toMutableList()
                    ))
            }

        }

        writeViewModel.addFeedResult.observe(viewLifecycleOwner){
            if(it == "SUCCESS"){
                Toast.makeText(requireContext(), "게시글 작성이 완료되었습니다.", Toast.LENGTH_SHORT).show()

                writeViewModel.finishUpload()
                visitDataList.clear()
                locationDataList.clear()
                mapSnapshot.clear()
                myImageAdapter.imageUri.clear()
                mapOverlayManager.deleteOverlay()
                binding.EditTitle.text.clear()
                binding.EditContent.text.clear()

                (activity as? MainActivity)?.binding?.navView?.selectedItemId = R.id.SearchFragment
            }
        }

        //API 오류 메시지
        writeViewModel.errorLiveData.observe(viewLifecycleOwner){
            sendFailMessage(it, requireContext(), "errorLiveData")
        }

        writeViewModel.feedErrorLiveData.observe(viewLifecycleOwner){
            sendFailMessage(it, requireContext(), "feedErrorLiveData")
        }

        writeViewModel.imageErrorLiveData.observe(viewLifecycleOwner){
            sendFailMessage(it, requireContext(), "imageErrorLiveData")
        }

    }

    private fun initListeners() {
        binding.PhotoBoxImage.setOnClickListener {
            openGallery()
        }

        binding.LocationBoxImage.setOnClickListener {
            if(writeViewModel.isCourseDataLoaded.value == false) {
                initMap()
            }
        }

        binding.DeleteButton.setOnClickListener {
            writeViewModel.deleteCourseData()
        }

        binding.WriteDoneButton.setOnClickListener {
            if(Auth.loginFlag.value == false)
            {
                val loginRequestDialog = LoginRequestDialog(requireContext())
                loginRequestDialog.showDialog()
                loginRequestDialog.onOkButtonClickListener = {
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    this.startActivity(intent)
                }

                return@setOnClickListener
            }

            if(::visitDataList.isInitialized) {
                if (binding.EditTitle.text.isEmpty())
                    Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                else if (visitDataList.size == 0)
                    Toast.makeText(requireContext(), "경로를 선택해주세요.", Toast.LENGTH_SHORT).show()
                else {
                    writeViewModel.startLoading()

                    uploadStarted = true
                    takeSnapForMap()
                }
            } else {
                Toast.makeText(requireContext(), "경로를 선택해주세요.", Toast.LENGTH_SHORT).show()
            }

        }

        binding.BackButton.setOnClickListener {
            (activity as? MainActivity)?.updateBottomNavigationSelection(R.id.SearchFragment)
            val navController = findNavController()
            navController.navigate(R.id.SearchFragment)
        }
    }

    private fun initRecyclerView() {
        myImageAdapter = MyImageAdapter(requireContext())
        myImageAdapter.setOnItemRemovedListener(object : MyImageAdapter.OnItemRemovedListener {
            override fun onItemRemoved() {
                binding.PhotoIconText.text = "${myImageAdapter.itemCount}/10"

                imageUri = myImageAdapter.imageUri
            }
        })
        binding.WriteRecyclerView.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelSize(R.dimen.image_margin)))
        binding.WriteRecyclerView.adapter = myImageAdapter
        binding.WriteRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun initMap() {
        mapView = childFragmentManager.findFragmentById(R.id.naver_map_Write) as MapFragment?
            ?: MapFragment.newInstance().also {
                childFragmentManager.beginTransaction().add(R.id.naver_map_Write, it).commit()
            }
        mapView.getMapAsync(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setGalleryResult()
        setCropResult()
    }

    private fun showDatePickerDialog(){
        val calendarDialog = CalendarDialog(requireContext())
        calendarDialog.onDateSelectedListener = this
        calendarDialog.showDialog()
    }

    private fun selectVisit(selectedDate: String) = //특정 날짜에 해당하는 방문지를 선택하는 함수
        if(visitDataList.any{ it.name.isEmpty() })//이름항목이 하나라도 비어있을 경우
        {
            val writeNameEmptyDialog = WriteNameEmptyDialog(requireContext())
            writeNameEmptyDialog.showDialog()
            writeNameEmptyDialog.onOkButtonClickListener = {
                (activity as? MainActivity)?.updateBottomNavigationSelection(R.id.EditCourseFragment)
                val navController = findNavController()
                val bundle = bundleOf("selectedDateKey" to selectedDate)
                navController.navigate(R.id.EditCourseFragment, bundle)
            }
        }
        else//완성된 경우
        {
            val writeBottomDialog = WriteBottomDialog(requireContext())
            writeBottomDialog.showDialog(visitDataList) { selectedItems -> //구간을 선택함 (모든 정보 완료)

                binding.naverMapWrite.visibility = View.VISIBLE
                mapOverlayManager.deleteOverlay()

                //선택한 날짜의 방문지의 처음과 끝까지의 경로
                locationDataList = LocationDatabase(requireContext()).getBetweenLocationData(selectedItems.first().datetime, selectedItems.last().datetime)

                mapOverlayManager.addPathOverlayForLocation(locationDataList)

                if(selectedItems.size == 1)
                    mapOverlayManager.moveCameraForVisitUpload(selectedItems[0])
                else
                    mapOverlayManager.moveCameraForPathUpload(locationDataList)
                val locationDataforPath = mutableListOf<LocationData>()

                //location 첫 좌표 넣어줌.
                locationDataforPath.add(LocationData(selectedItems[0].datetime, selectedItems[0].lat_set, selectedItems[0].lng_set))

                for (visitdatalist in selectedItems){
                    mapMarkerManager.addVisitMarkerBasic(visitdatalist)
                }

                visitDataList = selectedItems.toMutableList()

                writeViewModel.reverseGeocode(visitDataList)
            }
        }

    private fun changeMapSize(w: Int, h: Int){
        val params = binding.naverMapWrite.layoutParams

        val density = resources.displayMetrics.density
        val width = (w * density)
        val height = (h * density)

        params.width = width.toInt() // 원하는 가로길이
        params.height = height.toInt() // 원하는 세로 길이

        binding.naverMapWrite.layoutParams = params
    }

    private fun takeSnapForMap(){
        binding.MapImage.visibility = View.INVISIBLE
        binding.naverMapWrite.visibility = View.VISIBLE

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
            binding.naverMapWrite.visibility = View.INVISIBLE

            val mapImageParts = mutableListOf<MultipartBody.Part>()
            for (bitmap in mapSnapshot) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
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
                    Toast.makeText(context, "게시글 업로드에 실패하였습니다.", Toast.LENGTH_SHORT).show()
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

    /*   Override 관련   */
    override fun onDateSelected(date: String) {
        val previousDate = DateTimeUtils.getPreviousDate(date)

        val filteredData = VisitDatabase(requireContext()).getAllVisitData().filter { visitData ->
            val visitDate = visitData.datetime.substring(0, 10)
            val endDate = visitData.endtime.substring(0, 10)
            visitDate == previousDate && endDate == date
        }

        visitDataList = VisitDatabase(requireContext()).getAllVisitData().filter {
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

        mapMarkerManager = MapMarkerManager(requireContext(), naverMap)
        mapOverlayManager = MapOverlayManager(requireContext(), mapMarkerManager, naverMap)

        showDatePickerDialog()
    }



    /*   이미지 관련   */

    //사진 추가 박스를 클릭 시, 갤러리를 열어 이미지 선택을 할 수 있도록 함
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryResultLauncher.launch(intent) // 갤러리 결과를 galleryResultLauncher로 받음
    }

    //갤러리 결과를 받는 galleryResultLauncher 설정
    private fun setGalleryResult() {
        galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImages = result.data?.clipData

                val totalImage =
                    if (selectedImages != null)
                        myImageAdapter.itemCount + selectedImages.itemCount
                    else
                        myImageAdapter.itemCount + 1

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
    }

    //galleryResultLauncher 로부터 전달 받은 이미지 crop 진행
    private fun startCrop(sourceUri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, destinationFileName))
        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(uCropOptions())
            .withAspectRatio(1f, 1f) // 1:1 비율로 자르기
            .getIntent(requireContext())
        cropResultLauncher.launch(uCropIntent)  //Crop 결과를 cropResultLauncher 에게 전달
    }

    //uCrop 옵션
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

    //Crop 결과를 받는 cropResultLauncher 설정
    private fun setCropResult() {
        cropResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                addImage(resultUri!!)
                myImageAdapter.notifyDataSetChanged()
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("UCrop", "Crop error: $cropError")
            }
        }
    }

    //Crop 된 이미지를 전달 받아 adapter 에 추가 하기 위한 전처리 작업 수행
    private fun addImage(newImageUri: Uri) {

        if (myImageAdapter.itemCount > 10) {
            Toast.makeText(requireContext(), "최대 10개의 이미지만 추가 가능합니다.", Toast.LENGTH_LONG).show()
            return
        }

        // 이미지 압축
        val compressedBytes = compressImage(newImageUri)
        saveCompressedImage(compressedBytes)


        // 가장 앞쪽의 ImageView에 새 이미지를 셋팅
        binding.PhotoIconText.text = "${myImageAdapter.itemCount}/10"
    }

    //이미지 압축을 진행
    private fun compressImage(uri: Uri): ByteArray {
        val outStream = ByteArrayOutputStream()
        val inputStream = requireContext().contentResolver.openInputStream(uri)

        if (inputStream == null) {
            Log.e("WriteFragment", "Failed to open InputStream for the provided Uri: $uri")
            return byteArrayOf()  // empty array
        }

        inputStream.use { stream ->
            val original = BitmapFactory.decodeStream(stream)

            if(original == null){
                Log.e("WriteFragment", "Failed to decode the image from Uri: $uri")
                return byteArrayOf()
            }

            val resizedImage = Bitmap.createScaledBitmap(original, 1080, 1080, true)

            resizedImage.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
        }

        return outStream.toByteArray()
    }

    //압축된 이미지를 저장 후 adapter 에 저장
    private fun saveCompressedImage(compressedBytes: ByteArray) {
        val filename = "compressed_${System.currentTimeMillis()}.jpeg"
        val file = File(requireContext().cacheDir, filename)
        file.outputStream().use { it.write(compressedBytes) }

        imageUri.add(Uri.fromFile(file))
        myImageAdapter.imageUri.add(Uri.fromFile(file))
    }
}