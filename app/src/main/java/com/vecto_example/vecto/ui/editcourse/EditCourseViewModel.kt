package com.vecto_example.vecto.ui.editcourse

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naver.maps.geometry.LatLng
import com.vecto_example.vecto.data.model.LocationData
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.data.repository.NaverRepository
import com.vecto_example.vecto.data.repository.TMapRepository
import com.vecto_example.vecto.retrofit.TMapAPIService
import com.vecto_example.vecto.utils.ServerResponse
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EditCourseViewModel(private val repository: TMapRepository, private val naverRepository: NaverRepository) : ViewModel() {
    private val _setVisitDataList = MutableSharedFlow<MutableList<VisitData>>()
    val setVisitDataList = _setVisitDataList.asSharedFlow()

    private val _responseRecommendLiveData = MutableLiveData<TMapAPIService.GeoJsonResponse>()
    val responseRecommendLiveData: LiveData<TMapAPIService.GeoJsonResponse> = _responseRecommendLiveData

    val poiResponseList = mutableListOf<TMapAPIService.Poi>()

    private val _responseErrorLiveData = MutableLiveData<String>()
    val responseErrorLiveData: LiveData<String> = _responseErrorLiveData

    private val _buttonRecommend = MutableLiveData<Boolean>()
    val buttonRecommend: LiveData<Boolean> = _buttonRecommend

    private val _buttonSelect = MutableLiveData<Boolean>()
    val buttonSelect: LiveData<Boolean> = _buttonSelect

    private val _editVisitButton = MutableLiveData<Boolean>()
    val editVisitButton: LiveData<Boolean> = _editVisitButton

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isBlock = MutableLiveData<Boolean>()
    val isBlock: LiveData<Boolean> = _isBlock

    private val _isFinished = MutableLiveData<Boolean>()
    val isFinished: LiveData<Boolean> = _isFinished

    private val _date = MutableLiveData<String?>(null)
    val date: LiveData<String?> = _date

    var start = LatLng(0.0, 0.0)
    var end = LatLng(0.0, 0.0)

    private var currentPage = 1

    var responsePathData = mutableListOf<LatLng>()

    var totalDistance = 0

    fun recommendRoute(locationDataList: MutableList<LocationData>, type: String) {
        startLoading()

        start = LatLng(locationDataList.first().lat, locationDataList.first().lng)
        end = LatLng(locationDataList.last().lat, locationDataList.last().lng)


        viewModelScope.launch {

            val recommendRouteResponse: Result<TMapAPIService.GeoJsonResponse> = when(type){
                ServerResponse.VISIT_TYPE_WALK.code -> {
                    repository.recommendRoute(TMapAPIService.RecommendRouteRequest(1, TMapAPIService.key(), start.latitude, start.longitude, end.latitude, end.longitude, "WGS84GEO", "WGS84GEO", "출발지_이름", "도착지_이름", 0))
                }

                ServerResponse.VISIT_TYPE_CAR.code -> {
                    repository.recommendCarRoute(TMapAPIService.RecommendRouteRequest(1, TMapAPIService.key(), start.latitude, start.longitude, end.latitude, end.longitude, "WGS84GEO", "WGS84GEO", "출발지_이름", "도착지_이름", 0))
                }

                ServerResponse.VISIT_TYPE_PUBLIC_TRANSPORT.code -> {
                    repository.recommendCarRoute(TMapAPIService.RecommendRouteRequest(1, TMapAPIService.key(), start.latitude, start.longitude, end.latitude, end.longitude, "WGS84GEO", "WGS84GEO", "출발지_이름", "도착지_이름", 0))
                }

                else -> {
                    repository.recommendRoute(TMapAPIService.RecommendRouteRequest(1, TMapAPIService.key(), start.latitude, start.longitude, end.latitude, end.longitude, "WGS84GEO", "WGS84GEO", "출발지_이름", "도착지_이름", 0))
                }
            }


            recommendRouteResponse.onSuccess {

                _responseRecommendLiveData.postValue(it)

                totalDistance = it.features[0].properties.totalDistance

            }.onFailure {
                if(it.message == "FAIL"){
                    _responseErrorLiveData.value = "FAIL"
                }else if(it.message == "ERROR"){
                    _responseErrorLiveData.value = "ERROR"
                }

                endLoading()
            }
        }
    }

    fun searchNearbyPoi(visitData: VisitData, category: String) {
        startLoading()

        viewModelScope.launch {
            val searchNearbyPoiResponse = repository.searchNearbyPoi(TMapAPIService.SearchNearbyPoiRequest(
                1, category, TMapAPIService.key(), currentPage, 1,100 , visitData.lat, visitData.lng))

            searchNearbyPoiResponse.onSuccess {
                val pois = it.searchPoiInfo.pois.poi

                for (poi in pois) {
                    if(checkDistance(LatLng(visitData.lat, visitData.lng), LatLng(poi.frontLat, poi.frontLon), 100)){
                        Log.d("POI Name", poi.name)
                        Log.d("POI Latitude", poi.frontLat.toString())
                        Log.d("POI Longitude", poi.frontLon.toString())
                        poiResponseList.add(poi)
                    }
                    else
                    {
                        _isFinished.value = true
                        currentPage = 1
                        break
                    }

                    if(poi == pois[pois.lastIndex]){
                        _isFinished.value = false
                        currentPage++
                        break
                    }
                }

            }.onFailure {
                if(it.message == "FAIL"){
                    _responseErrorLiveData.value = "FAIL"
                }else if(it.message == "ERROR"){
                    _responseErrorLiveData.value = "ERROR"
                }

                currentPage = 1

                endLoading()
            }

        }
    }

    fun getTimeDiff(datetime1: String, datetime2: String): Int {
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        val date1 = LocalDateTime.parse(datetime1, format)
        val date2 = LocalDateTime.parse(datetime2, format)

        return Duration.between(date1, date2).toMinutes().toInt()
    }

    fun setDate(date: String?) {
        _date.value = date

        if(date == null){
            setBlock()
        } else {
            clearBlock()
        }
    }

    fun setButtonVisibility(type: String) {
        when(type){
            ButtonType.EDIT_VISIT.name -> {
                _editVisitButton.value = true
                _buttonRecommend.value = false
                _buttonSelect.value = false
            }

            ButtonType.EDIT_PATH.name -> {
                _editVisitButton.value = false
                _buttonRecommend.value = true
                _buttonSelect.value = false
            }

            ButtonType.SELECT.name -> {
                _editVisitButton.value = false
                _buttonRecommend.value = false
                _buttonSelect.value = true
            }

            ButtonType.NONE.name -> {
                _editVisitButton.value = false
                _buttonRecommend.value = false
                _buttonSelect.value = false
            }
        }
    }

    fun checkDistance(centerLatLng: LatLng, currentLatLng: LatLng, checkDistance: Int): Boolean{
        return centerLatLng.distanceTo(currentLatLng) <= checkDistance.toDouble()
    }

    fun calculateDistance(centerLatLng: LatLng, currentLatLng: LatLng): Double{
        return centerLatLng.distanceTo(currentLatLng)
    }

    fun overlayDone(){
        endLoading()
    }

    fun overlayStart(){
        startLoading()
    }

    private fun setBlock(){
        Log.d("SET_BLOCK", "SET")
        _isBlock.value = true
    }

    private fun clearBlock(){
        Log.d("CLEAR_BLOCK", "CLEAR")
        _isBlock.value = false
    }

    private fun startLoading(){
        Log.d("START_LOADING", "START")
        _isLoading.value = true
    }

    private fun endLoading(){
        Log.d("END_LOADING", "END")
        _isLoading.value = false
    }

    enum class ButtonType{
        EDIT_VISIT, EDIT_PATH, SELECT, NONE
    }

    fun reverseGeocode(visitDataList: MutableList<VisitData>) {
        viewModelScope.launch {

            val geocodeList = visitDataList.map { async { naverRepository.reverseGeocode(it) } }.awaitAll()

            var finishedCount = 0


            geocodeList.forEachIndexed { index, result ->

                if(visitDataList[index].address.isNullOrEmpty()){
                    result.onSuccess { geocodeResult ->

                        if(geocodeResult.results.isEmpty())
                            return@onSuccess

                        val requestResult = geocodeResult.results[0]

                        requestResult.region?.area1?.name?.takeIf { it.isNotEmpty() }?.let { visitDataList[index].address = it }
                        requestResult.region?.area2?.name?.takeIf { it.isNotEmpty() }?.let { visitDataList[index].address += " $it" }
                        requestResult.region?.area3?.name?.takeIf { it.isNotEmpty() }?.let { visitDataList[index].address += " $it" }
                        requestResult.land?.name?.takeIf { it.isNotEmpty() }?.let { visitDataList[index].address += " $it" }
                        requestResult.land?.number1?.takeIf { it.isNotEmpty() }?.let { visitDataList[index].address += " $it" }
                        requestResult.land?.number2?.takeIf { it.isNotEmpty() }?.let { visitDataList[index].address += " $it" }
                        requestResult.addition0?.value?.takeIf { it.isNotEmpty() }?.let { visitDataList[index].address += " $it" }

                        if(visitDataList[index].name.isEmpty() && visitDataList[index].address!!.isNotEmpty()) {
                            getSearch(visitDataList[index].address!!, visitDataList, finishedCount) { searchResult ->
                                Log.d("SEARCH ", "SEARCH")
                                visitDataList[index].name = searchResult
                                finishedCount++
                            }
                        } else {
                            finishedCount++

                            if(finishedCount == visitDataList.size)
                                emitVisitData(visitDataList)
                        }
                    }.onFailure {
                        visitDataList[index].address = ""
                        finishedCount++

                        if(finishedCount == visitDataList.size)
                            emitVisitData(visitDataList)
                    }
                }
            }

            if(finishedCount == visitDataList.size)
                emitVisitData(visitDataList)
        }
    }

    private fun getSearch(query: String, visitDataList: MutableList<VisitData>, finishedCount: Int, callback: (String) -> Unit){

        viewModelScope.launch {
            val searchResponse = naverRepository.getSearch(query)

            searchResponse.onSuccess {
                if(it.items.isNotEmpty()){
                    callback(it.items[0].title)
                } else {
                    callback("")
                }
            }.onFailure {
                callback("")
            }

            if(finishedCount == visitDataList.size - 1)
                emitVisitData(visitDataList)
        }
    }

    private suspend fun emitVisitData(visitDataList: MutableList<VisitData>){
        _setVisitDataList.emit(visitDataList)
    }
}