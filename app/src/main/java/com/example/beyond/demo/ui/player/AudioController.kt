package com.example.beyond.demo.ui.player

/**
 *
 *
 * @author wangshichao
 * @date 2024/6/12
 */
internal class AudioController {
    private val map by lazy {
        mutableMapOf<String, PlayerWrapper>()
    }

    private var onCompleteListener: ((key: String) -> Unit)? = null
    private var onPlaybackStateChangedListener: ((key: String, time: Float) -> Unit)? = null
    private var onErrorListener: ((key: String, desc: String) -> Unit)? = null

    fun create(
        url: String,
        key: String,
        onPrepareReady: ((key: String, player: PlayerWrapper) -> Unit)? = null,
        onPrepareError: ((desc: String?) -> Unit)? = null
    ) {
        val player = PlayerWrapper(url, { player ->
            onPrepareReady?.invoke(key, player)
        }, { desc ->
            map.remove(key)?.release()
            onPrepareError?.invoke(desc)
        })
        player.setOnCompletionListener {
            onCompleteListener?.invoke(key)
        }
        player.setOnPlaybackStateChangedListener { time ->
            onPlaybackStateChangedListener?.invoke(key, time)
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

    fun currentTime(key: String) : Float? {
        return map[key]?.currentTime
    }

    fun seekTo(key: String, time: Float) {
        map[key]?.currentTime = time
    }

    fun setScreenOnWhilePlaying(key: String, value: Boolean) {
        map[key]?.setScreenOnWhilePlaying(value)
    }

    fun setOnCompleteListener(listener: ((key: String) -> Unit)? = null) {
        onCompleteListener = listener
    }

    fun setOnPlaybackStateChangedListener(
            listener: ((key: String, time: Float) -> Unit)? = null) {
        onPlaybackStateChangedListener = listener
    }

    fun setOnErrorListener(listener: ((key: String, desc: String) -> Unit)?) {
        onErrorListener = listener
    }

    fun contains(key: String): Boolean {
        return map.contains(key)
    }

    fun forEach(fn: (key: String) -> Unit) {
        map.keys.forEach {
            fn(it)
        }
    }

    fun release() {
        onCompleteListener = null
        onPlaybackStateChangedListener = null
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