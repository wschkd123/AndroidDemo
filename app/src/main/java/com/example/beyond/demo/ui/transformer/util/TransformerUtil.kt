package com.example.beyond.demo.ui.transformer.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.example.base.Init


/**
 *
 * @author wangshichao
 * @date 2024/7/24
 */
internal object TransformerUtil {
    private val TAG = "TransformerUtil-Overlay"

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

    fun addBitmap(
        srcBitmap: Bitmap,
        newBitmap: Bitmap
    ): Bitmap {
        val targetBitmap = srcBitmap.copy(srcBitmap.config, true)
        try {
            val canvas = Canvas(targetBitmap)
            val start = System.currentTimeMillis()
            val paint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(newBitmap, 54f, 0f, paint)
            Log.d(TAG, "addBitmap: drawBitmap cost=${System.currentTimeMillis() - start}")
//            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }

    fun createEmptyBitmap(): Bitmap {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}