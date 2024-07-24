package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.ui.transformer.util.ReflectUtil

/**
 * 渐隐
 *
 * @author wangshichao
 * @date 2024/7/23
 */
class AlphaOutOverlay(
    context: Context,
    url: String,
    startTimeMs: Long,
    durationMs: Long
) : BaseAlphaBitmapOverlay(context, url, startTimeMs, durationMs) {

    override val initAlpha: Float
        get() = 1f

    override fun updateAlpha(overlaySettings: OverlaySettings, curAlpha: Float) {
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alpha", 1 - curAlpha)
    }

}