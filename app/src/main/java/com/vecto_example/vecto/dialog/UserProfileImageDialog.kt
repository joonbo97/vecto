package com.vecto_example.vecto.dialog

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.widget.ImageView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.utils.LoadImageUtils

class UserProfileImageDialog(private val context: Context, private val profileUrl: String?) {
    private val dialog = Dialog(context, R.style.CustomDialog)

    fun showDialog() {
        dialog.setContentView(R.layout.dialog_profile_image)
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        LoadImageUtils.loadUserProfileImage(context, dialog.findViewById(R.id.ProfileImage), profileUrl)

        dialog.show()
    }
}