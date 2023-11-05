package com.example.vecto

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.vecto.databinding.ActivityUserInfoBinding

class UserInfoActivity : AppCompatActivity() {
    lateinit var binding: ActivityUserInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = intent.getStringExtra("userId")
        //TODO userId를 통해 정보를 받아 표시해주어야함
        //받아야 하는 정보
        //TODO 총 게시글 개수, 팔로워 수, 팔로잉 수
        //TODO 나와 팔로워 여부
        //TODO 작성자가 게시한 글 정보. (FeedInfo 정보와 동일한 정보)
    }
}