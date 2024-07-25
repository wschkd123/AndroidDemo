package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.BitmapLoader
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.ui.transformer.util.ReflectUtil
import com.example.beyond.demo.ui.transformer.util.TransformerUtil
import java.util.concurrent.ExecutionException

/**
 * 视频封面-第一帧
 *
 * @author wangshichao
 * @date 2024/7/24
 */
class CoverOverlay(
    private val context: Context,
    private val url: String,
    private val startTimeUs: Long,
    private val durationUs: Long
) : BitmapOverlay() {
    private val TAG = javaClass.simpleName
    private var overlaySettings: OverlaySettings = OverlaySettings.Builder().build()
    private var lastBitmap: Bitmap? = null
    private val endTimeUs: Long = startTimeUs + durationUs

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
            lastBitmap = TransformerUtil.createCharacterBgWithMask(bitmap)
        }
        if (presentationTimeUs !in startTimeUs..endTimeUs) {
            Log.w(TAG, "getBitmap: hide")
            ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alpha", 0)
        }
        return lastBitmap!!
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

}