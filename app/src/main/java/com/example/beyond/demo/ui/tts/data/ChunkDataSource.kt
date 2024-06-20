package com.example.beyond.demo.ui.tts.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 音频片段播放数据源
 */
@Parcelize
data class ChunkDataSource(
    /**
     * 本次tts请求的id
     */
    val traceId: String? = null,

    /**
     * 本次tts请求的key。用于用于请求、播放和下载
     */
    val ttsKey: String,

    /**
     * 获取的音频片段缓存路径
     */
    val chunkPath: String,
) : Parcelable