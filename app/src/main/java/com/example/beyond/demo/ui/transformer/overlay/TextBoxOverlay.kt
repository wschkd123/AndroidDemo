package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.Matrix
import android.util.Log
import androidx.media3.common.util.GlUtil
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.R
import com.example.beyond.demo.ui.transformer.util.AudioTrackHelper
import com.example.beyond.demo.ui.transformer.util.ReflectUtil
import com.example.beyond.demo.ui.transformer.util.TransformerUtil

/**
 * 聊天文本框覆盖物
 *
 * 1. 动画：从视频底部向上移动至画面中下方，位移过程中透明度从0%-100%，0.4秒内完成
 *
 * @author wangshichao
 * @date 2024/7/24
 * @param startTimeUs 整体动画开始时间
 * @param durationUs 持续时间
 */
class TextBoxOverlay(
    private val context: Context,
    private val startTimeUs: Long,
    private val durationUs: Long
) : BitmapOverlay() {
    private val TAG = javaClass.simpleName
    private var overlaySettings: OverlaySettings
    private val endTimeUs: Long = startTimeUs + durationUs
    private val translateMatrix: FloatArray = GlUtil.create4x4IdentityMatrix()
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
    // 音频动画
    private val audioTrackHelper: AudioTrackHelper = AudioTrackHelper(context)
    private var isPlaying: Boolean = true

    companion object {
        private const val FRAME_WIDTH = 1020
        private const val FRAME_HEIGHT = 361
        private const val TRANSLATE_DISTANCE = 240
    }

    init {
        // 覆盖物在视频底部以下
        Matrix.translateM(translateMatrix, 0, 0f, 0f, 1f)
        overlaySettings = OverlaySettings.Builder()
            .setMatrix(translateMatrix)
            .setAnchor(0f, 1f)
            .build()
    }

    fun updateAudioTrack(playing: Boolean) {
        this.isPlaying = playing
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val startTime = System.currentTimeMillis()
        // 不在指定的时间范围，返回最后一帧
//        if (presentationTimeUs !in startTimeUs..endTimeUs) {
//            Log.d(TAG, "getBitmap: use last frame")
//            return lastBitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
//        }

        // 整体文本框动画
        if (presentationTimeUs in startTimeUs..endTimeUs) {
//            return lastBitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            val animatedValue = (presentationTimeUs - startTimeUs).toFloat().div(durationUs)
            Log.i(
                TAG,
                "getBitmap: animatedValue=$animatedValue presentationTimeMs=$presentationTimeUs"
            )
            //TODO 方案一，通过改变overlaySettings实现平移
            updateBgAnimation(animatedValue)
            if (lastBitmap == null) {
                lastBitmap = createNewBitmapOnce(getFrameBg())
            }
        }

        // 绘制音轨
        if (presentationTimeUs > endTimeUs && isPlaying) {
            lastBitmap = TransformerUtil.addBitmap(lastBitmap!!, audioTrackHelper.getNextBitmap())
        }
        Log.d(TAG, "getBitmap: cost ${System.currentTimeMillis() - startTime}")
        return lastBitmap ?: TransformerUtil.createEmptyBitmap()
    }

    /**
     * 动画：从视频底部向上移动至画面中下方，位移过程中透明度从0%-100%，0.4秒内完成
     */
    private fun updateBgAnimation(animatedValue: Float) {
        val translateMatrix: FloatArray = GlUtil.create4x4IdentityMatrix()
        Matrix.translateM(translateMatrix, 0, 0f, animatedValue - 1, 1f)
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "matrix", translateMatrix)
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alpha", animatedValue)
    }

    private fun getFrameBg(): Bitmap {
        if (srcBitmap == null || srcBitmap?.isRecycled == true) {
            Log.d(TAG, "getBitmap: loadImage")
            srcBitmap = TransformerUtil.loadImage(
                R.drawable.user_text_bg,
                FRAME_WIDTH,
                FRAME_HEIGHT
            ) ?: return TransformerUtil.createEmptyBitmap()

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
        matrix.setTranslate(0f, dy)
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
            canvas!!.drawBitmap(audioTrackHelper.getFirstBitmap(), 0f, 0f, paint)
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
            canvas!!.drawBitmap(audioTrackHelper.getFirstBitmap(), 54f, 0f, paint)
            Log.d(TAG, "createNewBitmap: drawBitmap cost=${System.currentTimeMillis() - start}")
            canvas!!.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return frameBitmap!!
    }

}