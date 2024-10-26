package com.example.beyond.demo.ui.trackanimation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.beyond.demo.R

/**
 * 音轨动画视图
 *
 * 1. 4条音轨跳动，支持配置颜色。
 * 2. 支持任意尺寸
 *
 * @author wangshichao
 * @date 2024/10/18
 */
class TrackAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), Runnable {
    companion object {
        private const val FRAME_INTERVAL = 100L
    }

    private val paint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            strokeWidth = lineWidth
            style = Paint.Style.FILL
            strokeCap = Paint.Cap.ROUND
        }
    }
    private var paddingStart = 0f

    /**
     * 线条的颜色
     */
    var lineColor = 0
        set(value) {
            field = value
            paint.color = value
        }

    /**
     * 线条的宽度
     */
    private var lineWidth = 0f
        set(value) {
            field = value
            paint.strokeWidth = value
        }

    /**
     * 线条之间间距
     */
    private var lineSpacing = 0f
    private val keyframeProvider = KeyframeProvider()
    private var isRunning = false

    init {
        val array = context.obtainStyledAttributes(attrs, R.styleable.TrackAnimationView)
        lineColor = array.getColor(R.styleable.TrackAnimationView_line_color, 0)
        array.recycle()
    }

    fun start() {
        if (isRunning) {
            return
        }
        isRunning = true
        removeCallbacks(this)
        post(this)
    }

    fun stop() {
        if (isRunning.not()) {
            return
        }
        isRunning = false
        removeCallbacks(this)
        // 重置为首帧
        keyframeProvider.reset()
        invalidate()
    }

    override fun run() {
        invalidate()
        postDelayed(this, FRAME_INTERVAL)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            paddingStart = w * 0.11f
            lineWidth = w * 0.12f
            lineSpacing = w * 0.1f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        val startX = paddingStart + lineWidth.div(2f)
        val frameData = keyframeProvider.getFrame()
        for (index in 0..<4) {
            if (index > 0) {
                // 向右偏移用于绘制下一条线
                canvas.translate(lineWidth + lineSpacing, 0f)
            }
            // 垂直居中绘制。因为使用了Cap.ROUND，需要减去两端的线帽
            val lineHeight = height.times(frameData.getOrNull(index) ?: 1f) - lineWidth
            val startY = (height - lineHeight) / 2f
            canvas.drawLine(startX, startY, startX, startY + lineHeight, paint)
        }
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    class KeyframeProvider {
        /**
         * 关键帧，4条音轨长度参数
         */
        private val keyframeList = mutableListOf(
            arrayOf(0.45f, 0.35f, 0.75f, 0.35f),
            arrayOf(0.5f, 0.4f, 0.7f, 0.4f),
            arrayOf(0.55f, 0.45f, 0.65f, 0.45f),
            arrayOf(0.6f, 0.5f, 0.6f, 0.5f),
            arrayOf(0.65f, 0.55f, 0.55f, 0.55f),
            arrayOf(0.7f, 0.6f, 0.5f, 0.6f),
            arrayOf(0.75f, 0.55f, 0.45f, 0.65f),
            arrayOf(0.8f, 0.5f, 0.4f, 0.6f),
            arrayOf(0.75f, 0.55f, 0.45f, 0.55f),
            arrayOf(0.7f, 0.6f, 0.5f, 0.5f),
            arrayOf(0.65f, 0.55f, 0.55f, 0.45f),
            arrayOf(0.6f, 0.5f, 0.6f, 0.4f),
            arrayOf(0.55f, 0.45f, 0.65f, 0.35f),
            arrayOf(0.5f, 0.4f, 0.7f, 0.3f),
        )
        private var frameIndex = 0

        fun reset() {
            frameIndex = 0
        }

        fun getFrame(): Array<Float> {
            if (frameIndex < 0 || frameIndex >= keyframeList.size) {
                frameIndex = 0
            }
            val keyframe = keyframeList[frameIndex]
            frameIndex++
            return keyframe
        }

    }

}
