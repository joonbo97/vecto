package com.example.vecto.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.naver.maps.geometry.LatLng

object Auth {
    val loginFlag: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    fun setLoginFlag(flag: Boolean) {
        loginFlag.value = flag
    }

    val showFlag: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)//확인 안한게 있으면 true
    fun setShowFlag(flag: Boolean){
        showFlag.value = flag
    }










    /*사용자 정보 관련*/
    var token: String = ""

    var provider = ""

    val _userId: MutableLiveData<String> = MutableLiveData("")

    val _profileImage: MutableLiveData<String?> = MutableLiveData(null)

    val _nickName: MutableLiveData<String> = MutableLiveData("")

    var email: String? = ""

    fun setUserData(prov: String, id: String, image: String?, nick: String, em: String?) {
        _userId.value = id
        provider = prov
        _profileImage.value = image
        _nickName.value = nick
        email = em
    }
}