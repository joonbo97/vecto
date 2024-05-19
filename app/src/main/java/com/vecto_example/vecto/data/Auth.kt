package com.vecto_example.vecto.data

import androidx.lifecycle.MutableLiveData
import com.vecto_example.vecto.retrofit.VectoService

object Auth {
    val loginFlag: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    fun setLoginFlag(flag: Boolean) {
        loginFlag.value = flag
    }

    /*사용자 정보 관련*/
    var accessToken: String = ""
    var refreshToken: String = ""

    var provider = ""
    val userId: MutableLiveData<String> = MutableLiveData("")
    val profileImage: MutableLiveData<String?> = MutableLiveData(null)
    val nickName: MutableLiveData<String> = MutableLiveData("")
    var email: String? = ""

    fun setUserData(prov: String, id: String, image: String?, nick: String, em: String?) {
        userId.value = id
        provider = prov
        profileImage.value = image
        nickName.value = nick
        email = em
    }

    fun setToken(loginResponse: VectoService.UserToken){
        accessToken = loginResponse.accessToken
        refreshToken = loginResponse.refreshToken
    }
}