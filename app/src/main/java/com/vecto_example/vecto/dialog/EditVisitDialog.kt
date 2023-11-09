package com.vecto_example.vecto.dialog

import android.app.Dialog
import android.content.Context
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.vecto_example.vecto.R

class EditVisitDialog(val context: Context) {
    private val dialog = Dialog(context, R.style.CustomDialog)
    var onOkButtonClickListener: ((String) -> Unit)? = null

    fun showDialog() {
        dialog.setContentView(R.layout.dialog_edit_visit)
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.show()

        val editText: EditText = dialog.findViewById(R.id.EdittextVisit)

        val okButton: ImageView = dialog.findViewById(R.id.ButtonImage)
        okButton.setOnClickListener {
            if(editText.text.isEmpty()){
                Toast.makeText(context, "방문지를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
            else {
                onOkButtonClickListener?.invoke(editText.text.toString())
                dialog.dismiss()
            }
        }


    }
}