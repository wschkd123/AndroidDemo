package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.opengl.Matrix
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.GlUtil
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.base.util.ext.resToColor
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
    private var lastAudioTimeUs: Long = 0
    private val text = "林泽林泽林泽"

    companion object {
        // 文本框整体宽高
        private const val FRAME_WIDTH = 1020
        private const val FRAME_HEIGHT = 361
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
        if (isPlaying == playing) {
            return
        }
        audioTrackHelper.reset()
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
                lastBitmap = createContainerBitmap(srcBitmap)
            }
        }

        // 绘制音轨。每200毫秒重绘一帧实现动画
        val audioPeriod = presentationTimeUs - lastAudioTimeUs
        if (presentationTimeUs > endTimeUs && isPlaying && audioPeriod > 200 * C.MILLIS_PER_SECOND) {
            val bgBitmap = createContainerBitmap(srcBitmap)
//            lastBitmap = bgBitmap
            lastBitmap = addAudioView(bgBitmap)
            lastAudioTimeUs = presentationTimeUs
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
     * 创建容器。包括文本框内部气泡等
     */
    private fun createContainerBitmap(
        srcBitmap: Bitmap,
    ): Bitmap {
        val targetBitmap = Bitmap.createBitmap(
            FRAME_WIDTH,
            FRAME_HEIGHT,
            Bitmap.Config.ARGB_8888
        )
        try {
            val canvas = Canvas(targetBitmap)
            val start = System.currentTimeMillis()
            canvas.drawBitmap(srcBitmap, matrix, paint)
            drawBubbleView(canvas)
            Log.d(TAG, "createNewBitmap: drawBitmap cost=${System.currentTimeMillis() - start}")
            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }

    private fun drawBubbleView(canvas: Canvas) {
        paint.apply {
            textAlign = Paint.Align.LEFT
            textSize = 42f
            color = R.color.video_create_nick_text.resToColor(context)
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }
        val textWidth = paint.measureText(text)
        val marginStart = 54f
        val paddingHorizontal = 30f
        val drawablePadding = 12f
        val bubbleWidth = textWidth + drawablePadding + AudioTrackHelper.ICON_SIZE + paddingHorizontal.times(2)
        val bubbleHeight = 78f
        val bubbleRectF = RectF(0f, 0f, bubbleWidth, bubbleHeight)

        // 绘制背景
        val bubbleBgBitmap = TransformerUtil.loadImage(context, R.drawable.bubble_bg, bubbleRectF.width().toInt(), bubbleRectF.height().toInt())
            ?: TransformerUtil.createEmptyBitmap()
        canvas.drawBitmap(bubbleBgBitmap, marginStart, 0f, paint)

        // 绘制文本
        val fontMetrics: Paint.FontMetrics = paint.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        val baseline: Float = bubbleRectF.centerY() + distance
        val textX = marginStart + paddingHorizontal
        canvas.drawText(text, textX, baseline, paint)
    }

    /**
     * 添加音频视图
     *
     * @param srcBitmap 源Bitmap
     */
    private fun addAudioView(srcBitmap: Bitmap): Bitmap {
        val targetBitmap = srcBitmap.copy(srcBitmap.config, true)
        try {
            val canvas = Canvas(targetBitmap)
            val start = System.currentTimeMillis()
            drawAudioView(canvas)
            Log.d(TAG, "addBitmap: drawBitmap cost=${System.currentTimeMillis() - start}")
            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }

    /**
     * 绘制音频视图
     */
    private fun drawAudioView(canvas: Canvas) {
        val textWidth = paint.measureText(text)
        val marginStart = 54f
        val paddingHorizontal = 30f
        val drawablePadding = 12f
        val bubbleWidth = textWidth + drawablePadding + AudioTrackHelper.ICON_SIZE + paddingHorizontal.times(2)
        val bubbleHeight = 78f
        val bubbleRectF = RectF(0f, 0f, bubbleWidth, bubbleHeight)

        val iconLeft = marginStart + paddingHorizontal + textWidth + drawablePadding
        val iconTop = bubbleRectF.centerY() - AudioTrackHelper.ICON_SIZE.div(2)
        canvas.drawBitmap(audioTrackHelper.getNextBitmap(), iconLeft, iconTop, paint)
    }

}