package com.example.base.player

/**
 * 播放器监听
 */
interface OnPlayerListener {
    /**
     * 播放状态变化
     */
    fun onPlaybackStateChanged(playKey: String, playState: Int)

    /**
     * 播放错误
     */
    fun onPlayerError(uri: String, playKey: String, desc: String)
}