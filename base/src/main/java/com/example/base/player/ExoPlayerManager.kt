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
    var onErrorListener: ((uri: String, desc: String) -> Unit)? = null

    init {
        player = ExoPlayer.Builder(AppContext.application)
            .build()
            .also { exoPlayer ->
                exoPlayer.playWhenReady = true
                exoPlayer.addListener(playbackStateListener)
            }
    }

    fun addMediaItem(uri: String) {
        Log.w(TAG, "addMediaItem uri:${uri}")
        player.apply {
            addMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
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

    private fun currentPlayUri() = player.getMediaItemAt(player.currentMediaItemIndex).localConfiguration?.uri.toString()

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

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            Log.i(TAG, "onMediaItemTransition reason:$reason")
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            val uri = currentPlayUri()
            Log.e(TAG, "Playback code:${error.errorCode} msg:${error.message} uri:${uri}")
            onErrorListener?.invoke(uri, error.message ?: "")
        }
    }

}