package com.nano71.cameramonitor.feature.streamer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.graphics.toColorInt

class HistogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 180
    }

    var histogramData: IntArray = IntArray(256)

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        visibility = GONE
        // 设置半透明黑色背景，方便观察
        setBackgroundColor("#66000000".toColorInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val barWidth = w / 256f
        val max = histogramData.maxOrNull()?.let { if (it == 0) 1f else it.toFloat() } ?: 1f

        for (i in 0 until 256) {
            val value = histogramData[i] / max
            val barHeight = value * h
            canvas.drawLine(
                i * barWidth,
                h,
                i * barWidth,
                h - barHeight,
                paint
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                isDragging = false

                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastTouchX
                val dy = event.rawY - lastTouchY

                if (!isDragging && (dx * dx + dy * dy) > touchSlop * touchSlop) {
                    isDragging = true
                }

                if (isDragging) {
                    translationX += dx
                    translationY += dy

                    lastTouchX = event.rawX
                    lastTouchY = event.rawY

                    constrainPositionInScreen()

                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun constrainPositionInScreen() {
        val parentView = parent as? View ?: return
        val parentWidth = parentView.width.toFloat()
        val parentHeight = parentView.height.toFloat()

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val currentLeft = left + translationX
        val currentTop = top + translationY
        val currentRight = currentLeft + viewWidth
        val currentBottom = currentTop + viewHeight

        if (currentLeft < 0) {
            translationX = -left.toFloat()
        } else if (currentRight > parentWidth) {
            translationX = parentWidth - left - viewWidth
        }

        if (currentTop < 0) {
            translationY = -top.toFloat()
        } else if (currentBottom > parentHeight) {
            translationY = parentHeight - top - viewHeight
        }
    }
}
