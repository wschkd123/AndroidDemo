package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.beyond.demo.ui.transformer.util.FullscreenBgHelper
import com.example.beyond.demo.ui.transformer.util.ReflectUtil

/**
 * 全屏渐隐
 *
 * @author wangshichao
 * @date 2024/7/23
 */
@UnstableApi
class FullscreenAlphaOutOverlay(
    context: Context,
    url: String,
    private val durationUs: Long,
    private val enableAnimation: Boolean = true
) : BaseBitmapOverlay(context, url, durationUs) {

    private val bitmapHelper: FullscreenBgHelper = FullscreenBgHelper()

    override fun cropBitmap(srcBitmap: Bitmap): Bitmap {
        return bitmapHelper.createCharacterBgWithMask(srcBitmap)
    }

    override fun updateAnimation(presentationTimeUs: Long) {
        if (enableAnimation.not()) {
            return
        }
        val animatedValue =
            (presentationTimeUs - startTimeUs).toFloat().div(durationUs)
        Log.w(
            TAG,
            "updateAnimation: animatedValue=$animatedValue presentationTimeMs=$presentationTimeUs"
        )
        if (animatedValue in 0.0..1.0) {
            ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alphaScale", 1 - animatedValue)
        }
    }

}