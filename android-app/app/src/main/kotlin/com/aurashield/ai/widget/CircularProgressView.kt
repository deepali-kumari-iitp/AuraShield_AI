package com.aurashield.ai.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.aurashield.ai.R

/**
 * Lightweight canvas ring used for the protection score (Console) and
 * confidence score (Risk Log items). Animates from 0 -> target on bind.
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        color = ContextCompat.getColor(context, R.color.bg_stroke)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.aura_mint)
    }

    private val rect = RectF()
    private var animatedProgress = 0f // 0..100

    fun setRingColor(colorRes: Int) {
        progressPaint.color = ContextCompat.getColor(context, colorRes)
        invalidate()
    }

    fun setProgress(target: Int, animate: Boolean = true) {
        if (!animate) {
            animatedProgress = target.toFloat()
            invalidate()
            return
        }
        ValueAnimator.ofFloat(0f, target.toFloat()).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animatedProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val stroke = trackPaint.strokeWidth
        rect.set(stroke / 2, stroke / 2, w - stroke / 2, h - stroke / 2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)
        val sweep = animatedProgress / 100f * 360f
        canvas.drawArc(rect, -90f, sweep, false, progressPaint)
    }
}
