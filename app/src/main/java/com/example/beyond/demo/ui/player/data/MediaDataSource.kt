package com.example.beyond.demo.ui.player.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 音频片段播放数据资源
 */
@Parcelize
data class MediaDataSource(
    /**
     * 本次会话的id
     */
    val traceId: String,
    /**
     * 音频片段路径
     */
    val chunkPath: String,
    /**
     * 生成的音频格式。默认mp3，范围[mp3,pcm,flac]
     */
    val format: String,
    /**
     * 是否合成结束
     */
    val isEnd: Boolean,
    val chunkIndex: Int = 0
) : Parcelable {
    /**
     * 新的资源
     */
    fun isNewSource() = chunkIndex == 0
}