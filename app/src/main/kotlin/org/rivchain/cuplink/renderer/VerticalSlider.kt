package org.rivchain.cuplink.renderer

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import org.rivchain.cuplink.R

class VerticalSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface OnSeekBarChangeListener {
        fun onProgressChanged(slider: VerticalSlider, progress: Int, fromUser: Boolean)
        fun onStartTrackingTouch(slider: VerticalSlider) {}
        fun onStopTrackingTouch(slider: VerticalSlider) {}
    }

    private val trackView = View(context).apply {
        setBackgroundColor(Color.LTGRAY)
    }
    private val progressView = View(context).apply {
        setBackgroundColor(ContextCompat.getColor(context, R.color.selectedColor))
    }
    private val thumbView = View(context).apply {
        setBackgroundColor(ContextCompat.getColor(context, R.color.selectedColor))
    }

    private var max = 100
    private var progress = 50
    private var thumbSize = 40
    private var listener: OnSeekBarChangeListener? = null
    private var isTracking = false

    init {
        addView(trackView)
        addView(progressView)
        addView(thumbView)

        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isTracking = true
                    listener?.onStartTrackingTouch(this)
                    updateProgress(event.y, true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    updateProgress(event.y, true)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isTracking = false
                    listener?.onStopTrackingTouch(this)
                    true
                }
                else -> false
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = measuredWidth
        val height = measuredHeight

        val trackWidth = width / 4
        val trackLeft = (width - trackWidth) / 2
        trackView.layout(trackLeft, 0, trackLeft + trackWidth, height)

        val progressHeight = height * progress / max
        progressView.layout(trackLeft, height - progressHeight, trackLeft + trackWidth, height)

        val thumbY = height - progressHeight - thumbSize / 2
        thumbView.layout(trackLeft - thumbSize / 2, thumbY, trackLeft + trackWidth + thumbSize / 2, thumbY + thumbSize)
    }

    private fun updateProgress(y: Float, fromUser: Boolean) {
        val height = measuredHeight
        val newProgress = max - (y / height * max).toInt()
        val clampedProgress = newProgress.coerceIn(0, max)

        if (clampedProgress != progress) {
            progress = clampedProgress
            listener?.onProgressChanged(this, progress, fromUser)
            requestLayout()
        }
    }

    fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener) {
        this.listener = listener
    }

    fun setProgress(value: Int) {
        progress = value.coerceIn(0, max)
        requestLayout()
    }

    fun getProgress(): Int = progress
}
