package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.opengl.Matrix
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.util.GlUtil
import androidx.media3.effect.DrawableOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.R
import com.example.beyond.demo.ui.transformer.util.ReflectUtil.updateOverlaySettingsFiled

/**
 * 渐隐
 *
 * @author wangshichao
 * @date 2024/7/23
 */
open class AlphaOutOverlay(
    private val context: Context,
    private val durationSeconds: Float
) : DrawableOverlay() {
    private var overlaySettings: OverlaySettings

    init {
        val translateMatrix = GlUtil.create4x4IdentityMatrix()
        Matrix.translateM(translateMatrix, 0, 0f, 0f, 1f)
        overlaySettings = OverlaySettings.Builder()
            .setMatrix(translateMatrix) // 向下偏移
            .setAnchor(0f, 1f)
            .build()
    }

    override fun getDrawable(presentationTimeUs: Long): Drawable {
        val drawable = ContextCompat.getDrawable(context, R.drawable.character_bg)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        return drawable
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val progress = presentationTimeUs.toFloat() / (C.MICROS_PER_SECOND * durationSeconds)
        Log.i(
            TAG,
            "getBitmap: progress=" + progress + " presentationTimeMs=" + presentationTimeUs / 1000
        )
        if (progress in 0.0..1.0) {
            updateOverlaySettingsFiled(overlaySettings, "alpha", 1 - progress)
        }
        return super.getBitmap(presentationTimeUs)
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

    companion object {
        private val TAG = AlphaOutOverlay::class.java.simpleName
    }
}
