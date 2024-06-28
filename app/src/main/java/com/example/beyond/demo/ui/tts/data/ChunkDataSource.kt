package com.example.beyond.demo.ui.tts.data

import com.example.base.bean.IgnoreProguard

/**
 * 音频片段播放数据源
 */
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
    val audioData: ByteArray
) : IgnoreProguard()