package com.example.base.player.mediaplayer

/**
 * 多实例播放器控制器
 *
 * @author wangshichao
 * @date 2024/6/13
 */
class MultiMediaPlayerController {
    private val map by lazy {
        mutableMapOf<String, PlayerWrapper>()
    }

    private var onCompleteListener: ((key: String) -> Unit)? = null
    private var onPlaybackStateChangedListener: ((time: Float) -> Unit)? = null
    private var onErrorListener: ((key: String, desc: String) -> Unit)? = null

    fun prepare(
        url: String,
        key: String,
        onPrepareReady: (() -> Unit)? = null,
        onPrepareError: ((desc: String?) -> Unit)? = null
    ) {
        val player = PlayerWrapper()
        player.prepare(url, {
            onPrepareReady?.invoke()
        }, { desc ->
            map.remove(key)?.release()
            onPrepareError?.invoke(desc)
        })
        player.setOnCompletionListener {
            onCompleteListener?.invoke(key)
        }
        player.setOnPlaybackStateChangedListener { time ->
            onPlaybackStateChangedListener?.invoke(time)
        }
        player.setOnErrorListener {
            onErrorListener?.invoke(key, it)
        }
        map[key] = player
    }

    fun destroy(key: String) {
        map.remove(key)?.release()
    }

    fun play(key: String) {
        map[key]?.play()
    }

    fun pause(key: String) {
        map[key]?.pause()
    }

    fun isPlaying(key: String): Boolean? {
        return map[key]?.isPlaying
    }

    fun setOnCompletionListener(listener: ((key: String) -> Unit)? = null) {
        onCompleteListener = listener
    }

    fun setOnPlaybackStateChangedListener(
        listener: ((time: Float) -> Unit)? = null) {
        onPlaybackStateChangedListener = listener
    }

    fun setOnErrorListener(listener: ((key: String, desc: String) -> Unit)?) {
        onErrorListener = listener
    }

    fun contains(key: String): Boolean {
        return map.contains(key)
    }

    fun release() {
        onCompleteListener = null
        onErrorListener = null
        clear()
    }

    private fun clear() {
        map.forEach {
            it.value.release()
        }
        map.clear()
    }
}