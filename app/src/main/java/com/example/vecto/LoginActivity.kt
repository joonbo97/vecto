package com.example.vecto

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.vecto.data.Auth
import com.example.vecto.databinding.ActivityLoginBinding
import com.example.vecto.retrofit.TMapAPIService
import com.example.vecto.retrofit.VectoService
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.model.KakaoSdkError
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.naver.maps.geometry.LatLng
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var nickname: String
    private lateinit var fcmtoken: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.LoginTextForRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java) //Register 화면으로 이동
            startActivity(intent)
        }



        KakaoSdk.init(this, "1ad70f21b84b4b2472d7b036d0fc40ce")
        binding.LoginBoxKakao.setOnClickListener {
            // 카카오톡 설치 확인
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                // 카카오톡 로그인
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    // 로그인 실패 부분
                    if (error != null) {
                        Log.e("TAG", "로그인 실패 $error")
                        // 사용자가 취소
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled ) {
                            return@loginWithKakaoTalk
                        }
                        // 다른 오류
                        else {
                            UserApiClient.instance.loginWithKakaoAccount(this, callback = mCallback) // 카카오 이메일 로그인
                        }
                    }
                    // 로그인 성공 부분
                    else if (token != null) {
                        Log.e("TAG", "로그인 성공 ${token.accessToken}")

                        if (AuthApiClient.instance.hasToken()) {
                            UserApiClient.instance.accessTokenInfo { _, e ->
                                if (e != null) {
                                    if (e is KakaoSdkError && e.isInvalidTokenError()) {
                                        //로그인 필요
                                    }
                                    else {
                                        //기타 에러
                                    }
                                }
                                else {
                                    //토큰 유효성 체크 성공(필요 시 토큰 갱신됨)
                                    UserApiClient.instance.me { user, error ->
                                        if (error != null) {
                                            Log.e("TAG", "사용자 정보 요청 실패 $error")
                                        } else if (user != null) {
                                            Log.e("TAG", "사용자 정보 요청 성공 : $user")

                                            nickname = user.kakaoAccount?.profile?.nickname.toString()
                                            fcmtoken = getTokenFromSharedPref(this).toString()
                                            //user.id 를 통해 고유 Unique한 데이터 획득 가능
                                            sendLoginRequest(VectoService.LoginRequest(user.id.toString(), null, fcmtoken))
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            //로그인 필요
                        }
                    }
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(this, callback = mCallback) // 카카오 이메일 로그인
            }
        }

    }

    private val mCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            Log.e("TAG", "로그인 실패 $error")
        } else if (token != null) {
            Log.e("TAG", "로그인 성공 ${token.accessToken}")
        }
    }

    private fun sendLoginRequest(loginRequest: VectoService.LoginRequest){
        val vectoService = VectoService.create()

        val call = vectoService.loginUser(loginRequest)
        call.enqueue(object : Callback<VectoService.VectoResponse>{
            override fun onResponse(call: Call<VectoService.VectoResponse>, response: Response<VectoService.VectoResponse>) {
                if (response.isSuccessful) {
                    // 로그인 성공
                    Log.d("VectoLogin", "로그인 성공 : " + response.message())
                    Auth.token = response.body()!!.token
                    getUserInfo(response.body()!!.token)
                } else {
                    // 서버 에러 처리
                    Log.d("VectoLogin", "로그인 실패 : " + response.errorBody()?.string())

                    if(response.code() == 401){
                        //kakao일 때, 로그인에 실패하면 회원가입까지 자동으로 완료.
                        sendRegisterRequest(VectoService.RegisterRequest(loginRequest.userId, null, "kakao", nickname, null))
                        Log.d("FCMTOKEN", fcmtoken)
                    }
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse>, t: Throwable) {
                // 네트워크 등 기타 에러 처리
            }
        })
    }

    private fun getUserInfo(token: String) {
        val vectoService = VectoService.create()

        val call = vectoService.getUserInfo("Bearer $token")
        call.enqueue(object : Callback<VectoService.UserInfoResponse>{
            override fun onResponse(call: Call<VectoService.UserInfoResponse>, response: Response<VectoService.UserInfoResponse>) {
                if (response.isSuccessful) {
                    //가입 성공
                    Log.d("VectoRegister", "정보 조회 성공 : " + response.message())
                    Auth.setLoginFlag(true)
                    finish()
                } else {
                    // 서버 에러 처리
                    Log.d("VectoRegister", "정보 조회 실패 : " + response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<VectoService.UserInfoResponse>, t: Throwable) {
                Log.d("VectoRegister", "정보 조회 실패 : " + t.message)
            }
        })

    }

    private fun sendRegisterRequest(registerRequest: VectoService.RegisterRequest){
        val vectoService = VectoService.create()

        val call = vectoService.registerUser(registerRequest)
        call.enqueue(object : Callback<VectoService.VectoResponse>{
            override fun onResponse(call: Call<VectoService.VectoResponse>, response: Response<VectoService.VectoResponse>) {
                if (response.isSuccessful) {
                    //가입 성공
                    Log.d("VectoRegister", "회원가입 성공 : " + response.message())

                    sendLoginRequest(VectoService.LoginRequest(registerRequest.userId, null, fcmtoken))
                } else {
                    // 서버 에러 처리
                    Log.d("VectoRegister", "회원가입 실패 : " + response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<VectoService.VectoResponse>, t: Throwable) {
                // 네트워크 등 기타 에러 처리
            }
        })
    }


    fun getTokenFromSharedPref(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("fcm_pref", Context.MODE_PRIVATE)
        return sharedPreferences.getString("fcm_token", null)
    }

}