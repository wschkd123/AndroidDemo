package com.example.beyond.demo.appwidget

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.beyond.demo.common.Init.applicationContext

/**
 * @author wangshichao
 * @date 2024/3/12
 */
internal object AppWidgetUtils {

    /**
     * 同步加载图片
     */
    @WorkerThread
    fun loadBitmapSync(
        tag: String,
        url: String,
        width: Int,
        height: Int,
        radius: Int
    ): Bitmap? {
        var bitmap: Bitmap? = null
        val requestOptions = RequestOptions()
            .transform(CenterCrop(), RoundedCorners(radius))
            .override(width, height)
        try {
            val futureTarget = Glide.with(applicationContext)
                .asBitmap()
                .load(url)
                .apply(requestOptions)
                .submit(width, height)
            bitmap = futureTarget.get()
            Log.i("AppWidget", "$tag loadBitmapSync success, url:${url}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AppWidget", "$tag loadBitmapSync fail, url:${url}", e)
        }
        return bitmap
    }
}
