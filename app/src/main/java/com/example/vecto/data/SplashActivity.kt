package com.example.vecto.data

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.vecto.MainActivity
import com.example.vecto.R

class SplashActivity : AppCompatActivity() {
    private val splashTimeOut: Long = 2000 // 스플래시 화면 지속 시간을 2초로 설정

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // 2초 후에 MainActivity로 이동하고 현재 액티비티는 종료합니다.
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, splashTimeOut)
    }
}