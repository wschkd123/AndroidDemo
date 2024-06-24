package com.example.base.player

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.base.AppContext

/**
 * exoPlayer 播放器管理
 *
 * @author wangshichao
 * @date 2024/6/17
 */
object ExoPlayerManager {
    private const val TAG = "ExoPlayerManager"
    private val player: Player
    private val playbackStateListener: Player.Listener = playbackStateListener()
    var onErrorListener: ((uri: String, playKey: String, desc: String) -> Unit)? = null
    var onPlaybackStateChangedListener: ((uri: String, playKey: String, playState: Int) -> Unit)? = null

    /**
     * 播放资源key。除了分片播放音频，其它场景key与uri保持一致
     */
    private var playerKey: String? = null

    init {
        player = ExoPlayer.Builder(AppContext.application)
            .build()
            .also { exoPlayer ->
                exoPlayer.playWhenReady = true
                exoPlayer.addListener(playbackStateListener)
            }
    }

    /**
     * 将媒体项添加到播放列表的末尾。其中播放列表只有一条数据，uri与playerKey保持一致
     *
     * @param uri 资源地址
     */
    fun addMediaItem(uri: String) {
        addMediaItem(uri, uri)
    }

    /**
     * 将媒体项添加到播放列表的末尾
     *
     * @param uri 资源地址
     * @param key 资源key
     */
    fun addMediaItem(uri: String, key: String) {
        Log.w(TAG, "addMediaItem uri:${uri} key:${key}")
        playerKey = key
        player.apply {
            addMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    fun clearMediaItems() {
        player.clearMediaItems()
        playerKey = null
    }

    /**
     * 指定资源是否播放中
     *
     * @param key 资源key
     */
    fun isPlaying(key: String): Boolean {
        Log.i(TAG, "key:$key playerKey:$playerKey")
        if (playerKey != key) {
            return false
        }
        val mediaItemCount = player.mediaItemCount
        for (i in 0 until mediaItemCount) {
            if (player.isPlaying && player.currentMediaItemIndex == i) {
                Log.i(TAG, "player item[$i] isPlaying")
                return true
            }
        }
        return false
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
    }

    fun release() {
        player.let { player ->
            player.removeListener(playbackStateListener)
            player.release()
        }
    }

    private fun currentPlayUri() = player.getMediaItemAt(player.currentMediaItemIndex).localConfiguration?.uri.toString()

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val playState = when (playbackState) {
                ExoPlayer.STATE_BUFFERING -> PlayState.LOADING
                ExoPlayer.STATE_READY -> PlayState.PLAYING
                ExoPlayer.STATE_IDLE, ExoPlayer.STATE_ENDED -> PlayState.IDLE
                else -> PlayState.IDLE
            }
            val uri = if (playbackState < ExoPlayer.STATE_ENDED) currentPlayUri() else ""
            Log.i(TAG, "changed state to $playbackState uri:${uri} key:${playerKey}")
            onPlaybackStateChangedListener?.invoke(uri, playerKey ?: "", playState)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            Log.i(TAG, "onMediaItemTransition reason:$reason")
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            val uri = currentPlayUri()
            Log.e(TAG, "Playback code:${error.errorCode} msg:${error.message} uri:${uri} key:${playerKey}")
            onErrorListener?.invoke(uri, playerKey ?: "", error.message ?: "")
        }
    }

}