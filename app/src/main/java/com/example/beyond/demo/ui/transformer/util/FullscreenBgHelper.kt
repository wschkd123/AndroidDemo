package com.example.beyond.demo.ui.transformer.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.Log
import com.example.beyond.demo.ui.transformer.TransformerConstant

/**
 *
 * @author beyond
 * @date 2024/7/26
 */
internal class FullscreenBgHelper {
    private val TAG = "FullscreenBgHelper"
    private val paint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix: android.graphics.Matrix = android.graphics.Matrix()
    private val dstWidth: Int = TransformerConstant.OUT_VIDEO_WIDTH
    private val dstHeight: Int = TransformerConstant.OUT_VIDEO_HEIGHT

    /**
     * 蒙层距上部的距离
     */
    private val maskTop: Float = TransformerConstant.OUT_VIDEO_HEIGHT / 3f
    private var targetBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)


    /**
     * 带蒙层的人物背景图片。图片“高宽比”比目标视图高时，缩放图像保留顶部内容裁掉底部内容，反之，缩放裁掉左右内容
     */
    fun createCharacterBgWithMask(src: Bitmap): Bitmap {
        // 矩阵变换用于绘制新的Bitmap
        val dx: Float
        val dy: Float
        val scale: Float
        if (dstHeight.div(dstWidth) > src.height.div(src.width)) {
            // 裁掉左右内容
            scale = dstHeight.toFloat() / src.height.toFloat()
            dx = (dstWidth - src.width * scale) * 0.5f
            dy = 0f
        } else {
            // 保留顶部内容裁掉底部内容
            scale = dstWidth.toFloat() / src.width
            dx = 0f
            dy = 0f
        }
        Log.i(
            TAG,
            "createCharacterBgWithMask: dstWidth=$dstWidth dstHeight=$dstHeight bitmapWidth=${src.width}" +
                    " bitmapHeight=${src.height} scale=$scale dx=$dx dy=$dy"
        )
        matrix.postScale(scale, scale)
        matrix.postTranslate(dx, dy)

        // 渐变蒙层画笔
        maskPaint.apply {
            val shader: Shader = LinearGradient(
                0f,
                maskTop,
                0f,
                dstHeight.toFloat(),
                0x001B1625,
                0xFF1B1625.toInt(),
                Shader.TileMode.CLAMP
            )
            setShader(shader)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }

//        targetBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
        try {
            Canvas(targetBitmap).let {
                it.drawBitmap(src, matrix, paint)
                // 添加渐变蒙层
                it.drawRect(
                    0f,
                    maskTop,
                    targetBitmap.width.toFloat(),
                    targetBitmap.height.toFloat(),
                    maskPaint
                )
                it.setBitmap(null)
                it
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }
}