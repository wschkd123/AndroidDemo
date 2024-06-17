package com.example.beyond.demo.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.example.beyond.demo.R
import kotlin.math.min

/**
 * 圆角带边框的 image imageView
 * Created by fengkeke on 2023/12/21
 */
class RoundedImageView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    AppCompatImageView(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)


    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
    }
    private val rect = RectF()
    private var radius = 0f
    private val path = Path()
    private var borderWidth = 0f
    private var borderColor = Color.WHITE

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView)
        radius = typedArray.getDimension(R.styleable.RoundedImageView_round_corner_radius, 0f)
        borderWidth = typedArray.getDimension(R.styleable.RoundedImageView_round_border_width, 0f)
        borderColor =
            typedArray.getColor(R.styleable.RoundedImageView_round_border_color, Color.TRANSPARENT)
        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderWidth

        if (radius > min(width, height) / 2f) {
            radius = min(width, height) / 2f
        }
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas?.clipPath(path.apply {
            addRoundRect(
                rect,
                radius,
                radius,
                Path.Direction.CW
            )
        })

        super.onDraw(canvas)

        val borderRadius = radius
        rect.set(
            borderWidth / 2,
            borderWidth / 2,
            width.toFloat() - borderWidth / 2,
            height.toFloat() - borderWidth / 2
        )
        canvas?.drawRoundRect(rect, borderRadius, borderRadius, paint)
    }
}