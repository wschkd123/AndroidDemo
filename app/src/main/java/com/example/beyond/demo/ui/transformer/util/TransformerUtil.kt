package com.example.beyond.demo.ui.transformer.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.Log
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.example.base.Init
import com.example.beyond.demo.ui.transformer.TransformerConstant


/**
 *
 * @author wangshichao
 * @date 2024/7/24
 */
internal object TransformerUtil {
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
        maskTop: Float = TransformerConstant.OUT_VIDEO_HEIGHT / 3f
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

        // 渐变蒙层画笔
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val shader: Shader = LinearGradient(
                0f,
                maskTop,
                0f,
                dstHeight.toFloat(),
                0x00FF4081,
                0xFFFF4081.toInt(),
                Shader.TileMode.CLAMP
            )
            setShader(shader)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        val targetBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(targetBitmap)
            // 添加渐变蒙层
            canvas.drawBitmap(src, matrix, Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.drawRect(
                0f,
                maskTop,
                targetBitmap.width.toFloat(),
                targetBitmap.height.toFloat(),
                maskPaint
            )
            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }

    /**
     * 同步加载本地图片
     */
    @WorkerThread
    fun loadImage(
        resId: Int,
        width: Int,
        height: Int
    ): Bitmap? {
        var bitmap: Bitmap? = null
        val requestOptions = RequestOptions()
            .transform(CenterCrop())
            .override(width, height)
        try {
            val futureTarget = Glide.with(Init.applicationContext)
                .asBitmap()
                .load(resId)
                .apply(requestOptions)
                .submit()
            bitmap = futureTarget.get()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AppWidget", "load fail ${e.message}")
        }
        return bitmap
    }

    fun createRoundedBitmapWithColor(width: Int, height: Int, cornerRadius: Float, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        return bitmap
    }
}