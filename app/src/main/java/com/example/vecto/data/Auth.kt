package com.example.vecto.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.naver.maps.geometry.LatLng

object Auth {
    val loginFlag: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    fun setLoginFlag(flag: Boolean) {
        loginFlag.value = flag
    }











    /*사용자 정보 관련*/
    var token: String = ""

    var provider = ""

    private val _userId: MutableLiveData<String> = MutableLiveData("")
    val userId: LiveData<String> = _userId

    private val _profileImage: MutableLiveData<String?> = MutableLiveData(null)
    val profileImage: LiveData<String?> = _profileImage

    private val _nickName: MutableLiveData<String> = MutableLiveData("")
    val nickName: LiveData<String> = _nickName

    var email: String? = ""

    fun setUserData(prov: String, id: String, image: String?, nick: String, em: String?) {
        _userId.value = id
        provider = prov
        _profileImage.value = image
        _nickName.value = nick
        email = em
    }
}