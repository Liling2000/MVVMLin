package com.jiyi.power.ui.mobilepower

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @date 2023/6/1
 * @description
 */
class TimeSetGridItemDecoration(var columnSpace : Int, var rowSpace : Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = rowSpace

//        if(AppState.isRtl()){
//            if (parent.getChildLayoutPosition(view) % 2 == 0) {
//                outRect.left = columnSpace
//            } else {
//                outRect.left = 0
//            }
//        }else{
//            if (parent.getChildLayoutPosition(view) % 2 == 0) {
//                outRect.right = columnSpace
//            } else {
//                outRect.right = 0
//            }
//        }
    }
}