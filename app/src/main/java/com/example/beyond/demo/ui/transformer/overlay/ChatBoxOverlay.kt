package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.ui.transformer.ChatMsgItem
import com.example.beyond.demo.ui.transformer.util.ChatBoxHelper
import com.example.beyond.demo.ui.transformer.util.TransformerUtil

/**
 * 聊天文本框
 *
 * @author wangshichao
 * @date 2024/7/24
 */
@UnstableApi
class ChatBoxOverlay(
    context: Context,
    val chatMsg: ChatMsgItem,
) : BitmapOverlay() {
    private val TAG = javaClass.simpleName
    private val overlaySettings: OverlaySettings = OverlaySettings.Builder()
        // 覆盖物在视频中下部
        .setBackgroundFrameAnchor(0f, -0.3f)
        // 在原覆盖物下面的位置
        .setOverlayFrameAnchor(0f, -1f)
        .build()

    private val chatBoxHelper: ChatBoxHelper = ChatBoxHelper(context, TAG, chatMsg)

    // 长一帧音频
    private var lastAudioFrameTimeUs: Long = 0

    /**
     * 上一帧图
     */
    private var lastBitmap: Bitmap? = null
    private var startTimeUs: Long = 0L

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        Log.d(TAG, "getBitmap: presentationTimeMs=$presentationTimeUs")
        // 首帧记录开始和结束时间
        if (startTimeUs <= 0L) {
            startTimeUs = presentationTimeUs
        }
        val startTime = System.currentTimeMillis()

        // 初始化整体文本框
        if (lastBitmap == null) {
            lastBitmap = chatBoxHelper.drawContainerView()
        }

        // 绘制音轨。每200毫秒重绘一帧实现动画
        val audioPeriod = presentationTimeUs - lastAudioFrameTimeUs
        if (chatMsg.havaAudio() && audioPeriod > 200 * C.MILLIS_PER_SECOND) {
            Log.d(TAG, "getBitmap: isPlaying")
            val bgBitmap = chatBoxHelper.drawContainerView()
            lastBitmap = chatBoxHelper.addAudioView(bgBitmap)
            lastAudioFrameTimeUs = presentationTimeUs
        }
        Log.d(TAG, "getBitmap: cost ${System.currentTimeMillis() - startTime}")
        return lastBitmap ?: TransformerUtil.createEmptyBitmap()
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

}