package com.vecto_example.vecto.popupwindow

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.R
import com.vecto_example.vecto.ui_bottom.MyPlaceAdapter

class PlacePopupWindow (
    private val context: Context
) {
    private var popupWindow: PopupWindow? = null

    fun showPopupWindow(anchorView: View, data: List<String>, onItemClick: ((String) -> Unit)? = null) {
        // 레이아웃 인플레이션
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.place_infowindow, null)
        val adapter = MyPlaceAdapter(context) { name ->
            // 아이템 클릭 시 처리할 로직
            onItemClick?.invoke(name)
            dismiss() // 팝업 윈도우 닫기
        }


        // RecyclerView 설정
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.PlaceRecyclerView)
        recyclerView.adapter = adapter

        // 어댑터에 데이터 설정
        adapter.nameList.clear()
        adapter.nameList.addAll(data)
        adapter.notifyDataSetChanged()

        // 팝업 윈도우 생성 및 표시
        popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = 10f
            isOutsideTouchable = true
            showAsDropDown(anchorView)
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
    }
}