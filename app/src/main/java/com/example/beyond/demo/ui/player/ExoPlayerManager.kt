package com.example.beyond.demo.ui.player

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.TimelineChangeReason
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
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
    private var onErrorListener: ((desc: String) -> Unit)? = null
    private var curPlayUri: String? = ""

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
            Log.w(TAG, "prepare play path:${dataSource.audioChunk.chunkPath}")
            player.apply {
                curPlayUri = dataSource.audioChunk.chunkPath
                addMediaItem(MediaItem.fromUri(dataSource.audioChunk.chunkPath))
                prepare()
                playWhenReady = playWhenReady
            }
        }
    }

    fun addMediaItem(uri: String) {
        curPlayUri = uri
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

        override fun onTimelineChanged(timeline: Timeline, reason: @TimelineChangeReason Int) {
            super.onTimelineChanged(timeline, reason)
            Log.i(TAG, "onTimelineChanged reason:$reason")
        }

        override fun onTracksChanged(tracks: Tracks) {
            super.onTracksChanged(tracks)
            Log.i(TAG, "onTracksChanged tracks:$tracks")
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            Log.i(TAG, "onPlayWhenReadyChanged playWhenReady:$playWhenReady reason:$reason")
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            Log.i(TAG, "onMediaItemTransition reason:$reason")
        }

        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
            super.onAvailableCommandsChanged(availableCommands)
            Log.i(TAG, "onAvailableCommandsChanged availableCommands:$availableCommands")
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback code:${error.errorCode} msg:${error.message}")
            super.onPlayerError(error)
            Log.e(TAG, "Playback error", error)
            onErrorListener?.invoke("code:${error.errorCode} msg:${error.message}")
        }
    }

}