package com.shirou.shibamusic.helper.recyclerview

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class CustomLinearSnapHelper : LinearSnapHelper() {

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        if (layoutManager is LinearLayoutManager) {
            if (!needToDoSnap(layoutManager)) {
                return null
            }
        }
        return super.findSnapView(layoutManager)
    }

    fun needToDoSnap(linearLayoutManager: LinearLayoutManager): Boolean =
        linearLayoutManager.findFirstCompletelyVisibleItemPosition() != 0 &&
        linearLayoutManager.findLastCompletelyVisibleItemPosition() != linearLayoutManager.itemCount - 1
}
