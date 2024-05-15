package com.vecto_example.vecto.utils

import android.content.Context
import android.widget.Toast
import com.vecto_example.vecto.R

object ToastMessageUtils {
    private var toast: Toast? = null

    fun showToast(context: Context, message: String) {
        toast?.cancel()
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast?.show()
    }

    enum class UserInterActionType {
        FEED, FOLLOW, FOLLOW_POST, FOLLOW_DELETE
    }

    enum class FeedInterActionType {
        LIKE_POST, LIKE_DELETE
    }

    fun errorMessageHandler(context: Context, type: String, message: String){

        if(message == "FAIL"){
            when(type){
                UserInterActionType.FEED.name -> {
                    showToast(context, context.getString(R.string.get_feed_fail))
                }
                UserInterActionType.FOLLOW.name -> {
                    showToast(context, context.getString(R.string.get_follow_relation_fail))
                }
                UserInterActionType.FOLLOW_POST.name ->{
                    showToast(context, context.getString(R.string.post_follow_fail))
                }
                UserInterActionType.FOLLOW_DELETE.name ->{
                    showToast(context, context.getString(R.string.delete_follow_fail))
                }
            }
        } else {
            showToast(context, context.getString(R.string.APIErrorToastMessage))
        }
    }

    fun followMessageHandler(context: Context, type: String, message: String, nickName: String){

    }
}