package com.example.beyond.demo.ui.swipe.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import com.example.base.util.ext.dpToPx
import com.example.base.util.ext.dpToPxFloat

/**
 *
 * 滑动过程中顶部和顶部逐渐被裁剪掉
 *
 * @author wangshichao
 * @date 2025/9/18
 */
open class ClipView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val topHideHeightPx = 48.dpToPx()
    private val bottomHideHeightPx = 60.dpToPx()
    private val radius = 16.dpToPxFloat()
    private val radii: FloatArray =
        floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)

    private var clipProgress: Float = 0f
    private var clipRectF: RectF = RectF()

    fun setProgress(progress: Float) {
        clipProgress = progress.coerceIn(0f, 1f)
        val topClip = topHideHeightPx * clipProgress
        val bottomClip = bottomHideHeightPx * clipProgress
        clipRectF = RectF(0f, topClip, width.toFloat(), height - bottomClip)
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (clipRectF.isEmpty) {
            super.dispatchDraw(canvas)
            return
        }

        val saveCount = canvas.save()

        val path = Path().apply {
            // 进度为0时，直接矩形裁剪；反之则圆角裁剪
            if (clipProgress == 0f) {
                addRect(clipRectF, Path.Direction.CW)
            } else {
                addRoundRect(clipRectF, radii, Path.Direction.CW)
            }
        }
        // 裁剪子视图
        canvas.clipPath(path)
        super.dispatchDraw(canvas)

        canvas.restoreToCount(saveCount)
    }

}