package com.example.beyond.demo.ui.transformer.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import com.example.beyond.demo.ui.transformer.TransformerConstant


/**
 *
 * @author wangshichao
 * @date 2024/7/24
 */
object TransformerUtil {
    private val TAG = "TransformerUtil"

    /**
     * 带蒙层的人物背景图片。图片“高宽比”比目标视图高时，缩放图像保留顶部内容裁掉底部内容，反之，缩放裁掉左右内容
     *
     * @param maskTop 蒙层位置
     */
    fun createCharacterBgWithMask(
        src: Bitmap,
        dstWidth: Int = TransformerConstant.OUT_VIDEO_WIDTH,
        dstHeight: Int = TransformerConstant.OUT_VIDEO_HEIGHT,
        maskTop: Int = 690
    ): Bitmap {
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
            "adapterBitmapSize: dstWidth=$dstWidth dstHeight=$dstHeight bitmapWidth=${src.width}" +
                    " bitmapHeight=${src.height} scale=$scale dx=$dx dy=$dy"
        )
        val matrix = android.graphics.Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)

        // 渐变画笔
        val maskPaint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            val shader: Shader = LinearGradient(
                0f,
                0f,
                0f,
                dstHeight.toFloat(),
                Color.TRANSPARENT,
                Color.parseColor("#1F1730"),
                Shader.TileMode.CLAMP
            )
            setShader(shader)
        }
        val targetBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(targetBitmap)
            canvas.drawBitmap(src, matrix, Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG))
            // 添加渐变蒙层
            canvas.drawRect(
                0f,
                maskTop.toFloat(),
                targetBitmap.width.toFloat(),
                targetBitmap.height.toFloat(),
                maskPaint
            );
            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }
}