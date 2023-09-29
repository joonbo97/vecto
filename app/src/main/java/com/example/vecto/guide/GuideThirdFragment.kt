package com.example.vecto.guide

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.vecto.R
import com.example.vecto.databinding.FragmentGuideThirdBinding

class GuideThirdFragment : Fragment() {
    private lateinit var binding: FragmentGuideThirdBinding
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1500
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGuideThirdBinding.inflate(inflater, container, false)

        val permission: Array<String> = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        //필요한 permission array

        binding.NextButton.setOnClickListener {

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED)
            {
                ActivityCompat.requestPermissions(requireActivity(), permission, NOTIFICATION_PERMISSION_REQUEST_CODE) //권한이 하나 라도 없으면 권한에 대한 허가 요청
            }
            else
            {
                (activity as? GuideActivity)?.moveToNextFragment()
            }

        }

        return binding.root
    }
}