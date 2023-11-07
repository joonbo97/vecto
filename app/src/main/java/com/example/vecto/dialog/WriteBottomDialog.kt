package com.example.vecto.dialog

import android.content.Context
import android.view.Gravity
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vecto.R
import com.example.vecto.data.VisitData
import com.example.vecto.ui_bottom.MyWriteCourseAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog

class WriteBottomDialog(private val context: Context) {
    private val dialog = BottomSheetDialog(context, R.style.CustomBottomDialog)
    private lateinit var recyclerView: RecyclerView
    private lateinit var myWriteCourseAdapter: MyWriteCourseAdapter


    fun showDialog(visitData: MutableList<VisitData>, onItemsSelected: (List<VisitData>) -> Unit) {
        dialog.setContentView(R.layout.dialog_write_bottom)
        val window = dialog.window
        if (window != null) {
            window.attributes.windowAnimations = R.style.CustomBottomDialog
        }

        myWriteCourseAdapter = MyWriteCourseAdapter(context)
        recyclerView = dialog.findViewById(R.id.WriteBottomRecyclerView)!!

        myWriteCourseAdapter = MyWriteCourseAdapter(context).apply {
            myVisit = visitData
            notifyDataSetChanged()

            // 선택된 아이템들을 리스너를 통해 반환
            onItemsSelectedListener = { selectedItems ->
                // 선택된 아이템들 처리
                onItemsSelected(selectedItems)
                dialog.dismiss() // 선택 후 다이얼로그 종료
            }
        }

        recyclerView.adapter = myWriteCourseAdapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(true)
        dialog.show()
    }
}