package com.example.vecto.ui_bottom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import com.example.vecto.R
import com.example.vecto.databinding.FragmentMypagePostBinding
import com.example.vecto.databinding.FragmentMypageSettingBinding

class MypagePostFragment : Fragment() {
    lateinit var binding: FragmentMypagePostBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypagePostBinding.inflate(inflater, container, false)

        return binding.root
    }
}