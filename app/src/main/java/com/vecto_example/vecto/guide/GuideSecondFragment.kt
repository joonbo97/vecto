package com.vecto_example.vecto.guide

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vecto_example.vecto.databinding.FragmentGuideSecondBinding

class GuideSecondFragment : Fragment() {
    private lateinit var binding: FragmentGuideSecondBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGuideSecondBinding.inflate(inflater, container, false)



        binding.NextButton.setOnClickListener {
            (activity as? GuideActivity)?.moveToNextFragment()
        }



        return binding.root
    }
}