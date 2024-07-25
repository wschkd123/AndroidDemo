package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.beyond.demo.ui.transformer.util.ReflectUtil
import com.example.beyond.demo.ui.transformer.util.TransformerUtil

/**
 * 全屏渐显
 *
 * @author wangshichao
 * @date 2024/7/23
 */
class FullscreenAlphaInOverlay(
    context: Context,
    url: String,
    private val startTimeUs: Long,
    private val durationUs: Long
) : BaseBitmapOverlay(context, url, startTimeUs, durationUs) {

    override fun cropBitmap(srcBitmap: Bitmap): Bitmap {
        return TransformerUtil.createCharacterBgWithMask(srcBitmap)
    }

    override fun updateAnimation(presentationTimeUs: Long) {
        val animatedValue =
            (presentationTimeUs - startTimeUs).toFloat().div(durationUs)
        Log.w(
            TAG,
            "updateAnimation: animatedValue=$animatedValue presentationTimeMs=$presentationTimeUs"
        )
        if (animatedValue in 0.0..1.0) {
            ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alpha", animatedValue)
        }
    }

}