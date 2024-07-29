package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.ui.transformer.util.AudioTrackHelper
import com.example.beyond.demo.ui.transformer.util.ChatBoxHelper
import com.example.beyond.demo.ui.transformer.util.TransformerUtil

/**
 * 聊天文本框
 *
 * @author wangshichao
 * @date 2024/7/24
 * @param durationUs 持续时间
 */
@UnstableApi
class ChatBoxOverlay(
    private val context: Context,
    private val durationUs: Long
) : BitmapOverlay() {
    private val TAG = javaClass.simpleName
    private val overlaySettings: OverlaySettings = OverlaySettings.Builder()
        // 覆盖物在视频底部以下
        .setBackgroundFrameAnchor(0f, -0.3f)
        // 在原覆盖物下面的位置
        .setOverlayFrameAnchor(0f, 1f)
        .build()

    private val chatBoxHelper: ChatBoxHelper = ChatBoxHelper(context, TAG, "林泽林泽林泽")

    // 音频
    private val audioTrackHelper: AudioTrackHelper = AudioTrackHelper(context)
    private var isPlaying: Boolean = true
    private var lastAudioTimeUs: Long = 0

    /**
     * 上一帧图
     */
    private var lastBitmap: Bitmap? = null
    private var startTimeUs: Long = 0L
    private var endTimeUs: Long = durationUs

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

        // 初始化整体文本框
        if (lastBitmap == null) {
            lastBitmap = chatBoxHelper.drawContainerView()
        }

        // 绘制音轨。每200毫秒重绘一帧实现动画
        val audioPeriod = presentationTimeUs - lastAudioTimeUs
        if (isPlaying && audioPeriod > 200 * C.MILLIS_PER_SECOND) {
            Log.d(TAG, "getBitmap: isPlaying")
            val bgBitmap = chatBoxHelper.drawContainerView()
            lastBitmap = chatBoxHelper.addAudioView(bgBitmap)
            lastAudioTimeUs = presentationTimeUs
        }
        Log.d(TAG, "getBitmap: cost ${System.currentTimeMillis() - startTime}")
        return lastBitmap ?: TransformerUtil.createEmptyBitmap()
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

}