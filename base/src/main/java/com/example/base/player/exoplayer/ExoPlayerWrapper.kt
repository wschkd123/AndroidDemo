package com.example.base.player.exoplayer

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.base.Init
import com.example.base.player.OnPlayerListener
import com.example.base.player.PlayState
import java.lang.ref.WeakReference


/**
 * exoPlayer 播放器封装
 *
 * @author wangshichao
 * @date 2024/6/17
 */
class ExoPlayerWrapper {
    private val TAG = "ExoPlayerWrapper"
    val player: ExoPlayer
    private val playbackStateListener: Player.Listener = playbackStateListener()
    private val playerListenerList: MutableList<WeakReference<OnPlayerListener>> = mutableListOf()

    /**
     * 播放资源key。除了分片播放音频，其它场景key与uri保持一致
     */
    private var playerKey: String? = null
    private var dataSourceFactory: FileChannelDataSource.Factory? = null


    init {
        player = ExoPlayer.Builder(Init.application)
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
        Log.w(TAG, "addMediaItem uri=${uri} key=${key}")
        playerKey = key
        dataSourceFactory = null
        player.apply {
            addMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    /**
     * 增加音频片段
     */
    fun addChunk(data: ByteArray, key: String, path: String) {
        Log.w(TAG, "addChunk: data=${data.size} key=${key} path=$path")
        // 正在播放的数据源追加数据
        if (dataSourceFactory != null) {
            if (data.isEmpty()) {
                // 没有更多数据
                dataSourceFactory!!.dataSource.noMoreData()
            } else {
                // 追加数据
                dataSourceFactory!!.dataSource.appendDataAsync(data)
            }
            return
        }
        // 同一个key只有一个MediaItem
        if (key == playerKey) {
            return
        }
        val factory = FileChannelDataSource.Factory(path, data, object : TransferListener {
            override fun onTransferInitializing(
                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean
            ) {
                Log.i(TAG, "onTransferInitializing dataSpec=$dataSpec")
            }

            override fun onTransferStart(
                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean
            ) {
                Log.i(TAG, "onTransferStart dataSpec=$dataSpec")
            }

            override fun onBytesTransferred(
                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean,
                bytesTransferred: Int
            ) {
                Log.i(TAG, "onBytesTransferred dataSpec=$dataSpec")
            }

            override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
                Log.i(TAG, "onTransferEnd dataSpec=$dataSpec")
            }

        })
        dataSourceFactory = factory
        val audioByteUri = ByteArrayUriUtil.getUri()
        val mediaItem = MediaItem.fromUri(audioByteUri)
        val audioSource = ProgressiveMediaSource.Factory(factory)
            .createMediaSource(mediaItem)
        playerKey = key
        player.apply {
            setMediaSource(audioSource)
            prepare()
            playWhenReady = true
        }
    }

    fun clearMediaItems() {
        player.clearMediaItems()
        playerKey = null
        dataSourceFactory = null
    }

    fun addPlayerListener(listener: OnPlayerListener) {
        if (!hasListener(listener)) {
            playerListenerList.add(WeakReference<OnPlayerListener>(listener))
        }
    }

    fun removePlayerListener(listener: OnPlayerListener) {
        var weakRefToRemove: WeakReference<OnPlayerListener>? = null
        for (weakRef in playerListenerList) {
            val onPlayerListener: OnPlayerListener? = weakRef.get()
            if (onPlayerListener == null || onPlayerListener === listener) {
                weakRefToRemove = weakRef
                break
            }
        }
        if (weakRefToRemove != null) {
            playerListenerList.remove(weakRefToRemove)
        }
    }

    /**
     * 是否播放中
     */
    fun isPlaying(): Boolean {
        return isPlaying(playerKey)
    }

    /**
     * 指定资源是否播放中
     *
     * @param key 资源key
     */
    fun isPlaying(key: String?): Boolean {
        if (playerKey != key) {
            return false
        }
        val mediaItemCount = player.mediaItemCount
        for (i in 0 until mediaItemCount) {
            if (player.isPlaying && player.currentMediaItemIndex == i) {
                Log.i(TAG, "key=$key player item[$i] isPlaying")
                return true
            }
        }
        return false
    }

    fun isLoading(key: String?):Boolean{
        if (isPlaying(key)){
            return true
        }
        if (playerKey != key) {
            return false
        }
        val mediaItemCount = player.mediaItemCount
        for (i in 0 until mediaItemCount) {
            if (player.isLoading && player.currentMediaItemIndex == i) {
                Log.i(TAG, "key=$key player item[$i] isPlaying")
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

    private fun currentPlayUri(): String? {
        val index = player.currentMediaItemIndex
        return if (index >= 0 && index < player.mediaItemCount) {
            player.getMediaItemAt(index).localConfiguration?.uri?.toString()
        } else {
            null
        }
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val playState = when (playbackState) {
                ExoPlayer.STATE_BUFFERING -> PlayState.LOADING
                ExoPlayer.STATE_READY -> PlayState.PLAYING
                ExoPlayer.STATE_IDLE, ExoPlayer.STATE_ENDED -> PlayState.IDLE
                else -> PlayState.IDLE
            }
            Log.i(TAG, "changed state to originPlayState=$playbackState realPlayState=${playState} key=${playerKey}")
            playerListenerList.forEach {
                it.get()?.onPlaybackStateChanged(playerKey ?: "", playState)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            val uri = currentPlayUri() ?: ""
            Log.e(TAG, "Playback code=${error.errorCode} msg=${error.message} uri=${uri} key=${playerKey}")
            playerListenerList.forEach {
                it.get()?.onPlayerError(uri, playerKey ?: "", error.message ?: "")
            }
        }
    }

    private fun hasListener(listener: OnPlayerListener): Boolean {
        for (weakRef in playerListenerList) {
            val onPlayerListener: OnPlayerListener? = weakRef.get()
            if (listener === onPlayerListener) {
                return true
            }
        }
        return false
    }
}