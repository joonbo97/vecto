package com.vecto_example.vecto.ui.write

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDataForWrite
import com.vecto_example.vecto.retrofit.VectoService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class WriteViewModel(private val repository: WriteRepository): ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _mapImageUrls = MutableLiveData<List<String>>()
    val mapImageUrls: LiveData<List<String>> = _mapImageUrls

    private val _imageUrls = MutableLiveData<List<String>>()
    val imageUrls: LiveData<List<String>> = _imageUrls

    private val _errorLiveData = MutableLiveData<String>()
    val errorLiveData: LiveData<String> = _errorLiveData

    private val _imageErrorLiveData = MutableLiveData<String>()
    val imageErrorLiveData: LiveData<String> = _imageErrorLiveData

    private val _feedErrorLiveData = MutableLiveData<String>()
    val feedErrorLiveData: LiveData<String> = _feedErrorLiveData

    private val _addFeedResult = MutableLiveData<String>()
    val addFeedResult: LiveData<String> = _addFeedResult

    private val _updateFeedResult = MutableLiveData<String>()
    val updateFeedResult: LiveData<String> = _updateFeedResult

    private lateinit var address: MutableList<String>

    lateinit var visitDataForWriteList: MutableList<VisitDataForWrite>

    private val _mapImageDone = MutableLiveData<Boolean>()
    val mapImageDone: LiveData<Boolean> = _mapImageDone

    private val _normalImageDone = MutableLiveData<Boolean>()
    val normalImageDone: LiveData<Boolean> = _normalImageDone

    private val _isCourseDataLoaded = MutableLiveData<Boolean>(false)
    val isCourseDataLoaded: LiveData<Boolean> = _isCourseDataLoaded

    fun uploadImages(type: String, imageParts: List<MultipartBody.Part>) {

        viewModelScope.launch {
            val uploadImagesResponse = repository.uploadImages(imageParts)

            uploadImagesResponse.onSuccess {

                when(type){
                    "MAP" -> {
                        _mapImageUrls.postValue(it.url)
                        _mapImageDone.value = true
                    }

                    "NORMAL" -> {
                        _imageUrls.postValue(it.url)
                        _normalImageDone.value = true
                    }
                }

            }.onFailure {
                _imageErrorLiveData.value = it.message

                when(type){
                    "MAP" -> {
                        _mapImageDone.value = false
                    }

                    "NORMAL" -> {
                        _normalImageDone.value = false
                    }
                }

                endLoading()
            }

        }
    }

    fun addFeed(feedDataForUpload: VectoService.FeedDataForUpload) {

        viewModelScope.launch{
            val addFeedResponse = repository.addFeed(feedDataForUpload)

            addFeedResponse.onSuccess {
                _addFeedResult.postValue("SUCCESS")

                endLoading()
            }.onFailure {
                _feedErrorLiveData.value = it.message

                endLoading()
            }
        }

    }

    fun updateFeed(updateFeedRequest: VectoService.UpdateFeedRequest) {
        Log.d("UPDATE DATA", "${visitDataForWriteList.size}}")

        viewModelScope.launch {
            val updateFeedResponse = repository.updateFeed(updateFeedRequest)

            updateFeedResponse.onSuccess {
                _updateFeedResult.postValue("SUCCESS")

                endLoading()
            }.onFailure {
                _feedErrorLiveData.value = it.message

                endLoading()
            }
        }
    }

    fun reverseGeocode(visitDataList: MutableList<VisitData>) {

        visitDataForWriteList = MutableList(visitDataList.size){ VisitDataForWrite("", "", 0.0, 0.0, 0.0, 0.0, 0, "", "") }
        address = MutableList(visitDataList.size) {""}
        _isCourseDataLoaded.value = true

        viewModelScope.launch {
            try {
                val geocodeList = visitDataList.map { async { repository.reverseGeocode(it) } }.awaitAll()

                geocodeList.forEachIndexed { index, result ->
                    result.onSuccess {
                        if(it.results[0].region?.area1?.name?.isEmpty() == false) {
                            address[index] += it.results[0].region?.area1?.name.toString()
                            if (it.results[0].region?.area2?.name?.isEmpty() == false) {
                                address[index] += (" " + it.results[0].region?.area2?.name)
                                if (it.results[0].region?.area3?.name?.isEmpty() == false)
                                    address[index] += (" " + it.results[0].region?.area3?.name)
                            }
                        }
                    }.onFailure {
                        address[index] = ""
                    }

                    visitDataForWriteList[index] = VisitDataForWrite(
                        datetime = visitDataList[index].datetime,
                        endtime = visitDataList[index].endtime,
                        lat = visitDataList[index].lat,
                        lng = visitDataList[index].lng,
                        lat_set = visitDataList[index].lat_set,
                        lng_set = visitDataList[index].lng_set,
                        staytime = visitDataList[index].staytime,
                        name = visitDataList[index].name,
                        address = address[index]
                    )

                }
            } catch (e: Exception) {
                if(e.message == "FAIL"){
                    _errorLiveData.value = "FAIL"
                }else if(e.message == "ERROR"){
                    _errorLiveData.value = "ERROR"
                }
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

        _isCourseDataLoaded.value = false
    }

    fun finishUpload(){
        _addFeedResult.value = ""
        visitDataForWriteList.clear()
        _mapImageUrls.postValue(emptyList())
        _imageUrls.postValue(emptyList())
        _isCourseDataLoaded.value = false
    }
}