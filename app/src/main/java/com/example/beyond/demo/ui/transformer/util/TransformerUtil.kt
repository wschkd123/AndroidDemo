package com.example.beyond.demo.ui.transformer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.example.base.util.YWBitmapUtil


/**
 *
 * @author wangshichao
 * @date 2024/7/24
 */
internal object TransformerUtil {
    private val TAG = "TransformerUtil-Overlay"

    /**
     * 同步加载本地图片。如果加载失败，返回空位图
     */
    @WorkerThread
    fun loadImage(
        context: Context,
        url: String
    ): Bitmap {
        var bitmap: Bitmap? = null
        try {
            val futureTarget = Glide.with(context)
                .asBitmap()
                .load(url)
                .submit()
            bitmap = futureTarget.get()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AppWidget", "load $url fail ${e.message}")
        }
        return bitmap?: createEmptyBitmap()
    }

    /**
     * 同步加载本地图片
     */
    @WorkerThread
    fun loadImage(
        context: Context,
        resId: Int,
        width: Int,
        height: Int
    ): Bitmap? {
        var bitmap: Bitmap? = null
        val requestOptions = RequestOptions()
            .transform(CenterCrop())
            .override(width, height)
        try {
            val futureTarget = Glide.with(context)
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

    /**
     * 加载本地图片
     *
     * @param width 指定宽度
     */
    fun loadImage(context: Context, resId: Int, width: Int = 0): Bitmap? {
        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        if (width == 0) return bitmap
        return YWBitmapUtil.scaleBitmapByWidth(bitmap, width)
    }

    /**
     * @param srcBitmap 源Bitmap
     * @param newBitmap 添加的Bitmap
     * @return 两者混合的Bitmap
     */
    fun addBitmap(
        srcBitmap: Bitmap,
        newBitmap: Bitmap,
        left: Float,
        top: Float
    ): Bitmap {
        val targetBitmap = srcBitmap.copy(srcBitmap.config, true)
        try {
            val canvas = Canvas(targetBitmap)
            val start = System.currentTimeMillis()
            val paint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(newBitmap, left, top, paint)
            Log.d(TAG, "addBitmap: drawBitmap cost=${System.currentTimeMillis() - start}")
            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }

    fun createEmptyBitmap(): Bitmap {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}