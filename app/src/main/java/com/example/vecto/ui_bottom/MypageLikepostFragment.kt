package com.example.vecto.ui_bottom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import com.example.vecto.R
import com.example.vecto.databinding.FragmentMypageLikepostBinding

class MypageLikepostFragment : Fragment() {
    lateinit var binding: FragmentMypageLikepostBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageLikepostBinding.inflate(inflater, container, false)

        return binding.root
    }

}