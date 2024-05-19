package com.vecto_example.vecto.ui.login

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.retrofit.VectoService
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.model.KakaoSdkError
import com.kakao.sdk.user.UserApiClient
import com.vecto_example.vecto.data.repository.TokenRepository
import com.vecto_example.vecto.ui.register.RegisterActivity
import com.vecto_example.vecto.data.repository.UserRepository
import com.vecto_example.vecto.databinding.ActivityLoginBinding
import com.vecto_example.vecto.utils.SaveLoginDataUtils
import com.vecto_example.vecto.utils.ToastMessageUtils

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(UserRepository(VectoService.create()), TokenRepository(VectoService.create()))
    }

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
                val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
                sharedPreferences.edit().putString("FCM", token).apply()
            } else {
                Log.w("FCM Token", "Fetching FCM registration token failed", task.exception)
            }
        }

        initObservers()
        initListeners()
    }

    private fun initListeners() {
        binding.LoginTextForRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java) //Register 화면으로 이동
            startActivity(intent)
        }

        binding.LoginButton.setOnClickListener {
            val userId = binding.editTextID.text.toString()
            val userPw = binding.editTextPW.text.toString()

            loginViewModel.userId = userId
            fcmtoken = getTokenFromSharedPref(this).toString()
            loginViewModel.loginRequest(VectoService.LoginRequest(userId, userPw, fcmtoken))
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

                                            loginViewModel.nickname = user.kakaoAccount?.profile?.nickname.toString()
                                            loginViewModel.userId = user.id.toString()
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
        loginViewModel.isLoading.observe(this){
            if(it){
                binding.constraintProgress.visibility = View.VISIBLE
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.constraintProgress.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
            }
        }

        loginViewModel.loginResult.observe(this){
            Auth.setToken(it)

            SaveLoginDataUtils.saveLoginInformation(this, loginViewModel.userId!!, provider, fcmtoken)

            loginViewModel.getUserInfo()
        }

        loginViewModel.registerResult.observe(this){
            loginViewModel.loginRequest(VectoService.LoginRequest(loginViewModel.loginRequestData.userId, null, fcmtoken))
        }

        loginViewModel.userInfoResult.observe(this) {
            Auth.setLoginFlag(true)

            Auth.setUserData(it.provider, it.userId, it.profileUrl, it.nickName, it.email)
            finish()
        }

        loginViewModel.errorMessage.observe(this) {
            ToastMessageUtils.showToast(this, getString(it))
        }
    }

    private val mCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        if (error != null) {
            Log.e("TAG", "로그인 실패 $error")
        } else if (token != null) {
            Log.e("TAG", "로그인 성공 ${token.accessToken}")
        }
    }


    private fun getTokenFromSharedPref(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("FCM", null)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {

        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

        return super.dispatchTouchEvent(ev)
    }
}