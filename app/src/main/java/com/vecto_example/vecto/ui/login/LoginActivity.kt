package com.vecto_example.vecto.ui.login

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.model.KakaoSdkError
import com.kakao.sdk.user.UserApiClient
import com.vecto_example.vecto.BuildConfig
import com.vecto_example.vecto.R
import com.vecto_example.vecto.ui.register.RegisterActivity
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(UserRepository(VectoService.create()))
    }

    private lateinit var nickname: String
    private lateinit var fcmtoken: String

    var provider = "vecto"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*FCM 토큰 얻기*/
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.i("FCM Token", token)
                val sharedPreferences = getSharedPreferences("fcm_pref", Context.MODE_PRIVATE)
                sharedPreferences.edit().putString("fcm_token", token).apply()
            } else {
                Log.w("FCM Token", "Fetching FCM registration token failed", task.exception)
            }
        }


        KakaoSdk.init(this, BuildConfig.KAKAO_KEY)
        initObservers()
        initListeners()
    }

    private fun initListeners() {
        binding.LoginTextForRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java) //Register 화면으로 이동
            startActivity(intent)
        }

        binding.LoginButton.setOnClickListener {
            val userid = binding.editTextID.text.toString()
            val userpw = binding.editTextPW.text.toString()

            fcmtoken = getTokenFromSharedPref(this).toString()
            loginViewModel.loginRequest(VectoService.LoginRequest(userid, userpw, fcmtoken))
            provider = "vecto"
        }

        binding.LoginBoxKakao.setOnClickListener {
            provider = "kakao"

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

                                        //로그인 필요1

                                    }
                                    else {
                                        UserApiClient.instance.loginWithKakaoAccount(this, callback = mCallback)
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

                                            loginViewModel.loginRequest(VectoService.LoginRequest(user.id.toString(), null, fcmtoken))
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

    private fun initObservers() {

        loginViewModel.loginResult.observe(this){ loginResult ->
            loginResult.onSuccess {
                Auth.token = it

                saveLoginInformation(loginViewModel.loginRequestData.userId,
                    loginViewModel.loginRequestData.userPw,
                    provider)

                loginViewModel.getUserInfo()
            }.onFailure {
                if(it.message == "FAIL"){

                    if(provider == "vecto"){    //vecto 로그인 인 경우
                        Toast.makeText(this, "아이디 혹은 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                    else{                       //kakao 로그인 인 경우
                        loginViewModel.registerRequest(VectoService.RegisterRequest(
                            loginViewModel.loginRequestData.userId, null, provider, nickname, null, null
                        ))
                    }
                }
                else{
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }

        loginViewModel.registerResult.observe(this){ registerResult ->
            registerResult.onSuccess {
                loginViewModel.loginRequest(VectoService.LoginRequest(loginViewModel.loginRequestData.userId, null, fcmtoken))
            }
                .onFailure {
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }
        }

        loginViewModel.userInfoResult.observe(this) { userInfoResult ->
            userInfoResult.onSuccess {
                Auth.setLoginFlag(true)

                Auth.setUserData(it.provider, it.userId, it.profileUrl, it.nickName, it.email)
                finish()
            }.onFailure {
                if(it.message == "E020"){
                    Toast.makeText(this, "사용자 정보가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this, getText(R.string.APIErrorToastMessage), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveLoginInformation(userId: String, password: String?, provider: String) {
        val sharedPreferences = this.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        // 기존 정보 삭제
        editor.remove("userId")
        editor.remove("password")
        editor.remove("provider")
        editor.apply()

        // 새로운 정보 저장
        editor.putString("username", userId)
        editor.putString("password", password)
        editor.putString("provider", provider)
        if (!sharedPreferences.contains("FCM")) {
            editor.putString("FCM", fcmtoken)
        }
        editor.apply()
    }

    private val mCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            Log.e("TAG", "로그인 실패 $error")
        } else if (token != null) {
            Log.e("TAG", "로그인 성공 ${token.accessToken}")
        }
    }


    private fun getTokenFromSharedPref(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("fcm_pref", Context.MODE_PRIVATE)
        return sharedPreferences.getString("fcm_token", null)
    }


}