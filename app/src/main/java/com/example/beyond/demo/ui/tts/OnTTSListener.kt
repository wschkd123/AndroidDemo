package com.example.beyond.demo.ui.tts

/**
 * tts监听
 */
interface OnTTSListener {
    /**
     * tts状态变化。包括请求和播放状态
     */
    fun onTTSStateChanged(playKey: String, playState: Int)

}