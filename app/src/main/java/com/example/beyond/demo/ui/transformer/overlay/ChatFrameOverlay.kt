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
import com.example.beyond.demo.ui.transformer.util.ReflectUtil
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
    private val emptyBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    // 文本框背景
    private val paint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix: android.graphics.Matrix = android.graphics.Matrix()
    private var frameBitmap: Bitmap? = null
    private var canvas: Canvas? = null

    /**
     * 原背景图
     */
    private var srcBitmap: Bitmap? = null

    /**
     * 上一帧图
     */
    private var lastBitmap: Bitmap? = null

    companion object {
        private const val FRAME_WIDTH = 1029
        private const val FRAME_HEIGHT = 354
        private const val TRANSLATE_DISTANCE = 354 * 2
    }

    init {
        // 覆盖物在视频底部以下
        Matrix.translateM(translateMatrix, 0, 0f, -1f, 1f)
        overlaySettings = OverlaySettings.Builder()
            .setMatrix(translateMatrix)
            .setAnchor(0f, 1f)
            .build()
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        // 不在指定的时间范围，返回最后一帧
        if (presentationTimeUs !in startTimeUs..endTimeUs) {
            Log.d(TAG, "getBitmap: use last frame")
            return lastBitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val startTime = System.currentTimeMillis()
        Log.d(TAG, "getBitmap: start")
        if (lastBitmap == null) {
            lastBitmap = createNewBitmapOnce(getFrameBg())
        }
        //TODO 方案一，通过改变overlaySettings实现平移
        updateAnimation(presentationTimeUs)

        Log.d(TAG, "getBitmap: cost ${System.currentTimeMillis() - startTime}")
        return lastBitmap!!
    }

    /**
     * 动画：从视频底部向上移动至画面中下方，位移过程中透明度从0%-100%，0.4秒内完成
     */
    private fun updateAnimation(presentationTimeUs: Long) {
        val animatedValue = (presentationTimeUs - startTimeUs).toFloat().div(durationUs)
        Log.w(
            TAG,
            "getBitmap: animatedValue=$animatedValue presentationTimeMs=$presentationTimeUs"
        )
        val translateMatrix: FloatArray = GlUtil.create4x4IdentityMatrix()
        Matrix.translateM(translateMatrix, 0, 0f, animatedValue - 1, 1f)
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "matrix", translateMatrix)
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alpha", animatedValue)
    }

    private fun getFrameBg(): Bitmap {
        if (srcBitmap == null || srcBitmap?.isRecycled == true) {
            Log.d(TAG, "getBitmap: loadImage")
            srcBitmap = TransformerUtil.loadImage(
                R.drawable.user_frame,
                FRAME_WIDTH,
                FRAME_HEIGHT
            ) ?: return emptyBitmap
        }
        return srcBitmap!!
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

    /**
     * 方案二，不断重绘Bitmap实现
     */
    private fun createNewBitmap(
        srcBitmap: Bitmap,
        animatedValue: Float
    ): Bitmap {
        val offset = TRANSLATE_DISTANCE * animatedValue
        val dy = TRANSLATE_DISTANCE - offset
        matrix.postTranslate(0f, dy)
        try {
            frameBitmap = Bitmap.createBitmap(
                FRAME_WIDTH,
                TRANSLATE_DISTANCE,
                Bitmap.Config.ARGB_8888
            )
            canvas = Canvas(frameBitmap!!)
            val start = System.currentTimeMillis()
            //TODO 平均耗时6ms
            canvas!!.drawBitmap(srcBitmap, matrix, paint)
            Log.d(TAG, "createNewBitmap: drawBitmap cost=${System.currentTimeMillis() - start} dy=$dy")
            canvas!!.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return frameBitmap!!
    }

    private fun createNewBitmapOnce(
        srcBitmap: Bitmap,
    ): Bitmap {
        try {
            frameBitmap = Bitmap.createBitmap(
                FRAME_WIDTH,
                FRAME_HEIGHT,
                Bitmap.Config.ARGB_8888
            )
            canvas = Canvas(frameBitmap!!)
            val start = System.currentTimeMillis()
            canvas!!.drawBitmap(srcBitmap, matrix, paint)
            Log.d(TAG, "createNewBitmap: drawBitmap cost=${System.currentTimeMillis() - start}")
            canvas!!.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return frameBitmap!!
    }

}