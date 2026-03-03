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

class HistogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 120
    }

    var histogramData: IntArray = IntArray(256)

    // 拖动相关属性
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop // 防误触阈值

    // 可选：拖动回调
    var onPositionChanged: ((x: Float, y: Float) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val barWidth = w / 256f
        val max = histogramData.maxOrNull()?.toFloat() ?: 1f

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
                // 【关键修改 1】使用 rawX/rawY 记录手指在屏幕上的绝对位置
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                isDragging = false

                // 【关键修改 2】请求父容器不要拦截触摸事件
                // 防止被 RecyclerView/ScrollView 等拦截导致拖动卡顿
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // 【关键修改 3】始终使用 rawX/rawY 计算位移
                val dx = event.rawX - lastTouchX
                val dy = event.rawY - lastTouchY

                // 判断是否开始拖动（防误触）
                if (!isDragging && (dx * dx + dy * dy) > touchSlop * touchSlop) {
                    isDragging = true
                }

                if (isDragging) {
                    // 直接应用位移到 translation
                    translationX += dx
                    translationY += dy

                    // 更新上一次记录的绝对坐标
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY

                    constrainPositionInScreen()

                    onPositionChanged?.invoke(translationX, translationY)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                // 释放事件拦截
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 限制 View 的位置，使其不超出父容器（屏幕）范围
     */
    private fun constrainPositionInScreen() {
        val parentView = parent as? View ?: return
        val parentWidth = parentView.width.toFloat()
        val parentHeight = parentView.height.toFloat()

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // 计算当前 View 相对于父容器的实际坐标
        // left/top 是 View 的初始布局位置，translationX/Y 是当前的拖拽偏移量
        val currentLeft = left + translationX
        val currentTop = top + translationY
        val currentRight = currentLeft + viewWidth
        val currentBottom = currentTop + viewHeight

        // 检查并修正左右边界
        if (currentLeft < 0) {
            translationX = -left.toFloat()
        } else if (currentRight > parentWidth) {
            translationX = parentWidth - left - viewWidth
        }

        // 检查并修正上下边界
        if (currentTop < 0) {
            translationY = -top.toFloat()
        } else if (currentBottom > parentHeight) {
            translationY = parentHeight - top - viewHeight
        }
    }
}