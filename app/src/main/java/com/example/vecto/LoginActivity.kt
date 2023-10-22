package com.example.vecto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.vecto.databinding.ActivityLoginBinding
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.model.KakaoSdkError
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
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

                                            Toast.makeText(this, user.kakaoAccount?.email, Toast.LENGTH_LONG).show()
                                            /*binding.txtNickName.text = user.kakaoAccount?.profile?.nickname
                                            binding.txtAge.text = user.kakaoAccount?.ageRange.toString()
                                            binding.txtEmail.text = user.kakaoAccount?.email*/
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
}