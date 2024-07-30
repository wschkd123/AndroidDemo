package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Pair
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.example.beyond.demo.ui.transformer.ChatMsgItem
import com.example.beyond.demo.ui.transformer.util.ChatBoxHelper
import com.example.beyond.demo.ui.transformer.util.ReflectUtil
import com.example.beyond.demo.ui.transformer.util.TransformerUtil

/**
 * 聊天文本框移到中下方且渐显
 *
 * 1. 动画：从视频底部向上移动至画面中下方，位移过程中透明度从0%-100%，0.4秒内完成
 *
 * @author wangshichao
 * @date 2024/7/24
 * @param durationUs 持续时间
 */
@UnstableApi
class ChatBoxInOverlay(
    context: Context,
    chatMsg: ChatMsgItem,
    val durationUs: Long
) : BitmapOverlay() {
    private val TAG = javaClass.simpleName
    private val overlaySettings: OverlaySettings = OverlaySettings.Builder()
        // 覆盖物在视频底部以下
        .setBackgroundFrameAnchor(0f, -1f)
        // 在原覆盖物下面的位置，屏幕以下
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
        Log.i(TAG, "getBitmap: startTimeUs=$startTimeUs presentationTimeUs=$presentationTimeUs animatedValue=$animatedValue")
        updateBgAnimation(animatedValue)
        if (lastBitmap == null) {
            lastBitmap = chatBoxHelper.drawContainerView()
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
        Log.i(TAG, "animatedValue=$animatedValue curY=$curY")
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "backgroundFrameAnchor", backgroundFrameAnchor)
        ReflectUtil.updateOverlaySettingsFiled(overlaySettings, "alphaScale", animatedValue)
    }


    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings
    }

}