package com.shirou.shibamusic.helper.recyclerview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FastScrollbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var bubble: TextView? = null
    private lateinit var handle: View
    private var recyclerView: RecyclerView? = null
    private var height: Int = 0
    private var currentAnimator: ObjectAnimator? = null

    init {
        orientation = HORIZONTAL
        clipChildren = false
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            updateBubbleAndHandlePosition()
        }
    }

    interface BubbleTextGetter {
        fun getTextToShowInBubble(pos: Int): String
    }

    fun setViewsToUse(@LayoutRes layoutResId: Int, @IdRes bubbleResId: Int, @IdRes handleResId: Int) {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(layoutResId, this, true)
        bubble = findViewById(bubbleResId)
        bubble?.visibility = INVISIBLE
        handle = findViewById(handleResId)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        height = h
        updateBubbleAndHandlePosition()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x < handle.x - ViewCompat.getPaddingStart(handle)) return false
                currentAnimator?.cancel()
                if (bubble?.visibility == INVISIBLE) showBubble()
                handle.isSelected = true
            }
            MotionEvent.ACTION_MOVE -> {
                val y = event.y
                setBubbleAndHandlePosition(y)
                setRecyclerViewPosition(y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handle.isSelected = false
                hideBubble()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setRecyclerView(recyclerView: RecyclerView?) {
        if (this.recyclerView != recyclerView) {
            this.recyclerView?.removeOnScrollListener(onScrollListener)
            this.recyclerView = recyclerView
            recyclerView?.addOnScrollListener(onScrollListener)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        recyclerView?.removeOnScrollListener(onScrollListener)
        recyclerView = null
    }

    private fun setRecyclerViewPosition(y: Float) {
        recyclerView?.let { rv ->
            val adapter = rv.adapter
            val itemCount = adapter?.itemCount ?: 0

            val proportion = when {
                handle.y == 0f -> 0f
                handle.y + handle.height >= height - TRACK_SNAP_RANGE -> 1f
                else -> y / height.toFloat()
            }

            val targetPos = getValueInRange(0, itemCount - 1, (proportion * itemCount.toFloat()).toInt())

            (rv.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(targetPos, 0)

            val bubbleText = (adapter as? BubbleTextGetter)?.getTextToShowInBubble(targetPos) ?: ""

            bubble?.let { b ->
                b.text = bubbleText
                if (bubbleText.isNullOrEmpty()) {
                    hideBubble()
                } else if (b.visibility == INVISIBLE) {
                    showBubble()
                }
            }
        }
    }

    private fun getValueInRange(min: Int, max: Int, value: Int): Int = value.coerceIn(min, max)

    private fun updateBubbleAndHandlePosition() {
        if (bubble == null || handle.isSelected) return

        val rv = recyclerView ?: return

        val verticalScrollOffset = rv.computeVerticalScrollOffset()
        val verticalScrollRange = rv.computeVerticalScrollRange()
        val proportion = verticalScrollOffset.toFloat() / (verticalScrollRange.toFloat() - height)
        setBubbleAndHandlePosition(height * proportion)
    }

    private fun setBubbleAndHandlePosition(y: Float) {
        val handleHeight = handle.height
        handle.y = getValueInRange(0, height - handleHeight, (y - handleHeight / 2).toInt()).toFloat()

        bubble?.let { b ->
            val bubbleHeight = b.height
            b.y = getValueInRange(0, height - bubbleHeight - handleHeight / 2, (y - bubbleHeight).toInt()).toFloat()
        }
    }

    private fun showBubble() {
        val b = bubble ?: return
        b.visibility = VISIBLE
        currentAnimator?.cancel()
        currentAnimator = ObjectAnimator.ofFloat(b, "alpha", 0f, 1f).setDuration(BUBBLE_ANIMATION_DURATION.toLong())
        currentAnimator?.start()
    }

    private fun hideBubble() {
        val b = bubble ?: return
        currentAnimator?.cancel()
        currentAnimator = ObjectAnimator.ofFloat(b, "alpha", 1f, 0f).setDuration(BUBBLE_ANIMATION_DURATION.toLong())
        currentAnimator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                b.visibility = INVISIBLE
                currentAnimator = null
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                b.visibility = INVISIBLE
                currentAnimator = null
            }
        })
        currentAnimator?.start()
    }

    companion object {
        private const val BUBBLE_ANIMATION_DURATION = 100
        private const val TRACK_SNAP_RANGE = 5
    }
}
