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

class MypageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if(!Auth.loginFlag.value!!){
            goLogin()
        }

        return inflater.inflate(R.layout.fragment_mypage, container, false)
    }

    private fun goLogin(){
        val intent = Intent(context, LoginActivity::class.java) //Login 화면으로 이동
        startActivity(intent)
    }
}