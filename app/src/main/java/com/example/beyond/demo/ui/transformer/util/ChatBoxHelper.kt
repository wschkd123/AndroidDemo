package com.example.beyond.demo.ui.transformer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import com.example.base.util.ext.resToColor
import com.example.beyond.demo.R
import com.example.beyond.demo.ui.transformer.ChatMsgItem

/**
 * 文本框辅助类。包括所有元素的绘制
 *
 * @author wangshichao
 * @date 2024/7/29
 */
class ChatBoxHelper(
    val context: Context,
    val TAG: String,
    val chatMsg: ChatMsgItem,
) {
    // 视图绘制
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix: android.graphics.Matrix = android.graphics.Matrix()
    private val bubbleRectF: RectF
    private val bubbleLeft: Float = 54f
    private val bubblePaddingHorizontal = 30f
    private val audioLeft: Float
    private val audioTop: Float

    /**
     * 背景图
     */
    private val srcBitmap: Bitmap =
        TransformerUtil.loadImage(context, chatMsg.getChatBoxBgResId(), FRAME_WIDTH)
            ?: TransformerUtil.createEmptyBitmap()
    private val audioTrackHelper: AudioTrackHelper = AudioTrackHelper(context)

    companion object {
        // 文本框整体宽高
        private const val FRAME_WIDTH = 1020
        private const val FRAME_HEIGHT = 361
    }

    init {
        // 昵称画笔
        paint.apply {
            textAlign = Paint.Align.LEFT
            textSize = 42f
            color = R.color.video_create_nick_text.resToColor(context)
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }

        // 气泡相对容器位置
        val textWidth = paint.measureText(chatMsg.nickname ?: "")
        val drawablePadding = 12f
        val audioWidth = if (chatMsg.havaAudio()) AudioTrackHelper.ICON_SIZE else 0
        val bubbleWidth =
            textWidth + drawablePadding + audioWidth + bubblePaddingHorizontal.times(2)
        val bubbleHeight = 78f
        bubbleRectF = RectF(0f, 0f, bubbleWidth, bubbleHeight)

        // audio相对容器位置
        audioLeft = bubbleLeft + bubblePaddingHorizontal + textWidth + drawablePadding
        audioTop = bubbleRectF.centerY() - AudioTrackHelper.ICON_SIZE.div(2)
    }

    /**
     * 绘制文本框容器。包括文本框内部气泡等
     */
    fun drawContainerView(): Bitmap {
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
     * 绘制气泡容器。内部有昵称、音频图标
     */
    fun drawBubbleView(canvas: Canvas) {
        // 绘制背景
        val bubbleBgBitmap = TransformerUtil.loadImage(
            context,
            R.drawable.bubble_bg,
            bubbleRectF.width().toInt(),
            bubbleRectF.height().toInt()
        )
            ?: TransformerUtil.createEmptyBitmap()
        canvas.drawBitmap(bubbleBgBitmap, bubbleLeft, 0f, paint)

        // 绘制昵称
        val fontMetrics: Paint.FontMetrics = paint.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        val baseline: Float = bubbleRectF.centerY() + distance
        val textX = bubbleLeft + bubblePaddingHorizontal
        canvas.drawText(chatMsg.nickname ?: "", textX, baseline, paint)
    }

    /**
     * 添加音频图标到文本框上
     *
     * @param srcBitmap 源Bitmap
     */
    fun addAudioView(srcBitmap: Bitmap): Bitmap {
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