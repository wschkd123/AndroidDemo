package com.example.beyond.demo.ui.transformer.overlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.Matrix
import android.util.Log
import androidx.media3.common.util.GlUtil
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.R
import com.example.beyond.demo.ui.transformer.util.TransformerUtil

/**
 * 对话文本框
 *
 * 1. 动画：从视频底部向上移动至画面中下方，位移过程中透明度从0%-100%，0.4秒内完成
 *
 * @author wangshichao
 * @date 2024/7/24
 */
class ChatFrameOverlay(
    private val startTimeUs: Long,
    private val durationUs: Long
) : BitmapOverlay() {
    private val TAG = javaClass.simpleName
    private var overlaySettings: OverlaySettings
    private val endTimeUs: Long = startTimeUs + durationUs
    private val translateMatrix: FloatArray = GlUtil.create4x4IdentityMatrix()
    private var frameBitmap: Bitmap
    private val canvas: Canvas
    private val matrix: android.graphics.Matrix = android.graphics.Matrix()
    private val paint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var srcBitmap: Bitmap? = null
    private var lastBitmap: Bitmap? = null

    companion object {
        private const val FRAME_WIDTH = 1029
        private const val FRAME_HEIGHT = 354
        private const val TRANSLATE_DISTANCE = 100
    }

    init {
        // 覆盖物在视频底部
        Matrix.translateM(translateMatrix, 0, 0f, -1f, 1f)
        overlaySettings = OverlaySettings.Builder()
            .setMatrix(translateMatrix)
            .setAnchor(0f, -1f)
            .build()
        frameBitmap = Bitmap.createBitmap(
            FRAME_WIDTH,
            TRANSLATE_DISTANCE,
            Bitmap.Config.ARGB_8888
        )
        canvas = Canvas(frameBitmap)
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        // 不在指定的时间范围，返回最后一帧
        val emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        if (presentationTimeUs !in startTimeUs..endTimeUs || lastBitmap != null) {
            Log.d(TAG, "getBitmap: use last frame")
            return lastBitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val startTime = System.currentTimeMillis()
        Log.d(TAG, "getBitmap: start")
        if (srcBitmap == null || srcBitmap?.isRecycled == true) {
            Log.d(TAG, "getBitmap: loadImage")
            srcBitmap = TransformerUtil.loadImage(
                R.drawable.user_frame,
                FRAME_WIDTH,
                FRAME_HEIGHT
            ) ?: return emptyBitmap
        }

        // 动画：从视频底部向上移动至画面中下方，位移过程中透明度从0%-100%，0.4秒内完成
        val animatedValue = (presentationTimeUs - startTimeUs).toFloat().div(durationUs)
        Log.w(
            TAG,
            "getBitmap: animatedValue=$animatedValue presentationTimeMs=$presentationTimeUs"
        )
        lastBitmap = createNewBitmap(srcBitmap!!, animatedValue)
//        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alpha", animatedValue)
        Log.d(TAG, "getBitmap: cost ${System.currentTimeMillis() - startTime}")
        return lastBitmap!!
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

    private fun createNewBitmap(
        srcBitmap: Bitmap,
        animatedValue: Float
    ): Bitmap {
        val offset = TRANSLATE_DISTANCE * animatedValue
//        val frameBitmap = Bitmap.createBitmap(
//            srcBitmap.width,
//            TRANSLATE_DISTANCE,
//            Bitmap.Config.ARGB_8888
//        )
        val dy = TRANSLATE_DISTANCE - offset
        try {
//            val canvas = Canvas(frameBitmap)
//            val matrix = android.graphics.Matrix()
//            matrix.postTranslate(0f, dy)
//            val paint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            //                alpha = (animatedValue * 100).toInt()
            canvas.drawBitmap(srcBitmap, matrix, paint)
//            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return frameBitmap
    }

}