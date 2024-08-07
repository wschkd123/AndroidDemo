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
    val tag: String,
    private val chatMsg: ChatMsgItem,
    enableAudio: Boolean = true
) {
    // 视图绘制
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubbleRectF: RectF
    private val bubbleLeft: Float = 54f
    private val bubblePaddingHorizontal = 30f
    private val audioLeft: Float
    private val audioTop: Float
    private val fitNickname: String

    /**
     * 背景图
     */
    private val srcBitmap: Bitmap =
        TransformerUtil.loadImage(context, chatMsg.getChatBoxBgResId(), FRAME_WIDTH)
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
        fitNickname = fitTextMaxWidth(chatMsg.nickname)
        val textWidth = paint.measureText(fitNickname)
        val drawablePadding = 12f
        val bubbleWidth = if (enableAudio && chatMsg.havaAudio()) {
            textWidth + drawablePadding + AudioTrackHelper.ICON_SIZE + bubblePaddingHorizontal.times(2)
        } else {
            textWidth + bubblePaddingHorizontal.times(2)
        }
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
            canvas.drawBitmap(srcBitmap, 0f, 0f, paint)
            drawBubbleView(canvas)
            Log.d(tag, "drawContainerView: cost=${System.currentTimeMillis() - start}")
            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }

    /**
     * 绘制气泡容器。内部有昵称、音频图标
     */
    private fun drawBubbleView(canvas: Canvas) {
        // 绘制背景
        val bubbleBgBitmap = TransformerUtil.loadImage(
            context,
            R.drawable.bubble_bg,
            bubbleRectF.width().toInt(),
            bubbleRectF.height().toInt()
        )
        canvas.drawBitmap(bubbleBgBitmap, bubbleLeft, 0f, paint)

        // 绘制昵称
        val fontMetrics: Paint.FontMetrics = paint.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        val baseline: Float = bubbleRectF.centerY() + distance
        val textX = bubbleLeft + bubblePaddingHorizontal
        canvas.drawText(fitNickname, textX, baseline, paint)
    }

    /**
     * 适配文本最大宽度。超出最大宽度添加省略号
     */
    private fun fitTextMaxWidth(input: String?): String {
        var text = input ?: ""
        var textWidth = paint.measureText(text)
        val maxWidth = 450f
        if (textWidth <= maxWidth) {
            return text
        }
        // 文本超出最大宽度，需要添加省略号
        while (textWidth > maxWidth - paint.measureText("...")) {
            // 省略字符直到符合最大宽度
            text = text.substring(0, text.length - 1)
            textWidth = paint.measureText(text)
        }
        return "$text..."
    }

    /**
     * 添加音频图标到文本框上
     *
     * @param srcBitmap 源Bitmap
     * @param nextBitmap 是否使用下一张音轨图片
     */
    fun addAudioView(srcBitmap: Bitmap, nextBitmap: Boolean): Bitmap {
        val targetBitmap = srcBitmap.copy(srcBitmap.config, true)
        try {
            val canvas = Canvas(targetBitmap)
            val start = System.currentTimeMillis()
            val bitmap = if(nextBitmap) audioTrackHelper.getNextBitmap() else audioTrackHelper.getCurBitmap()
            canvas.drawBitmap(bitmap, audioLeft, audioTop, paint)
            Log.d(tag, "addAudioView: cost=${System.currentTimeMillis() - start}")
            canvas.setBitmap(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return targetBitmap
    }

}