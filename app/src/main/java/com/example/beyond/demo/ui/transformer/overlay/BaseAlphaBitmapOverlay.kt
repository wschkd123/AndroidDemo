package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.Size
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.ui.transformer.util.BitmapHelper
import java.util.concurrent.ExecutionException

/**
 * 支持透明度动画的图片url覆盖物
 *
 * @author wangshichao
 * @date 2024/7/24
 */
abstract class BaseAlphaBitmapOverlay(
    private val context: Context,
    private val url: String,
    private val startTimeUs: Long,
    private val durationUs: Long
) : BitmapOverlay() {
    private val TAG = javaClass.simpleName
    open val initAlpha: Float
        get() = 0f
    private val overlaySettings: OverlaySettings by lazy {
        OverlaySettings.Builder()
            .setAlpha(initAlpha)
            .build()
    }
    private var lastBitmap: Bitmap? = null
    private val endTimeUs: Long = startTimeUs + durationUs
    private val bitmapHelper: BitmapHelper = BitmapHelper()

    abstract fun updateAlpha(overlaySettings: OverlaySettings, curAlpha: Float)

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        if (lastBitmap == null) {
            Log.w(
                TAG,
                "getBitmap: presentationTimeMs=$presentationTimeUs startTimeUs=${startTimeUs} endTimeUs=${endTimeUs} durationUs=${durationUs}"
            )
            val bitmapLoader: BitmapLoader = DataSourceBitmapLoader(context)
            val future = bitmapLoader.loadBitmap(Uri.parse(url))
            val bitmap = try {
                future.get()
            } catch (e: ExecutionException) {
                throw VideoFrameProcessingException(e)
            } catch (e: InterruptedException) {
                throw VideoFrameProcessingException(e)
            }
            /**
             * [androidx.media3.effect.Presentation]配置的输出视频分辨率
             */
            lastBitmap = bitmapHelper.createCharacterBgWithMask(bitmap)
        }
        updateAnimation(presentationTimeUs)
        return lastBitmap!!
    }

    /**
     * @param videoSize [androidx.media3.effect.Presentation]配置的输出视频分辨率，不然与输入（图片）一致
     */
    override fun configure(videoSize: Size) {
        super.configure(videoSize)
//        Log.d(TAG, "configure: videoWidth=${videoSize.width} videoHeight=${videoSize.height}")
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

    private fun updateAnimation(presentationTimeUs: Long) {
        Log.i(
            TAG,
            "updateAnimation: presentationTimeMs=$presentationTimeUs"
        )
        // 动画时间
        if (presentationTimeUs in startTimeUs..endTimeUs) {
            val animatedValue =
                (presentationTimeUs - startTimeUs).toFloat().div(durationUs)
            Log.w(TAG, "updateAnimation: animatedValue=$animatedValue")
            if (animatedValue in 0.0..1.0) {
                updateAlpha(overlaySettings, animatedValue)
            }
        }

    }

}