package com.example.beyond.demo.ui.player.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 音频片段播放数据源
 */
@Parcelize
data class MediaDataSource(
    /**
     * 本次tts请求的id
     */
    val traceId: String? = null,

    /**
     * 本次tts key。用于用于请求、播放和下载
     */
    val ttsKey: String,

    /**
     * 音频片段
     */
    val audioChunk: AudioChunk
) : Parcelable {

    @Parcelize
    data class AudioChunk(
        /**
         * 音频片段缓存路径
         */
        val chunkPath: String,
        /**
         * 是否是最后一个完整资源
         */
        val isLastComplete: Boolean,
    ) : Parcelable


}