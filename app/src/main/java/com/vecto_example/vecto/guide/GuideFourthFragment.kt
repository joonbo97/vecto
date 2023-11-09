package com.vecto_example.vecto.guide

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vecto_example.vecto.R
import com.vecto_example.vecto.databinding.FragmentGuideFourthBinding

class GuideFourthFragment : Fragment() {
    private lateinit var binding: FragmentGuideFourthBinding

    companion object {
        private const val BACKGROUND_PERMISSION_REQUEST_CODE = 2000
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGuideFourthBinding.inflate(inflater, container, false)

        //글자색 변경
        val text = binding.GuideTextView
        val guidetext = getString(R.string.GuideText4)
        val spannable = SpannableStringBuilder(guidetext)
        val start = guidetext.indexOf("위치권한을 항상 허용")
        val end = start + "위치권한을 항상 허용".length

        spannable.setSpan(ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.text =spannable

        binding.NextButton.setOnClickListener {
            val permission: Array<String> = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_DENIED)
            {
                ActivityCompat.requestPermissions(requireActivity(), permission, BACKGROUND_PERMISSION_REQUEST_CODE)
            }
            else {
                requireActivity().finish()
            }
        }

        return binding.root
    }
}