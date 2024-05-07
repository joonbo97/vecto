package com.vecto_example.vecto.ui.inquiry

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vecto_example.vecto.databinding.ActivityPolicyBinding

class PolicyActivity : AppCompatActivity() {
    lateinit var binding: ActivityPolicyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webView.loadUrl("https://vec-to.net/privacy-policy")
    }
}