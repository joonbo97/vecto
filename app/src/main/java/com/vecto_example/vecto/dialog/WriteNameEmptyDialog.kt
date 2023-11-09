package com.vecto_example.vecto.dialog

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.widget.ImageView
import com.vecto_example.vecto.R

class WriteNameEmptyDialog(context: Context) {
    private val dialog = Dialog(context, R.style.CustomDialog)
    var onOkButtonClickListener: (() -> Unit)? = null

    fun showDialog() {
        dialog.setContentView(R.layout.dialog_wite_name_empty)
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