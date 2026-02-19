package com.example.audiorecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.parseColor("#6200EE")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var amplitudes = mutableListOf<Float>()
    private val maxSpikes = 50
    private val spikeWidth = 8f
    private val spikeGap = 4f
    private val cornerRadius = 4f

    fun addAmplitude(amp: Float) {
        val normalized = (amp / 32767f) * height.toFloat() * 0.8f
        amplitudes.add(normalized)
        if (amplitudes.size > maxSpikes) {
            amplitudes.removeAt(0)
        }
        invalidate()
    }

    fun clear() {
        amplitudes.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (amplitudes.isEmpty()) return

        val centerY = height / 2f
        val startX = width - (amplitudes.size * (spikeWidth + spikeGap))

        amplitudes.forEachIndexed { index, amp ->
            val left = startX + index * (spikeWidth + spikeGap)
            val top = centerY - amp / 2f
            val right = left + spikeWidth
            val bottom = centerY + amp / 2f
            
            val rect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }
    }
}
