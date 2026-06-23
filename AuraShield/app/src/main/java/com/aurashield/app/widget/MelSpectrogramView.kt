package com.aurashield.app.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * Static bar-chart rendering of a mel-spectrogram layer, seeded per risk
 * event so the same call always renders the same "fingerprint".
 */
class MelSpectrogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var seed = 1L
    private var bars: FloatArray = FloatArray(48) { 0.3f }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setSeed(value: Long) {
        seed = value
        val rnd = Random(seed)
        bars = FloatArray(48) { i ->
            val base = 0.25f + 0.6f * sinWave(i)
            (base + rnd.nextFloat() * 0.3f).coerceIn(0.08f, 1f)
        }
        invalidate()
    }

    private fun sinWave(i: Int): Float =
        (Math.sin(i / 4.5) * 0.5 + 0.5).toFloat()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        paint.shader = LinearGradient(
            0f, height.toFloat(), 0f, 0f,
            Color.parseColor("#22E6B0"), Color.parseColor("#FF8A5C"),
            Shader.TileMode.CLAMP
        )
        val gap = 4f
        val barWidth = (width / bars.size) - gap
        var x = 0f
        for (v in bars) {
            val barHeight = height * v
            canvas.drawRoundRect(
                x, height - barHeight, x + barWidth, height.toFloat(),
                3f, 3f, paint
            )
            x += barWidth + gap
        }
    }
}
