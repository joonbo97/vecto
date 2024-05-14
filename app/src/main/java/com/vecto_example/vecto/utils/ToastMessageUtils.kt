package com.vecto_example.vecto.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat.getString
import com.vecto_example.vecto.R
import com.vecto_example.vecto.ui.login.LoginActivity
import com.vecto_example.vecto.dialog.LoginRequestDialog

object ToastMessageUtils {
    private var toast: Toast? = null

    fun showToast(context: Context, message: String) {
        toast?.cancel()
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast?.show()
    }

    enum class ValueType {
        FEED, FOLLOW, FOLLOW_POST, FOLLOW_DELETE
    }

    fun errorMessageHandler(context: Context, type: String, message: String){

        if(message == "FAIL"){
            when(type){
                ValueType.FEED.name -> {
                    showToast(context, context.getString(R.string.get_feed_fail))
                }
                ValueType.FOLLOW.name -> {
                    showToast(context, context.getString(R.string.get_follow_relation_fail))
                }
                ValueType.FOLLOW_POST.name ->{
                    showToast(context, context.getString(R.string.post_follow_fail))
                }
                ValueType.FOLLOW_DELETE.name ->{
                    showToast(context, context.getString(R.string.delete_follow_fail))
                }
            }
        } else {
            showToast(context, context.getString(R.string.APIErrorToastMessage))
        }
    }
}