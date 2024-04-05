package com.vecto_example.vecto.utils

import android.content.Context
import android.content.Intent
import com.vecto_example.vecto.LoginActivity
import com.vecto_example.vecto.dialog.LoginRequestDialog

class RequestLoginUtils {
    companion object {
        fun requestLogin(context: Context){
            val loginRequestDialog = LoginRequestDialog(context)
            loginRequestDialog.showDialog()
            loginRequestDialog.onOkButtonClickListener = {
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
            }
        }

    }
}