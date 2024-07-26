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
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix: android.graphics.Matrix = android.graphics.Matrix()
    private var bgBitmap: Bitmap? = null
    private var canvas: Canvas? = null

    /**
     * 原背景图
     */
    private val srcBitmap: Bitmap

    /**
     * 上一帧图
     */
    private var lastBitmap: Bitmap? = null

    // 音频动画
    private val audioTrackHelper: AudioTrackHelper = AudioTrackHelper(context)
    private var isPlaying: Boolean = true

    companion object {
        // 文本框整体宽高
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
        srcBitmap =
            TransformerUtil.loadImage(context, R.drawable.user_text_bg, FRAME_WIDTH)
                ?: TransformerUtil.createEmptyBitmap()
    }

    fun setAudioPlayState(playing: Boolean) {
        this.isPlaying = playing
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val startTime = System.currentTimeMillis()

        // 整体文本框平移和渐显动画
        if (presentationTimeUs in startTimeUs..endTimeUs) {
            val animatedValue = (presentationTimeUs - startTimeUs).toFloat().div(durationUs)
            Log.i(
                TAG,
                "getBitmap: animatedValue=$animatedValue presentationTimeMs=$presentationTimeUs"
            )
            updateBgAnimation(animatedValue)
            if (lastBitmap == null) {
                lastBitmap = createBgBitmap(srcBitmap)
            }
        }

        // 绘制音轨
        if (presentationTimeUs > endTimeUs && isPlaying) {
            val bgBitmap = createBgBitmap(srcBitmap)
            lastBitmap = TransformerUtil.addBitmap(bgBitmap, audioTrackHelper.getNextBitmap())
        }
        Log.d(TAG, "getBitmap: cost ${System.currentTimeMillis() - startTime}")
        return lastBitmap ?: TransformerUtil.createEmptyBitmap()
    }

    /**
     * 动画：从视频底部向上移动至画面中下方，位移过程中透明度从0%-100%，0.4秒内完成
     */
    private fun updateBgAnimation(animatedValue: Float) {
        val startY = -1f
        val targetY = -0.3f
        val curY = startY + (targetY - startY) * animatedValue
        val translateMatrix: FloatArray = GlUtil.create4x4IdentityMatrix()
        Matrix.translateM(translateMatrix, 0, 0f, curY, 1f)
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "matrix", translateMatrix)
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alpha", animatedValue)
    }


    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

    /**
     * 创建背景图。比原背景尺寸大
     */
    private fun createBgBitmap(
        srcBitmap: Bitmap,
    ): Bitmap {
        try {
            bgBitmap = Bitmap.createBitmap(
                FRAME_WIDTH,
                FRAME_HEIGHT,
                Bitmap.Config.ARGB_8888
            )
            canvas = Canvas(bgBitmap!!)
            val start = System.currentTimeMillis()
            canvas!!.drawBitmap(srcBitmap, matrix, paint)
//            canvas!!.drawBitmap(audioTrackHelper.getFirstBitmap(), 54f, 0f, paint)
            Log.d(TAG, "createNewBitmap: drawBitmap cost=${System.currentTimeMillis() - start}")
            canvas!!.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bgBitmap!!
    }

}