package com.vecto_example.vecto.ui.write

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.retrofit.VectoService
import com.vecto_example.vecto.utils.DateTimeUtils
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class WriteViewModel(private val repository: WriteRepository, private val tokenRepository: TokenRepository): ViewModel() {
    private val _reissueResponse = MutableSharedFlow<VectoService.TokenUpdateEvent>(replay = 0)
    val reissueResponse = _reissueResponse.asSharedFlow()

    lateinit var mapImagePart: List<MultipartBody.Part>
    lateinit var normalImagePart: List<MultipartBody.Part>
    lateinit var feedDataForUpload: VectoService.FeedDataForUpload
    lateinit var updateFeedRequest: VectoService.UpdateFeedRequest

    val visitDataList = mutableListOf<VisitData>()
    val locationDataList = mutableListOf<LocationData>()
    var imageUris = mutableListOf<Uri>()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _mapImageUrls = MutableLiveData<List<String>>()
    val mapImageUrls: LiveData<List<String>> = _mapImageUrls

    private val _imageUrls = MutableLiveData<List<String>>()
    val imageUrls: LiveData<List<String>> = _imageUrls

    private val _errorMessage = MutableLiveData<Int>()
    val errorMessage: LiveData<Int> = _errorMessage

    private val _addFeedResult = MutableLiveData<String>()
    val addFeedResult: LiveData<String> = _addFeedResult

    private val _updateFeedResult = MutableLiveData<String>()
    val updateFeedResult: LiveData<String> = _updateFeedResult

    lateinit var visitDataForWriteList: MutableList<VisitData>

    private val _mapImageDone = MutableLiveData<Boolean>()
    val mapImageDone: LiveData<Boolean> = _mapImageDone

    private val _normalImageDone = MutableLiveData<Boolean>()
    val normalImageDone: LiveData<Boolean> = _normalImageDone

    private val _isCourseDataLoaded = MutableLiveData(false)
    val isCourseDataLoaded: LiveData<Boolean> = _isCourseDataLoaded

    enum class Function {
        UploadMapImages, UploadNormalImages, AddFeed, UpdateFeed
    }

    enum class ImageType {
        MAP, NORMAL
    }

    fun uploadImages(type: String, imageParts: List<MultipartBody.Part>) {
        when(type){
            ImageType.MAP.name -> {
                mapImagePart = imageParts
            }
            ImageType.NORMAL.name -> {
                normalImagePart = imageParts
            }
        }

        viewModelScope.launch {
            val uploadImagesResponse = repository.uploadImages(imageParts)

            uploadImagesResponse.onSuccess {
                when(type){
                    ImageType.MAP.name -> {
                        _mapImageUrls.postValue(it.url)
                        _mapImageDone.value = true
                    }

                    ImageType.NORMAL.name -> {
                        _imageUrls.postValue(it.url)
                        _normalImageDone.value = true
                    }
                }
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        if(type == ImageType.MAP.name)
                            reissueToken(Function.UploadMapImages.name)
                        else if (type == ImageType.NORMAL.name)
                            reissueToken(Function.UploadNormalImages.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        endLoading()
                    }
                    else -> {
                        _errorMessage.postValue(R.string.upload_image_fail)
                        endLoading()
                    }
                }

                when(type){
                    ImageType.MAP.name -> {
                        _mapImageDone.value = false
                    }

                    ImageType.NORMAL.name -> {
                        _normalImageDone.value = false
                    }
                }
            }
        }
    }

    fun addFeed(feedDataForUpload: VectoService.FeedDataForUpload) {
        this.feedDataForUpload = feedDataForUpload

        viewModelScope.launch{
            val addFeedResponse = repository.addFeed(feedDataForUpload)

            addFeedResponse.onSuccess {
                _addFeedResult.postValue("SUCCESS")

                endLoading()
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.AddFeed.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        endLoading()
                    }
                    else -> {
                        _errorMessage.postValue(R.string.post_feed_fail)
                        endLoading()
                    }
                }
            }
        }

    }

    fun updateFeed(updateFeedRequest: VectoService.UpdateFeedRequest) {
        this.updateFeedRequest = updateFeedRequest

        viewModelScope.launch {
            val updateFeedResponse = repository.updateFeed(updateFeedRequest)

            updateFeedResponse.onSuccess {
                _updateFeedResult.postValue("SUCCESS")

                endLoading()
            }.onFailure {
                when(it.message){
                    ServerResponse.ACCESS_TOKEN_INVALID_ERROR.code -> {
                        reissueToken(Function.UpdateFeed.name)
                    }
                    ServerResponse.ERROR.code -> {
                        _errorMessage.postValue(R.string.APIErrorToastMessage)
                        endLoading()
                    }
                    else -> {
                        _errorMessage.postValue(R.string.update_feed_fail)
                        endLoading()
                    }
                }

            }
        }
    }

    fun isAllVisitDataValid(): Boolean {
        return visitDataForWriteList.all {
            it.name.isNotEmpty() && DateTimeUtils.isValidDateTimeFormat(it.datetime) && DateTimeUtils.isValidDateTimeFormat(it.endtime)
        }
    }

    private fun reissueToken(function: String){
        viewModelScope.launch {
            val reissueResponse = tokenRepository.reissueToken()

            reissueResponse.onSuccess { //Access Token이 만료되어서 갱신됨
                _reissueResponse.emit(VectoService.TokenUpdateEvent(function, it))
            }.onFailure {
                when(it.message){
                    //아직 유효한 경우
                    ServerResponse.ACCESS_TOKEN_VALID_ERROR.code -> {}
                    //Refresh Token 만료
                    ServerResponse.REFRESH_TOKEN_INVALID_ERROR.code -> {
                        _errorMessage.postValue(R.string.expired_login)
                    }
                    else -> {
                        _errorMessage.postValue(R.string.APIFailToastMessage)
                    }
                }
                endLoading()
            }
        }
    }

    fun startLoading(){
        Log.d("WriteViewModel", "START LOADING")

        _isLoading.value = true
    }

    private fun endLoading(){
        Log.d("WriteViewModel", "END LOADING")

        _isLoading.value = false
    }

    fun deleteCourseData(){
        Log.d("WriteViewModel", "deleteCourseData")

        visitDataList.clear()
        locationDataList.clear()

        _isCourseDataLoaded.value = false
        _mapImageDone.value = false
    }

    fun finishUpload(){
        _addFeedResult.value = ""

        deleteCourseData()
        visitDataForWriteList.clear()

        failUpload()
    }

    fun failUpload(){
        _mapImageUrls.value = emptyList()
        _mapImageDone.value = false

        _imageUrls.value = emptyList()
        _normalImageDone.value = false

        _isCourseDataLoaded.value = false
    }
}