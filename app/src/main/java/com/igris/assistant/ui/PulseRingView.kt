package com.igris.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Expanding cyan pulse rings — the "alive" aura around the IGRIS emblem. */
class PulseRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var phase = 0f
    private var pulsing = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF39D3F5.toInt()
    }
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { phase = it.animatedValue as Float; invalidate() }
    }

    fun setPulsing(on: Boolean) {
        pulsing = on
        if (on) { if (!anim.isStarted) anim.start() } else { anim.cancel(); invalidate() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!pulsing) return
        val cx = width / 2f
        val cy = height / 2f
        val maxR = minOf(width, height) / 2f
        for (i in 0 until 3) {
            val p = (phase + i / 3f) % 1f
            paint.alpha = (160 * (1 - p)).toInt()
            paint.strokeWidth = 2f + 7f * (1 - p)
            canvas.drawCircle(cx, cy, maxR * (0.35f + 0.65f * p), paint)
        }
    }

    override fun onDetachedFromWindow() {
        anim.cancel()
        super.onDetachedFromWindow()
    }
}
