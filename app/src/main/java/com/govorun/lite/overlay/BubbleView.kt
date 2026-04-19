package com.govorun.lite.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.govorun.lite.R

/**
 * The floating "Говорун" — a translucent dark circle with a white bird
 * silhouette. Red with pulsing halo while recording, orange while processing.
 *
 * Lite build has no mode dot and no pill expansion — a single VAD mode
 * is the only interaction, so Говорун carries no extra UI.
 */
class BubbleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density
    private val bubbleSize = (56 * dp).toInt()
    // Bird silhouette reads a bit small at 24dp on a 56dp disc — bump it up
    // so the shape is recognisable at a glance.
    private val iconSize = (32 * dp).toInt()

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80404040.toInt(); style = Paint.Style.FILL
    }
    private val recordingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE53935.toInt(); style = Paint.Style.FILL
    }
    private val processingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFA726.toInt(); style = Paint.Style.FILL
    }
    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x40E53935.toInt(); style = Paint.Style.FILL
    }

    private var isRecording = false
    private var isProcessing = false
    private var pulseRadius = 0f
    private var pulseAnimator: ValueAnimator? = null

    private val birdIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_bird_24)?.apply {
        setTint(0xFFFFFFFF.toInt())
    }

    init { elevation = 8 * dp }

    fun setRecording(recording: Boolean) {
        isRecording = recording; isProcessing = false
        if (recording) startPulse() else stopPulse()
        invalidate()
    }

    fun setProcessing(processing: Boolean) {
        isProcessing = processing; isRecording = false
        stopPulse(); invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = bubbleSize + (bubbleSize * 0.4f).toInt()
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f; val cy = height / 2f
        val radius = bubbleSize / 2f

        if (isRecording && pulseRadius > 0) {
            canvas.drawCircle(cx, cy, pulseRadius, pulsePaint)
        }

        val paint = when {
            isProcessing -> processingPaint
            isRecording -> recordingPaint
            else -> basePaint
        }
        canvas.drawCircle(cx, cy, radius, paint)

        birdIcon?.let {
            val l = (cx - iconSize / 2).toInt()
            val t = (cy - iconSize / 2).toInt()
            it.setBounds(l, t, l + iconSize, t + iconSize)
            it.draw(canvas)
        }
    }

    private fun startPulse() {
        pulseAnimator?.cancel()
        val maxR = bubbleSize / 2f * 1.4f
        pulseAnimator = ValueAnimator.ofFloat(bubbleSize / 2f, maxR).apply {
            duration = 800; repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { pulseRadius = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun stopPulse() { pulseAnimator?.cancel(); pulseRadius = 0f }
}
