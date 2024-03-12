package com.example.beyond.demo.util


import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest


/**
 * Glide加载带边框的圆形图片
 *
 * @author wangshichao
 * @date 2024/3/12
 */
class CircleCropTransform @JvmOverloads constructor(
    private val mBorderSize: Float = 0f,
    borderColor: Int = Color.TRANSPARENT
) : BitmapTransformation() {

    private val mBorderPaint: Paint?

    /**
     * @param borderSize  边框宽度
     * @param borderColor 边框颜色
     */
    init {
        mBorderPaint = Paint().apply {
            isDither = true
            isAntiAlias = true
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = mBorderSize
        }
    }

    private fun circleCrop(pool: BitmapPool, source: Bitmap?): Bitmap? {
        if (source == null) return null
        val size = (Math.min(source.width, source.height) - mBorderSize / 2).toInt()
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        val squared = Bitmap.createBitmap(source, x, y, size, size)
        val result = pool[size, size, Bitmap.Config.ARGB_8888]
        val canvas = Canvas(result)
        val paint = Paint()
        paint.setShader(
            BitmapShader(
                squared,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
        )
        paint.isAntiAlias = true
        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)
        if (mBorderPaint != null) {
            val borderRadius = r - mBorderSize / 2
            canvas.drawCircle(r, r, borderRadius, mBorderPaint)
        }
        return result
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        return circleCrop(pool, toTransform)!!
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {}
}
