package com.vecto_example.vecto.dialog

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import com.vecto_example.vecto.R

class ReportUserDialog (val context: Context) {
    private val dialog = Dialog(context, R.style.CustomDialog)
    var onOkButtonClickListener: ((Int, String?) -> Unit)? = null
    private var selectedOptionId: Int = -1 // Selected RadioButton ID

    fun showDialog() {
        dialog.setContentView(R.layout.dialog_report_user)
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(true)
        dialog.show()


        val okButton: ImageView = dialog.findViewById(R.id.ButtonImage)
        val radioGroupReport: RadioGroup = dialog.findViewById(R.id.radioGroupReport)
        val editText: EditText = dialog.findViewById(R.id.ReportContentEditText)
        val editBox: ImageView = dialog.findViewById(R.id.ReportContentBox)

        okButton.setOnClickListener {
            val detailContent = if (selectedOptionId == R.id.radioButton3) editText.text.toString() else null

            onOkButtonClickListener?.invoke(selectedOptionId, detailContent)
            dialog.dismiss()
        }

        radioGroupReport.setOnCheckedChangeListener{ group, checkedId  ->
            selectedOptionId = checkedId

            if(checkedId == R.id.radioButton3) {
                for (i in 0 until group.childCount) {
                    val radioButton = group.getChildAt(i) as RadioButton
                    if (radioButton.id != checkedId) {
                        radioButton.animate().alpha(0.0f).withEndAction {
                            radioButton.visibility = View.GONE
                            editBox.visibility = View.VISIBLE
                            editText.visibility = View.VISIBLE
                        }
                    }
                }



            }
            else{
                editBox.visibility = View.GONE
                editText.visibility = View.GONE
            }

        }

    }
}