package com.vecto_example.vecto.ui.guide.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vecto_example.vecto.databinding.FragmentGuideFirstBinding
import com.vecto_example.vecto.ui.guide.activity.GuideActivity

class GuideFirstFragment : Fragment() {
    /*   위치 권한 요청 안내를 위한 Guide Fragment   */

    private lateinit var binding: FragmentGuideFirstBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGuideFirstBinding.inflate(inflater, container, false)


        val permission: Array<String> = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        //필요한 permission array

        binding.NextButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED
                ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
            //권한이 하나 라도 없으면 권한에 대한 허가 요청
            {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    permission,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
            else
            {
                (activity as? GuideActivity)?.moveToNextFragment()
            }


        }


        return binding.root
    }

    companion object{
        const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}