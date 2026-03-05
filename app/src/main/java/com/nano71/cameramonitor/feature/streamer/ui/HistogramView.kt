package com.nano71.cameramonitor.feature.streamer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.graphics.toColorInt
import com.nano71.cameramonitor.core.usb.UsbMonitor
import kotlin.math.ln
import kotlin.math.min

class HistogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var maxVal = 0

    init {
        visibility = GONE
        // 设置半透明黑色背景，方便观察
        setBackgroundColor("#66000000".toColorInt())
    }
    fun setFrameSize(frameWidth: Int, frameHeight: Int){
        maxVal = (frameWidth * frameHeight) / 16 / 40
    }
    private fun filterOutliers(data: IntArray): IntArray {
        val result = data.copyOf()
        val thresholdMultiplier = 5.0 // 阈值系数，倍数越高越宽松

        for (i in 1 until data.size - 1) {
            val left = data[i - 1]
            val right = data[i + 1]
            val current = data[i]

            // 计算邻域平均值（避免除以零）
            val neighborAvg = (left + right) / 2.0

            // 如果当前值远大于邻居平均值，且达到一定量级，则判定为“离谱”
            if (neighborAvg > 0 && current > neighborAvg * thresholdMultiplier) {
                result[i] = neighborAvg.toInt()
            } else if (neighborAvg <= 0 && current > 100) {
                // 针对周围都是0，突然跳出一个大值的情况
                result[i] = 0
            }
        }
        return result
    }
    private var histogramData = IntArray(256)
    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun updateHistogram(newData: IntArray) {
        if (newData.size != 256) return
        val filteredData = filterOutliers(newData)

        histogramData = filteredData
        histogramData
            .mapIndexed { index, freq -> index to freq }
            .sortedByDescending { it.second }
            .take(10)
            .forEach { (gray, freq) ->
                Log.i("Histogram", "Gray $gray: $freq")
            }
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (histogramData.isEmpty() || width == 0 || height == 0) return

        val barWidth = width.toFloat() / 256f
        val viewHeight = height.toFloat()

        for (i in 0 until 256) {
            val barHeight = (histogramData[i].toFloat() / maxVal) * viewHeight

            val left = i * barWidth
            val top = viewHeight - barHeight
            val right = left + barWidth
            val bottom = viewHeight

            canvas.drawRect(left, top, right, bottom, paint)
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
