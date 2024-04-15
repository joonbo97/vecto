package com.vecto_example.vecto.ui.write

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.model.VisitDataForWite
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

    private val address = mutableListOf<String>()

    lateinit var visitDataForWriteList: MutableList<VisitDataForWite>

    private val _mapImageDone = MutableLiveData<Boolean>()
    val mapImageDone: LiveData<Boolean> = _mapImageDone

    private val _normalImageDone = MutableLiveData<Boolean>()
    val normalImageDone: LiveData<Boolean> = _normalImageDone

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

    fun addFeed(postDataForUpload: VectoService.PostDataForUpload) {

        viewModelScope.launch{
            val addFeedResponse = repository.addFeed(postDataForUpload)

            addFeedResponse.onSuccess {
                _addFeedResult.postValue("SUCCESS")

                endLoading()
            }.onFailure {
                _feedErrorLiveData.value = it.message

                endLoading()
            }
        }

    }

    fun updateFeed(updatePostRequest: VectoService.UpdatePostRequest) {

        viewModelScope.launch {
            val updateFeedResponse = repository.updateFeed(updatePostRequest)

            updateFeedResponse.onSuccess {

            }.onFailure {
                _feedErrorLiveData.value = it.message

                endLoading()
            }
        }
    }

    fun reverseGeocode(visitDataList: MutableList<VisitData>) {

        visitDataForWriteList = MutableList(visitDataList.size){ VisitDataForWite("", "", 0.0, 0.0, 0.0, 0.0, 0, "", "") }

        viewModelScope.launch {
            try {
                val geocodeList = visitDataList.map{
                    async { repository.reverseGeocode(it) }
                }.awaitAll()

                for(i in 0 until visitDataList.size){
                    geocodeList[i].onSuccess {
                        if(it.results[0].region?.area1?.name?.isEmpty() == false){
                            address[i] += it.results[0].region?.area1?.name.toString()
                            if(it.results[0].region?.area2?.name?.isEmpty() == false){
                                address[i] += (" " + it.results[0].region?.area2?.name)
                                if(it.results[0].region?.area3?.name?.isEmpty() == false)
                                    address[i] += (" " + it.results[0].region?.area3?.name)
                            }
                        }
                    }.onFailure {
                        address[i] = ""
                    }

                    visitDataForWriteList[i] = VisitDataForWite(visitDataList[i].datetime,
                        visitDataList[i].endtime, visitDataList[i].lat, visitDataList[i].lng,
                        visitDataList[i].lat_set, visitDataList[i].lng_set, visitDataList[i].staytime,
                        visitDataList[i].name, address[i])
                }

            } catch (e: Exception) {
                if(e.message == "FAIL"){
                    _errorLiveData.value = "FAIL"
                }else if(e.message == "ERROR"){
                    _errorLiveData.value = "ERROR"
                }
            }
        }

        /*viewModelScope.launch{
            val reverseGeocodeResponse = repository.reverseGeocode(visitData)

            reverseGeocodeResponse.onSuccess {

            }.onFailure {
                if(it.message == "FAIL"){
                    _errorLiveData.value = "FAIL"
                }else if(it.message == "ERROR"){
                    _errorLiveData.value = "ERROR"
                }
            }

        }*/

    }

    fun startLoading(){
        Log.d("STARTLOADING", "START")

        _isLoading.value = true
    }

    private fun endLoading(){
        Log.d("ENDLOADING", "END")

        _isLoading.value = false
    }
}