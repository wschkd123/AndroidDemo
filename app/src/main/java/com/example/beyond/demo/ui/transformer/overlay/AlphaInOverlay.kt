package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.opengl.Matrix
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.effect.DrawableOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.R
import com.example.beyond.demo.ui.transformer.util.ReflectUtil.updateOverlaySettingsFiled

/**
 * 渐显
 *
 * @author wangshichao
 * @date 2024/7/23
 */
open class AlphaInOverlay(
    private val context: Context,
    private val durationSeconds: Float
) : DrawableOverlay() {
    private var overlaySettings: OverlaySettings
    private val scaleMatrix: FloatArray = GlUtil.create4x4IdentityMatrix()
    private val drawable: Drawable? =  ContextCompat.getDrawable(context, R.drawable.character_bg)?.apply {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    }
    private var isInitConfigure = false

    init {
        overlaySettings = OverlaySettings.Builder()
            .setMatrix(scaleMatrix)
            .setAlpha(0f)
            .build()
    }

    override fun getDrawable(presentationTimeUs: Long): Drawable {
        return drawable!!
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val progress = presentationTimeUs.toFloat() / (C.MICROS_PER_SECOND * durationSeconds)
        Log.i(
            TAG,
            "getBitmap: progress=" + progress + " presentationTimeMs=" + presentationTimeUs / 1000
        )
        if (progress in 0.0..1.0) {
            updateOverlaySettingsFiled(overlaySettings, "alpha", progress)
        }
        return super.getBitmap(presentationTimeUs)
    }

    override fun configure(videoSize: Size) {
        super.configure(videoSize)
        if (isInitConfigure) {
            return
        }
        isInitConfigure = true
        adapterVideoSize(videoSize)
    }

    /**
     * 图片充满播放器，居住剪裁
     */
    private fun adapterVideoSize(videoSize: Size) {
        val drawable = drawable ?: return
        val drawableRatio = drawable.intrinsicHeight / drawable.intrinsicWidth.toFloat()
        val videoRatio = videoSize.height / videoSize.width.toFloat()
        val scale = if (drawableRatio > videoRatio) {
            videoSize.width / drawable.intrinsicWidth.toFloat()
        } else {
            videoSize.height / drawable.intrinsicHeight.toFloat()
        }
        Log.i(
            TAG,
            "adapterVideoSize: drawableWidth=${drawable.intrinsicWidth} drawableHeight=${drawable.intrinsicHeight}"
        )
        Log.i(TAG, "adapterVideoSize: videoWidth=${videoSize.width} videoHeight=${videoSize.height}")
        Log.i(TAG, "adapterVideoSize: scale=$scale")
        Matrix.scaleM(
            scaleMatrix,  /* mOffset= */
            0,  /* x= */
            scale,
            scale,
            1f
        )
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

    companion object {
        private val TAG = AlphaInOverlay::class.java.simpleName
    }
}
