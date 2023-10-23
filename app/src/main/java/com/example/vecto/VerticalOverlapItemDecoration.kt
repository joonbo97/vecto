package com.example.vecto

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalOverlapItemDecoration(private val overlapHeight: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View,
        parent: RecyclerView, state: RecyclerView.State
    ) {
        if (parent.getChildAdapterPosition(view) != 0) { // 첫번째 아이템은 제외
            outRect.top = -overlapHeight // 아이템들이 위쪽으로 겹치도록
        }
    }
}
