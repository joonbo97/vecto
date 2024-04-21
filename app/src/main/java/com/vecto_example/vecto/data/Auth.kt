package com.vecto_example.vecto.data

import androidx.lifecycle.MutableLiveData

object Auth {
    val loginFlag: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    fun setLoginFlag(flag: Boolean) {
        loginFlag.value = flag
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