package com.example.vecto.ui_bottom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.vecto.R
import com.example.vecto.databinding.FragmentWriteBinding

class WriteFragment : Fragment() {
    private lateinit var binding: FragmentWriteBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWriteBinding.inflate(inflater, container, false)

        return binding.root
    }
}