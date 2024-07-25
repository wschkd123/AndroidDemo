package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.opengl.Matrix
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.DrawableOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.R
import com.example.beyond.demo.ui.transformer.util.ReflectUtil

/**
 * @author wangshichao
 * @date 2024/7/10
 */
@UnstableApi
open class ImageOverlay(private val context: Context) : DrawableOverlay() {
    var overlaySettings: OverlaySettings

    init {
        val translateMatrix = GlUtil.create4x4IdentityMatrix()
        // 0，0在视频中心，1，1在右上角
        Matrix.translateM(translateMatrix,  /* mOffset= */0,  /* x= */0f,  /* y= */-1f,  /* z= */1f)
        overlaySettings = OverlaySettings.Builder()
            .setMatrix(translateMatrix)
            // -1 -1 在原覆盖物右上角的位置，1 1 在原覆盖物左下角的位置（接近覆盖物宽高，但是不超过）
            .setAnchor(0f, -1f)
            .build()
    }

    override fun getDrawable(presentationTimeUs: Long): Drawable {
        val drawable = ContextCompat.getDrawable(
            context, R.drawable.view_character_preview
        )
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        return drawable
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val progress = presentationTimeUs.toFloat() / (C.MICROS_PER_SECOND * 5)
        Log.i(
            TAG,
            "getBitmap: progress=" + progress + " presentationTimeMs=" + presentationTimeUs / 1000
        )
        if (progress in 0.0..1.0) {
            ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alpha", progress)
        }
        return super.getBitmap(presentationTimeUs)
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

    companion object {
        private val TAG = ImageOverlay::class.java.simpleName
    }
}
