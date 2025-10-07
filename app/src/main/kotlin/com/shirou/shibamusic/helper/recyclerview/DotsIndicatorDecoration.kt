package com.shirou.shibamusic.helper.recyclerview

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ceil

class DotsIndicatorDecoration(
    private val radius: Int,
    private val indicatorItemPadding: Int,
    private val indicatorHeight: Int,
    @ColorInt colorInactive: Int,
    @ColorInt colorActive: Int
) : RecyclerView.ItemDecoration() {

    private val inactivePaint = Paint().apply {
        strokeCap = Paint.Cap.ROUND
        strokeWidth = Resources.getSystem().displayMetrics.density * 1
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = colorInactive
    }

    private val activePaint = Paint().apply {
        strokeCap = Paint.Cap.ROUND
        strokeWidth = Resources.getSystem().displayMetrics.density * 1
        style = Paint.Style.FILL
        isAntiAlias = true
        color = colorActive
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val adapter = parent.adapter ?: return

        val itemCount = ceil(adapter.itemCount.toDouble() / 5).toInt()

        if (itemCount <= 1) {
            return
        }

        // center horizontally, calculate width and subtract half from center
        val totalLength = radius * 2 * itemCount
        val paddingBetweenItems = (itemCount - 1).coerceAtLeast(0) * indicatorItemPadding
        val indicatorTotalWidth = totalLength + paddingBetweenItems
        val indicatorStartX = (parent.width - indicatorTotalWidth) / 2f

        // center vertically in the allotted space
        val indicatorPosY = parent.height - indicatorHeight - indicatorItemPadding / 4f

        drawInactiveDots(c, indicatorStartX, indicatorPosY, itemCount)

        val layoutManager = parent.layoutManager ?: return

        val activePosition: Int = when (layoutManager) {
            is GridLayoutManager -> layoutManager.findFirstVisibleItemPosition()
            is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition()
            else -> return // not supported layout manager
        }

        if (activePosition == RecyclerView.NO_POSITION) {
            return
        }

        // find offset of active page if the user is scrolling
        val activeChild = layoutManager.findViewByPosition(activePosition)
        if (activeChild == null) {
            return
        }

        drawActiveDot(c, indicatorStartX, indicatorPosY, activePosition)
    }

    private fun drawInactiveDots(c: Canvas, indicatorStartX: Float, indicatorPosY: Float, itemCount: Int) {
        // width of item indicator including padding
        val itemWidth = radius * 2 + indicatorItemPadding

        var start = indicatorStartX + radius.toFloat()
        for (i in 0 until itemCount) {
            c.drawCircle(start, indicatorPosY, radius.toFloat(), inactivePaint)
            start += itemWidth
        }
    }

    private fun drawActiveDot(c: Canvas, indicatorStartX: Float, indicatorPosY: Float, highlightPosition: Int) {
        // width of item indicator including padding
        val itemWidth = radius * 2 + indicatorItemPadding
        val highlightStart = ceil(indicatorStartX + radius + itemWidth * highlightPosition / 5f)
        c.drawCircle(highlightStart, indicatorPosY, radius.toFloat(), activePaint)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.bottom = indicatorHeight
    }
}
