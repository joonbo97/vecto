package com.example.vecto.dialog

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.widget.ImageView
import com.example.vecto.R

class DeletePostDialog(context: Context) {
    private val dialog = Dialog(context, R.style.CustomDialog)
    var onOkButtonClickListener: (() -> Unit)? = null
    var onNoButtonClickListener: (() -> Unit)? = null

    fun showDialog() {
        dialog.setContentView(R.layout.dialog_delete_post)
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.show()

        val OKButton: ImageView = dialog.findViewById(R.id.OKButtonImage)
        val NOButton: ImageView = dialog.findViewById(R.id.NOButtonImage)
        OKButton.setOnClickListener {
            onOkButtonClickListener?.invoke()
            dialog.dismiss()
        }

        NOButton.setOnClickListener{
            onNoButtonClickListener?.invoke()
            dialog.dismiss()
        }
    }
}