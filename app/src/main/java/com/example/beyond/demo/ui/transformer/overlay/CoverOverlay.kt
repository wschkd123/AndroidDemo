package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.BitmapLoader
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.ui.transformer.util.TransformerUtil
import java.util.concurrent.ExecutionException

/**
 * 适配封面-第一帧
 *
 * @author wangshichao
 * @date 2024/7/24
 */
class CoverOverlay(
    private val context: Context,
    private val url: String
) : BitmapOverlay() {
    private val TAG = javaClass.simpleName
    private var overlaySettings: OverlaySettings = OverlaySettings.Builder().build()
    private var lastBitmap: Bitmap? = null

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        if (lastBitmap == null) {
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
        return lastBitmap!!
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

}