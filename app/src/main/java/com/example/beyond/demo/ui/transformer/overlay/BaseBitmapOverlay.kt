package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.ui.transformer.util.TransformerUtil
import java.util.concurrent.ExecutionException

/**
 * 图片url基类。支持在指定时间范围内显示
 *
 * @author wangshichao
 * @date 2024/7/25
 */
@UnstableApi
open class BaseBitmapOverlay(
    private val context: Context,
    private val url: String,
    private val durationUs: Long
) : BitmapOverlay() {
    protected val TAG = javaClass.simpleName
    protected val overlaySettings: OverlaySettings = OverlaySettings.Builder().build()
    protected var lastBitmap: Bitmap? = null
    protected var startTimeUs: Long = 0L
    private var endTimeUs: Long =  durationUs

    /**
     * 剪裁原Bitmap
     */
    open fun cropBitmap(srcBitmap: Bitmap): Bitmap {
        return srcBitmap
    }

    /**
     * 更新动画
     */
    open fun updateAnimation(presentationTimeUs: Long) {

    }

    open fun animationEnd(): Bitmap {
        return TransformerUtil.createEmptyBitmap()
    }


    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        Log.d(TAG, "getBitmap: presentationTimeUs=$presentationTimeUs")
        // 首帧记录开始和结束时间
        if (startTimeUs <= 0L) {
            startTimeUs = presentationTimeUs
            endTimeUs = startTimeUs + durationUs
        }

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
            lastBitmap = cropBitmap(bitmap)
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

}