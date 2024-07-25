package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.ui.transformer.util.ReflectUtil

/**
 * 渐显
 *
 * @author wangshichao
 * @date 2024/7/23
 */
class AlphaInOverlay(
    context: Context,
    url: String,
    startTimeUs: Long,
    durationUs: Long
) : BaseAlphaBitmapOverlay(context, url, startTimeUs, durationUs) {

    override val initAlpha: Float
        get() = 0f

    override fun updateAlpha(overlaySettings: OverlaySettings, curAlpha: Float) {
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alpha", curAlpha)
    }

}