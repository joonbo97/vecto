package com.vecto_example.vecto.dialog

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.widget.ImageView
import com.vecto_example.vecto.R

class LoginRequestDialog(context: Context) {
    private val dialog = Dialog(context, R.style.CustomDialog)
    var onOkButtonClickListener: (() -> Unit)? = null

    fun showDialog() {
        dialog.setContentView(R.layout.dialog_login_require)
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.show()

        val OKButton: ImageView = dialog.findViewById(R.id.ButtonImage)
        OKButton.setOnClickListener {
            onOkButtonClickListener?.invoke()
            dialog.dismiss()
        }
    }
}