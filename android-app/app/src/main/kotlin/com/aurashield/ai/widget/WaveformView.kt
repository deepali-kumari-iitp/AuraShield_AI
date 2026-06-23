package com.aurashield.ai.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.aurashield.ai.R
import kotlin.math.sin

/**
 * Continuously-scrolling dual sine wave, standing in for a live mic
 * waveform while "Analyzing Incoming Stream" is active.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val wavePaintA = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = ContextCompat.getColor(context, R.color.aura_mint)
    }
    private val wavePaintB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = ContextCompat.getColor(context, R.color.aura_mint_dim)
    }

    private var phase = 0f
    private var running = false

    private val path = Path()
    private val pathB = Path()

    private val invalidator = object : Runnable {
        override fun run() {
            if (!running) return
            phase += 0.18f
            invalidate()
            postDelayed(this, 16)
        }
    }

    fun start() {
        if (running) return
        running = true
        post(invalidator)
    }

    fun stop() {
        running = false
        removeCallbacks(invalidator)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val midY = height / 2f
        val amplitudeA = height * 0.22f
        val amplitudeB = height * 0.14f

        path.reset()
        pathB.reset()
        var x = 0f
        var first = true
        while (x <= width) {
            val yA = midY + amplitudeA * sin((x / 70f) + phase).toFloat()
            val yB = midY + amplitudeB * sin((x / 45f) + phase * 1.4f + 1.2f).toFloat()
            if (first) {
                path.moveTo(x, yA)
                pathB.moveTo(x, yB)
                first = false
            } else {
                path.lineTo(x, yA)
                pathB.lineTo(x, yB)
            }
            x += 6f
        }
        canvas.drawPath(pathB, wavePaintB)
        canvas.drawPath(path, wavePaintA)
    }
}
