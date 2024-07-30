package com.example.beyond.demo

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 *
 * Created by fengkeke on 2023/12/26
 */
@GlideModule
class DemoGlideModule : AppGlideModule() {
    companion object {
        const val TAG = "DemoGlideModule"
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        builder.addGlobalRequestListener(CustomRequestListener())
    }


    class CustomRequestListener<T> :
        RequestListener<T> {

        override fun onResourceReady(
            resource: T,
            model: Any,
            target: Target<T>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            return false
        }

        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<T>,
            isFirstResource: Boolean
        ): Boolean {
            Log.i(TAG, "onLoadFailed: ${e?.message}")
            return false
        }

    }

}