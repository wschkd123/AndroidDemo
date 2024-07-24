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
    startTimeUs: Float,
    durationSeconds: Float
) : BaseAlphaOverlay(context, startTimeUs, durationSeconds) {

    override fun updateAlpha(overlaySettings: OverlaySettings, curAlpha: Float) {
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alpha", curAlpha)
    }

}