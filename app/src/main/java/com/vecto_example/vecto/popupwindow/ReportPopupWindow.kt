package com.vecto_example.vecto.popupwindow

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.vecto_example.vecto.R

class ReportPopupWindow (
    private val context: Context,
    private val reportListener: () -> Unit,
) {
    private var popupWindow: PopupWindow? = null

    fun showPopupWindow(anchorView: View) {
        // 레이아웃 인플레이션
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popupwindow_report, null)

        // 팝업 윈도우 생성
        popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = 10f
            isOutsideTouchable = true // 팝업 외부 터치 시 팝업 닫기 활성화

            // 텍스트 뷰 클릭 리스너 설정
            popupView.findViewById<ImageView>(R.id.reportTouchImage).setOnClickListener {
                reportListener()
                dismiss()
            }

            // 팝업 윈도우 표시
            showAsDropDown(anchorView)
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
    }
}