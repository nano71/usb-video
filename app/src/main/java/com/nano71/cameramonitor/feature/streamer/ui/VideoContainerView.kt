/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nano71.cameramonitor.feature.streamer.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.nano71.cameramonitor.core.usb.UsbVideoNativeLibrary

class VideoContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val renderer = UsbVideoNativeLibrary.VideoRenderer(context)
    private var glSurfaceView: GLSurfaceView? = null
    private val gridOverlayView = GridOverlayView(context)
    private val histogramView = HistogramView(context)

    private var videoAspectRatio: Float = 0f

    init {
        renderer.onHistogramData = { data ->
            post {
                histogramView.updateHistogram(data)
            }
        }
    }

    fun toggleGridVisible() {
        gridOverlayView.visibility = if (gridOverlayView.isVisible) GONE else VISIBLE
    }

    fun toggleHistogramVisible() {
        val willShow = !histogramView.isVisible
        histogramView.visibility = if (willShow) VISIBLE else GONE
        renderer.showHistogram = willShow
    }

    fun setZebraVisible(visible: Boolean) {
        renderer.showZebra = visible
    }

    fun initialize(videoWidth: Int, videoHeight: Int, aspectRatioFloat: Float) {
        if (glSurfaceView != null) return
        glSurfaceView = GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER)

        addView(glSurfaceView, 0, params)
        addView(gridOverlayView, 1, params)

        val density = resources.displayMetrics.density
        val margin = (24 * density).toInt()
        val histogramParams = LayoutParams(
            (160 * density).toInt(),
            (90 * density).toInt(),
            Gravity.END or Gravity.BOTTOM
        ).apply {
            setMargins(0, 0, margin, margin)
        }
        histogramView.setFrameSize(videoWidth,videoHeight)

        addView(histogramView, 2, histogramParams)

        videoAspectRatio = aspectRatioFloat

        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (videoAspectRatio > 0) {
            var width = MeasureSpec.getSize(widthMeasureSpec)
            var height = MeasureSpec.getSize(heightMeasureSpec)
            val containerAspectRatio = width.toFloat() / height

            if (containerAspectRatio > videoAspectRatio) {
                width = (height * videoAspectRatio).toInt()
            } else {
                height = (width / videoAspectRatio).toInt()
            }

            super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
