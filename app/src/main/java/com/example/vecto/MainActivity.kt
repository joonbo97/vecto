package com.example.vecto

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.vecto.databinding.ActivityMainBinding
import com.example.vecto.editlocation.EditLocationActivity
import com.example.vecto.guide.GuideActivity
import com.example.vecto.ui_bottom.EditCourseFragment
import com.example.vecto.ui_bottom.MypageFragment
import com.example.vecto.ui_bottom.SearchFragment
import com.example.vecto.ui_bottom.TodayCourseFragment
import com.example.vecto.ui_bottom.WriteFragment


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.EditMapBtn.setOnClickListener{
            val intent = Intent(this, EditLocationActivity::class.java) //EditLocation 화면으로 이동
            //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.GuideBtn.setOnClickListener {
            val intent = Intent(this, GuideActivity::class.java) //Guide 화면으로 이동
            //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
        navView.itemIconTintList = null


    }
}