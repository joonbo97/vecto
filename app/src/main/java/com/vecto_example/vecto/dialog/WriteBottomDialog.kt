package com.vecto_example.vecto.dialog

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vecto_example.vecto.data.model.VisitData
import com.vecto_example.vecto.ui.write.adapter.MyWriteCourseAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.vecto_example.vecto.R

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

        myWriteCourseAdapter = MyWriteCourseAdapter()
        recyclerView = dialog.findViewById(R.id.WriteBottomRecyclerView)!!

        myWriteCourseAdapter = MyWriteCourseAdapter().apply {
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