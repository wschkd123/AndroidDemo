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
) : IgnoreProguard() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkDataSource

        if (traceId != other.traceId) return false
        if (ttsKey != other.ttsKey) return false
        if (chunkPath != other.chunkPath) return false
        return audioData.contentEquals(other.audioData)
    }

    override fun hashCode(): Int {
        var result = traceId?.hashCode() ?: 0
        result = 31 * result + ttsKey.hashCode()
        result = 31 * result + chunkPath.hashCode()
        result = 31 * result + audioData.contentHashCode()
        return result
    }
}