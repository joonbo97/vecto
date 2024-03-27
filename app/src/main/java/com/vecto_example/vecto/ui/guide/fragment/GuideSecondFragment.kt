package com.vecto_example.vecto.ui.guide.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vecto_example.vecto.databinding.FragmentGuideSecondBinding
import com.vecto_example.vecto.ui.guide.activity.GuideActivity

class GuideSecondFragment : Fragment() {
    /*   절전 모드 해제 안내를 위한 Guide Fragment   */

    private lateinit var binding: FragmentGuideSecondBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGuideSecondBinding.inflate(inflater, container, false)



        binding.NextButton.setOnClickListener {
            (activity as? GuideActivity)?.moveToNextFragment()
        }



        return binding.root
    }
}