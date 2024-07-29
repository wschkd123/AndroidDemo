package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import android.util.Pair
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
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
 * @param durationUs 持续时间
 */
@UnstableApi
class TextBoxOverlay(
    private val context: Context,
    private val durationUs: Long
) : BitmapOverlay() {
    private val TAG = javaClass.simpleName
    private val overlaySettings: OverlaySettings = OverlaySettings.Builder()
        // 覆盖物在视频底部以下
        .setBackgroundFrameAnchor(0f, -1f)
        // 在原覆盖物下面的位置
        .setOverlayFrameAnchor(0f, 1f)
        .build()

    // 视图绘制
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix: android.graphics.Matrix = android.graphics.Matrix()
    private val bubbleRectF: RectF
    private val bubbleLeft: Float = 54f
    private val bubblePaddingHorizontal = 30f
    private val audioLeft: Float
    private val audioTop: Float

    // 音频动画
    private val audioTrackHelper: AudioTrackHelper = AudioTrackHelper(context)
    private var isPlaying: Boolean = true
    private var lastAudioTimeUs: Long = 0

    /**
     * 原背景图
     */
    private val srcBitmap: Bitmap

    /**
     * 上一帧图
     */
    private var lastBitmap: Bitmap? = null
    private var startTimeUs: Long = 0L
    private var endTimeUs: Long = durationUs
    private val nickname = "林泽林泽林泽"

    companion object {
        // 文本框整体宽高
        private const val FRAME_WIDTH = 1020
        private const val FRAME_HEIGHT = 361
    }

    init {
        srcBitmap =
            TransformerUtil.loadImage(context, R.drawable.user_text_bg, FRAME_WIDTH)
                ?: TransformerUtil.createEmptyBitmap()
        // 昵称画笔
        paint.apply {
            textAlign = Paint.Align.LEFT
            textSize = 42f
            color = R.color.video_create_nick_text.resToColor(context)
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }

        // 气泡相对容器位置
        val textWidth = paint.measureText(nickname)
        val drawablePadding = 12f
        val bubbleWidth = textWidth + drawablePadding + AudioTrackHelper.ICON_SIZE + bubblePaddingHorizontal.times(2)
        val bubbleHeight = 78f
        bubbleRectF = RectF(0f, 0f, bubbleWidth, bubbleHeight)

        // audio相对容器位置
        audioLeft = bubbleLeft + bubblePaddingHorizontal + textWidth + drawablePadding
        audioTop = bubbleRectF.centerY() - AudioTrackHelper.ICON_SIZE.div(2)
    }

    fun setAudioPlayState(playing: Boolean) {
        if (isPlaying == playing) {
            return
        }
        audioTrackHelper.reset()
        this.isPlaying = playing
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        Log.d(TAG, "getBitmap: presentationTimeMs=$presentationTimeUs")
        // 首帧记录开始和结束时间
        if (startTimeUs <= 0L) {
            startTimeUs = presentationTimeUs
            endTimeUs = startTimeUs + durationUs
        }
        val startTime = System.currentTimeMillis()

        // 整体文本框平移和渐显动画
        if (presentationTimeUs in startTimeUs..endTimeUs) {
            val animatedValue = (presentationTimeUs - startTimeUs).toFloat().div(durationUs)
            Log.i(TAG, "getBitmap: animatedValue=$animatedValue")
            updateBgAnimation(animatedValue)
            if (lastBitmap == null) {
                lastBitmap = drawContainerView(srcBitmap)
            }
        }

        // 绘制音轨。每200毫秒重绘一帧实现动画
        val audioPeriod = presentationTimeUs - lastAudioTimeUs
        if (presentationTimeUs > endTimeUs && isPlaying && audioPeriod > 200 * C.MILLIS_PER_SECOND) {
            Log.d(TAG, "getBitmap: isPlaying")
            val bgBitmap = drawContainerView(srcBitmap)
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
        val backgroundFrameAnchor = Pair.create(0f, curY)
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "backgroundFrameAnchor", backgroundFrameAnchor)
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alphaScale", animatedValue)
    }


    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

    /**
     * 绘制容器。包括文本框内部气泡等
     */
    private fun drawContainerView(
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
            Log.d(TAG, "drawContainerView: cost=${System.currentTimeMillis() - start}")
            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }

    /**
     * 绘制气泡视图
     */
    private fun drawBubbleView(canvas: Canvas) {
        // 绘制背景
        val bubbleBgBitmap = TransformerUtil.loadImage(context, R.drawable.bubble_bg, bubbleRectF.width().toInt(), bubbleRectF.height().toInt())
            ?: TransformerUtil.createEmptyBitmap()
        canvas.drawBitmap(bubbleBgBitmap, bubbleLeft, 0f, paint)

        // 绘制昵称
        val fontMetrics: Paint.FontMetrics = paint.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        val baseline: Float = bubbleRectF.centerY() + distance
        val textX = bubbleLeft + bubblePaddingHorizontal
        canvas.drawText(nickname, textX, baseline, paint)
    }

    /**
     * 添加音频视图到原视图上
     *
     * @param srcBitmap 源Bitmap
     */
    private fun addAudioView(srcBitmap: Bitmap): Bitmap {
        val targetBitmap = srcBitmap.copy(srcBitmap.config, true)
        try {
            val canvas = Canvas(targetBitmap)
            val start = System.currentTimeMillis()
            canvas.drawBitmap(audioTrackHelper.getNextBitmap(), audioLeft, audioTop, paint)
            Log.d(TAG, "addAudioView: cost=${System.currentTimeMillis() - start}")
            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }

}