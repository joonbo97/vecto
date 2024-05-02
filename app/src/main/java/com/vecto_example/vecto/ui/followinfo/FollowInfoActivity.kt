package com.vecto_example.vecto.ui.followinfo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.material.tabs.TabLayoutMediator
import com.vecto_example.vecto.R
import com.vecto_example.vecto.data.Auth
import com.vecto_example.vecto.databinding.ActivityFollowInfoBinding
import com.vecto_example.vecto.ui.followinfo.adapter.FollowViewPagerAdapter

class FollowInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFollowInfoBinding

    var userId = ""
    var follower = 0
    var following = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFollowInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        follower = intent.getIntExtra("follower", 0)
        following = intent.getIntExtra("following", 0)

        intent.getStringExtra("userId")?.let {
            userId = it
        }

        intent.getStringExtra("nickName")?.let {
            binding.TitleNicknameText.text = it
        }

        intent.getStringExtra("type")?.let {
            initUI(it)
        }


    }

    private fun initUI(type: String) {
        binding.followViewPager.adapter = FollowViewPagerAdapter(this)

        TabLayoutMediator(binding.followTabLayout, binding.followViewPager) { tab, position ->
            when(position){
                0 -> {
                    tab.text = "팔로워 ${follower}명"
                }

                1 -> {
                    tab.text = "팔로잉 ${following}명"
                }
            }

        }.attach()

        // 초기 탭 설정
        when (type) {
            "follower" -> {
                val tab = binding.followTabLayout.getTabAt(0) // 팔로워 탭
                if (tab != null) {
                    binding.followTabLayout.selectTab(tab)
                }
            }
            "following" -> {
                val tab = binding.followTabLayout.getTabAt(1) // 팔로잉 탭
                if (tab != null) {
                    binding.followTabLayout.selectTab(tab)
                }
            }
        }
    }

    fun getUserIdValue(): String {
        return userId
    }
}