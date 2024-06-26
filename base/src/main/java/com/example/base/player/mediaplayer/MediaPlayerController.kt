package com.example.base.player.mediaplayer

/**
 * 单实例播放器控制器
 *
 * @author wangshichao
 * @date 2024/6/13
 */
class MediaPlayerController {
    private val player = PlayerWrapper()

    private var onCompleteListener: (() -> Unit)? = null
    private var onPlaybackStateChangedListener: ((time: Float) -> Unit)? = null
    private var onErrorListener: ((desc: String) -> Unit)? = null

    fun prepare(
        url: String,
        onPrepareReady: (() -> Unit)? = null,
        onPrepareError: ((desc: String?) -> Unit)? = null
    ) {
        player.apply {
            reset()
            prepare(url, {
                player.play()
                onPrepareReady?.invoke()
            }, { desc ->
                player.release()
                onPrepareError?.invoke(desc)
            })
            setOnCompletionListener {
                onCompleteListener?.invoke()
            }
            player.setOnPlaybackStateChangedListener { time ->
                onPlaybackStateChangedListener?.invoke(time)
            }
            setOnErrorListener {
                onErrorListener?.invoke(it)
            }
        }

    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }

    fun setOnCompletionListener(listener: (() -> Unit)? = null) {
        onCompleteListener = listener
    }

    fun setOnPlaybackStateChangedListener(
        listener: ((time: Float) -> Unit)? = null) {
        onPlaybackStateChangedListener = listener
    }

    fun setOnErrorListener(listener: ((desc: String) -> Unit)?) {
        onErrorListener = listener
    }

    fun release() {
        onCompleteListener = null
        onErrorListener = null
        player.release()
    }

}