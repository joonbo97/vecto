package com.example.vecto

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vecto.databinding.ActivityMainBinding
import com.example.vecto.guide.GuideActivity


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

    }
}