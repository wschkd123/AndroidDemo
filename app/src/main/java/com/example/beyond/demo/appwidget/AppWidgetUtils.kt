package com.example.beyond.demo.appwidget

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.beyond.demo.common.Init.applicationContext
import com.example.beyond.demo.util.kt.dpToPx

/**
 * @author wangshichao
 * @date 2024/3/12
 */
internal object AppWidgetUtils {
    fun loadBitmap(
        url: String,
        width: Int,
        height: Int,
        radius: Int,
        invoke: (Bitmap) -> Unit
    ) {
        val requestOptions = RequestOptions()
            .transform(RoundedCorners(radius.dpToPx()))
            .override(width, height)
            .centerCrop()

        Glide.with(applicationContext)
            .asBitmap()
            .load(url)
            .apply(requestOptions)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    invoke.invoke(resource)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
//                    Log.e("AppWidget", "${TAG} onLoadFailed url:$url")
                }
            })

    }
}
