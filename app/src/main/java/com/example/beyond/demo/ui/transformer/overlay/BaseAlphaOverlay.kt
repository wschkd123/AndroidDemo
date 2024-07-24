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

/**
 *
 * @author wangshichao
 * @date 2024/7/24
 */
abstract class BaseAlphaOverlay(
    private val context: Context,
    private val startTimeUs: Float,
    private val durationSeconds: Float
) : DrawableOverlay() {
    private val TAG = javaClass.simpleName
    private var overlaySettings: OverlaySettings
    private val scaleMatrix: FloatArray = GlUtil.create4x4IdentityMatrix()
    private val drawable: Drawable? =
        ContextCompat.getDrawable(context, R.drawable.character_bg)?.apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    private var isInitConfigure = false

    init {
        overlaySettings = OverlaySettings.Builder()
            .setMatrix(scaleMatrix)
            .setAlpha(0f)
            .build()
    }

    abstract fun updateAlpha(overlaySettings: OverlaySettings, curAlpha: Float)

    override fun getDrawable(presentationTimeUs: Long): Drawable {
        return drawable!!
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val animationEndTimeUs = startTimeUs.plus(C.MICROS_PER_SECOND.times(durationSeconds))
        Log.i(
            TAG,
            "getBitmap: presentationTimeMs=$presentationTimeUs startTimeMs=${startTimeUs} endTimeUs=${animationEndTimeUs}"
        )
        // 动画时间
        if (presentationTimeUs >= startTimeUs && presentationTimeUs <= animationEndTimeUs) {
            val animationProgress = (presentationTimeUs - startTimeUs).div(C.MICROS_PER_SECOND * durationSeconds)
            Log.w(
                TAG,
                "getBitmap: update alpha progress=$animationProgress"
            )
            if (animationProgress in 0.0..1.0) {
                updateAlpha(overlaySettings, animationProgress)
            }
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
        Log.i(
            TAG,
            "adapterVideoSize: videoWidth=${videoSize.width} videoHeight=${videoSize.height}"
        )
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

}