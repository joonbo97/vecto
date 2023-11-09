package com.vecto_example.vecto.ui_bottom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vecto_example.vecto.databinding.FragmentMypageInquiryBinding

class MypageInquiryFragment : Fragment() {
    lateinit var binding: FragmentMypageInquiryBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageInquiryBinding.inflate(inflater, container, false)

        return binding.root
    }
}