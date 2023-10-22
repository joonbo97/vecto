package com.example.vecto.dialog

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.widget.ImageView
import com.example.vecto.R

class StartServiceDialog(context: Context) {
    private val dialog = Dialog(context, R.style.CustomDialog)
    var onOkButtonClickListener: (() -> Unit)? = null


    fun showDialog() {
        dialog.setContentView(R.layout.dialog_start_service)
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.show()

        val Button: ImageView = dialog.findViewById(R.id.ButtonImage)
        Button.setOnClickListener {
            onOkButtonClickListener?.invoke()
            dialog.dismiss()
        }


    }
}


