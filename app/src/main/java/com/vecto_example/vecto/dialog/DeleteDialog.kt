package com.vecto_example.vecto.dialog

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.vecto_example.vecto.R

class DeleteDialog(context: Context, val type: Int) {
    companion object {
        const val VISIT = 0
        const val FEED = 1
        const val ACCOUNT = 2
    }

    private val dialog = Dialog(context, R.style.CustomDialog)
    var onOkButtonClickListener: (() -> Unit)? = null
    var onNoButtonClickListener: (() -> Unit)? = null

    fun showDialog() {
        dialog.setContentView(R.layout.dialog_delete)
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.show()

        when(type){
            VISIT -> {
                dialog.findViewById<TextView>(R.id.Guide_TextView).setText(R.string.delete_visit_dialog)
            }

            FEED -> {
                dialog.findViewById<TextView>(R.id.Guide_TextView).setText(R.string.delete_feed_dialog)
            }

            ACCOUNT -> {
                dialog.findViewById<TextView>(R.id.Guide_TextView).setText(R.string.delete_account_dialog)
            }
        }

        dialog.findViewById<ImageView>(R.id.OKButtonImage).setOnClickListener {
            onOkButtonClickListener?.invoke()
            dialog.dismiss()
        }

        dialog.findViewById<ImageView>(R.id.NOButtonImage).setOnClickListener{
            onNoButtonClickListener?.invoke()
            dialog.dismiss()
        }
    }
}