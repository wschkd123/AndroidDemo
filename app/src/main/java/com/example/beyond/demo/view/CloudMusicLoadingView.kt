package com.example.beyond.demo.view

import android.animation.FloatEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.beyond.demo.R
import java.util.Random

class CloudMusicLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), Runnable {
    companion object {

        /**
         * 默认4条音轨
         */
        private const val DEFAULT_RAIL_COUNT = 4
    }

    /**
     * 随机数
     */
    private val random = Random()

    /**
     * 画笔
     */
    private val paint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = railColor
            strokeWidth = railLineWidth
            style = Paint.Style.FILL
            strokeCap = Paint.Cap.ROUND
        }
    }

    /**
     * 音轨数量
     */
    private var railCount = 0

    /**
     * 音轨颜色
     */
    var railColor = 0
        set(value) {
            field = value
            paint.color = value
        }

    /**
     * 每条音轨的线宽
     */
    private var railLineWidth = 0f
        set(value) {
            field = value
            paint.strokeWidth = value
        }

    /**
     * Float类型估值器，用于在指定数值区域内进行估值
     */
    private val floatEvaluator: FloatEvaluator = FloatEvaluator()

    init {
        initAttr(context, attrs)
    }


    fun start() {
        postDelayed(this, 700)
    }

    fun stop() {
        removeCallbacks(this)
    }

    private fun initAttr(context: Context, attrs: AttributeSet?) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.CloudMusicLoadingView)
        railCount =
            array.getInt(R.styleable.CloudMusicLoadingView_cmlv_rail_count, DEFAULT_RAIL_COUNT)
        railColor = array.getColor(
            R.styleable.CloudMusicLoadingView_cmlv_rail_color,
            Color.argb(255, 255, 255, 255)
        )
        array.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            railLineWidth = w * 0.12f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //计算可用高度
        val totalAvailableHeight = (height - paddingBottom - paddingTop).toFloat()
        //计算每条音轨平分宽度后的位置
        val averageBound = (width - paddingStart - paddingEnd) / railCount.toFloat()
        //计算每条音轨的x坐标位置
        val x = averageBound - railLineWidth
        val y = paddingBottom.toFloat()

        //保存画布
        canvas.save()
        for (i in 1..railCount) {
            //估值x坐标
            val fraction = random.nextFloat()
            val evaluateY = floatEvaluator.evaluate(fraction, 0.3f, 0.8f) * totalAvailableHeight
            // 需要移动到垂直居中的位置，计算偏移
            val offset = (totalAvailableHeight - (evaluateY - y)) / 2f
            //第一个不需要偏移
            if (i == 1) {
                canvas.drawLine(x, y + offset, x, evaluateY + offset, paint)
            } else {
                //后续，每个音轨都固定偏移间距后，再画
                canvas.translate(x, 0f)
                canvas.drawLine(x, y + offset, x, evaluateY + offset, paint)
            }
        }
        //恢复画布
        canvas.restore()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
//        start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun run() {
        invalidate()
        postDelayed(this, 200)
    }

}
