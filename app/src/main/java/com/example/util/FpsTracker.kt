package com.example.util

import android.view.Choreographer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

class FpsTracker {

    private var lastTimeNanos = 0L
    private var frameCount = 0
    private var isRunning = false

    var currentFps by mutableIntStateOf(60)
        private set

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return

            frameCount++
            if (lastTimeNanos == 0L) {
                lastTimeNanos = frameTimeNanos
            } else {
                val elapsed = frameTimeNanos - lastTimeNanos
                if (elapsed >= 1_000_000_000L) { // 1 second
                    val calculatedFps = (frameCount * 1_000_000_000L / elapsed).toInt()
                    currentFps = calculatedFps.coerceIn(1, 144)
                    frameCount = 0
                    lastTimeNanos = frameTimeNanos
                }
            }

            if (isRunning) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    fun start() {
        if (!isRunning) {
            isRunning = true
            frameCount = 0
            lastTimeNanos = 0L
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun stop() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }
}
