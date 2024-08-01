package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.ui.transformer.ChatMsgItem
import com.example.beyond.demo.ui.transformer.util.ChatBoxHelper
import com.example.beyond.demo.ui.transformer.util.ReflectUtil

/**
 * 聊天文本框渐隐
 *
 * 1. 动画：透明度从100%-0%
 *
 * @author wangshichao
 * @date 2024/7/24
 * @param durationUs 持续时间
 */
@UnstableApi
class ChatBoxOutOverlay(
    context: Context,
    val chatMsg: ChatMsgItem,
    val durationUs: Long
) : BitmapOverlay() {
    private val TAG = javaClass.simpleName
    private val overlaySettings: OverlaySettings = OverlaySettings.Builder()
        // 覆盖物在视频中下部
        .setBackgroundFrameAnchor(0f, -0.3f)
        // 在原覆盖物下面的位置
        .setOverlayFrameAnchor(0f, -1f)
        .build()

    private val chatBoxHelper = ChatBoxHelper(context, TAG, chatMsg)

    /**
     * 上一帧图
     */
    private var lastBitmap: Bitmap? = null
    private var startTimeUs: Long = 0L

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        // 首帧记录开始和结束时间
        if (startTimeUs <= 0L) {
            startTimeUs = presentationTimeUs
        }

        // 整体文本框平移和渐显动画
        val startTime = System.currentTimeMillis()
        val animatedValue = (presentationTimeUs - startTimeUs).toFloat().div(durationUs)
        Log.i(TAG, "getBitmap: startTimeUs=$startTimeUs durationUs=${durationUs} presentationTimeUs=$presentationTimeUs")
        updateBgAnimation(animatedValue)
        if (lastBitmap == null) {
            lastBitmap = chatBoxHelper.drawContainerView()
        }

        // 绘制音轨
        if (chatMsg.havaAudio()) {
            Log.d(TAG, "getBitmap: isPlaying")
            val bgBitmap = chatBoxHelper.drawContainerView()
            lastBitmap = chatBoxHelper.addAudioView(bgBitmap, false)
        }

        Log.d(TAG, "getBitmap: cost ${System.currentTimeMillis() - startTime}")
        return lastBitmap!!
    }

    /**
     * 透明度从0%-100%
     */
    private fun updateBgAnimation(animatedValue: Float) {
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alphaScale", 1 - animatedValue)
    }


    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

}