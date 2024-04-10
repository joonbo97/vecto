package com.vecto_example.vecto.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import android.widget.TextView
import com.vecto_example.vecto.R

class EditDeletePopupWindow (
    private val context: Context,
    private val editListener: () -> Unit,
    private val deleteListener: () -> Unit,
    private val dismissListener: () -> Unit
) {
    private var popupWindow: PopupWindow? = null

    @SuppressLint("InflateParams")
    fun showPopupWindow(anchorView: View) {
        // 레이아웃 인플레이션
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popupwindow_edit_delete, null)

        // 팝업 윈도우 생성
        popupWindow = PopupWindow(popupView, WRAP_CONTENT, WRAP_CONTENT, true).apply {
            elevation = 10f
            isOutsideTouchable = true // 팝업 외부 터치 시 팝업 닫기 활성화

            // 텍스트 뷰 클릭 리스너 설정
            popupView.findViewById<TextView>(R.id.text_edit).setOnClickListener {
                editListener()
                dismiss()
            }
            popupView.findViewById<TextView>(R.id.text_delete).setOnClickListener {
                deleteListener()
                dismiss()
            }

            setOnDismissListener {
                dismissListener()
            }

            // 팝업 윈도우 표시
            showAsDropDown(anchorView)
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
    }
}