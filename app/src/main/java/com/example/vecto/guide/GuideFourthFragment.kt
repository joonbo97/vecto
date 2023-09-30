package com.example.vecto.guide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.vecto.editlocation.EditLocationActivity
import com.example.vecto.databinding.FragmentGuideFourthBinding

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

        binding.NextButton.setOnClickListener {
            val permission: Array<String> = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_DENIED)
            {
                ActivityCompat.requestPermissions(requireActivity(), permission, BACKGROUND_PERMISSION_REQUEST_CODE)
            }
            else {
                val intent =
                    Intent(context, EditLocationActivity::class.java) //EditLoaction 화면으로 이동
                startActivity(intent)
                requireActivity().finish()
            }
        }

        return binding.root
    }
}