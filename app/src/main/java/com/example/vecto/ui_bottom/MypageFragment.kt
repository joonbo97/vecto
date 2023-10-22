package com.example.vecto.ui_bottom

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.vecto.LoginActivity
import com.example.vecto.R
import com.example.vecto.data.Auth
import com.example.vecto.databinding.FragmentMypageBinding

class MypageFragment : Fragment() {
    lateinit var binding: FragmentMypageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageBinding.inflate(inflater, container, false)

        if(!Auth.loginFlag.value!!){
            goLogin()
        }

        return binding.root
    }

    private fun goLogin(){
        val intent = Intent(context, LoginActivity::class.java) //Login 화면으로 이동
        startActivity(intent)
    }
}