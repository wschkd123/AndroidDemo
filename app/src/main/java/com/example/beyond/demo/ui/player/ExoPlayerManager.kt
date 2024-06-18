package com.example.beyond.demo.ui.player

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.base.AppContext
import com.example.beyond.demo.ui.player.data.MediaDataSource

/**
 * exoPlayer 播放器管理
 *
 * @author wangshichao
 * @date 2024/6/17
 */
object ExoPlayerManager {
    private const val TAG = "ExoPlayerManager"
    private val player: Player
    private var playWhenReady = true
    private val playbackStateListener: Player.Listener = playbackStateListener()
    var onErrorListener: ((desc: String) -> Unit)? = null

    init {
        player = ExoPlayer.Builder(AppContext.application)
            .build()
            .also { exoPlayer ->
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.addListener(playbackStateListener)
            }
    }

    fun addMediaItem(dataSource: MediaDataSource) {
        if (dataSource.audioChunk.isLastComplete.not()) {
            Log.w(TAG, "addMediaItem path:${dataSource.audioChunk.chunkPath} ttsKey:${dataSource.ttsKey}")
            player.apply {
                addMediaItem(MediaItem.fromUri(dataSource.audioChunk.chunkPath))
                prepare()
                playWhenReady = playWhenReady
            }
        }
    }

    fun addMediaItem(uri: String) {
        Log.w(TAG, "addMediaItem uri:${uri}")
        player.apply {
            clearMediaItems()
            addMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = playWhenReady
        }
    }

    fun clearMediaItems() {
        player.clearMediaItems()
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
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

    private fun currentPlayUri() = player.getMediaItemAt(player.currentMediaItemIndex).localConfiguration?.uri

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.i(TAG, "changed state to $stateString")
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            Log.i(TAG, "onPlayWhenReadyChanged playWhenReady:$playWhenReady reason:$reason")
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            Log.i(TAG, "onMediaItemTransition reason:$reason")
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.e(TAG, "Playback code:${error.errorCode} msg:${error.message} uri:${currentPlayUri()}")
            onErrorListener?.invoke("${error.message}")
        }
    }

}