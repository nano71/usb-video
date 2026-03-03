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
package com.nano71.cameramonitor.feature.streamer.viewholder

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nano71.cameramonitor.R
import com.nano71.cameramonitor.feature.streamer.StreamerScreen
import com.nano71.cameramonitor.feature.streamer.StreamerViewModel
import com.nano71.cameramonitor.feature.streamer.ui.VideoContainerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "StreamingViewHolder"

@SuppressLint("ClickableViewAccessibility")
class StreamingViewHolder(
    private val rootView: View,
    private val streamerViewModel: StreamerViewModel,
    private val onNavigate: (StreamerScreen) -> Unit
) : RecyclerView.ViewHolder(rootView) {

    val streamStatsTextView: TextView = rootView.findViewById(R.id.stream_stats_text)
    val videoContainerView: VideoContainerView = rootView.findViewById(R.id.video_container)
    val bottomToolbar: LinearLayout = rootView.findViewById(R.id.bottom_toolbar)
    val backButton: MaterialButton = bottomToolbar.findViewById(R.id.back_button)
    val gridButton: MaterialButton = bottomToolbar.findViewById(R.id.grid_button)
    val zebraPrintButton: MaterialButton = bottomToolbar.findViewById(R.id.texture_button)
    val histogramButton: MaterialButton = bottomToolbar.findViewById(R.id.histogram_button)
    val buttons = listOf(gridButton, zebraPrintButton, histogramButton)

    // 状态 Map
    val buttonStates = mutableMapOf<MaterialButton, Boolean>().apply {
        buttons.forEach { put(it, false) } // 默认都未激活
    }
    var operating = false
    var showZebra = false


    init {
        // 禁用点击和焦点
        itemView.isClickable = false
        itemView.isFocusable = false

        // 拦截所有触摸事件
        itemView.setOnTouchListener { _, _ -> true }
        val videoFormat = streamerViewModel.videoFormat
        val width = videoFormat?.width ?: 1920
        val height = videoFormat?.height ?: 1080
        val aspectRatioFloat = videoFormat?.aspectRatioFloat ?: 1.77F
        videoContainerView.initialize(width, height, aspectRatioFloat)
        disableRootViewTouch()
        setupToolbarToggle()
        setupToolbarButtons()
    }

    fun disableRootViewTouch() {
        rootView.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                v.parent.requestDisallowInterceptTouchEvent(true)
            }
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            true
        }
    }

    fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    streamStatsTextView.text = streamerViewModel.getVideoStreamInfoString()
                    delay(1000L)
                }
            }
        }
    }

    private fun setupToolbarButtons() {
        fun updateButtonColor(button: MaterialButton) {
            val oldState = buttonStates[button] ?: false
            val color = if (oldState) {
                button.context.getColor(R.color.on_overlay_surface)
            } else {
                button.context.getColor(R.color.on_overlay_surface_active)
            }
            button.iconTint = ColorStateList.valueOf(color)
            buttonStates[button] = !oldState
        }
        backButton.setOnClickListener {
            Log.i(TAG, "offButton clicked")
            onNavigate(StreamerScreen.Status)
        }
        gridButton.setOnClickListener {
            videoContainerView.toggleGridVisible()
            updateButtonColor(gridButton)
        }
        zebraPrintButton.setOnClickListener {
            showZebra = !showZebra
            videoContainerView.setZebraVisible(showZebra)
            updateButtonColor(zebraPrintButton)

        }
        histogramButton.setOnClickListener {
            videoContainerView.toggleHistogramVisible()
            updateButtonColor(histogramButton)
        }
    }

    private fun setupToolbarToggle() {
        rootView.postDelayed({
            if (!operating) {
                toggleToolbar()
            }
        }, 3000L)
        rootView.setOnClickListener {
            toggleToolbar()
        }
    }

    private fun toggleToolbar() {
        if (bottomToolbar.isVisible) {
            fadeOut(bottomToolbar)
        } else {
            fadeIn(bottomToolbar)
        }
    }

    private fun fadeIn(view: View) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun fadeOut(view: View) {
        view.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
    }
}
