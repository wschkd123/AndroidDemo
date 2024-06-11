 package com.example.beyond.demo.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.beyond.demo.base.AppContext
import kotlin.math.max
import kotlin.math.min

internal object MediaPlugin {

    class Player(
        url: String,
        onReady: ((player: Player) -> Unit)? = null,
        onError: ((desc: String?) -> Unit)? = null
    ) {
        companion object {
            private const val TAG = "[Player-NA]"
        }
        private val mediaPlayer = MediaPlayer()
        private var playing: Boolean = false
        private var prepared: Boolean = false

        private var playbackStateChangedListener: ((time: Float) -> Unit)? = null
        private var playToEndListener:(() -> Unit)? = null
        private var errorListener:((desc: String) -> Unit)? = null

        private var lastTime: Float? = null

        init {
            try {
                mediaPlayer.setOnCompletionListener {
                    Log.d(TAG, "OnComplete")
                    playing = false
                    playToEndListener?.invoke()
                    syncPlaybackStatusIfNeeds()
                }
                mediaPlayer.setOnInfoListener { _, what, _ ->
                    return@setOnInfoListener when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START,
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                            Log.d(TAG, "OnInfo BUFFERING $what")
                            syncPlaybackStatusIfNeeds()
                            true
                        }
                        MediaPlayer.MEDIA_INFO_AUDIO_NOT_PLAYING,
                        MediaPlayer.MEDIA_INFO_VIDEO_NOT_PLAYING -> {
                            Log.d(TAG, "OnInfo NOT_PLAYING $what")
                            syncPlaybackStatusIfNeeds()
                            true
                        } else -> {
                            Log.d(TAG, "OnInfo others $what")
                            false
                        }
                    }
                }
                mediaPlayer.setOnSeekCompleteListener {
                    Log.d(TAG, "OnSeekComplete")
                    syncPlaybackStatusIfNeeds()
                }
                mediaPlayer.setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "OnError prepared:$prepared what:$what extra:$extra")
                    if (prepared) {
                        prepared = false
                        errorListener?.invoke("what:$what extra:$extra")
                    } else {
                        onError?.invoke("what:$what extra:$extra")
                    }
                    return@setOnErrorListener false
                }
                mediaPlayer.setOnPreparedListener {
                    Log.d(TAG, "OnPrepared")
                    prepared = true
                    onReady?.invoke(this)
                }
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                mediaPlayer.setDataSource(url)
                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                Log.w(TAG, "create player failed")
                onError?.invoke(e.message)
            }
        }

        val isPlaying: Boolean
            get() = playing

        var currentTime: Float
            get() {
                return mediaPlayer.currentPosition / 1000.0f
            }
            set(value) {
                val v = min(max((value * 1000).toInt(), 0), mediaPlayer.duration)
                Log.d(TAG, "seekToTime:$v")
                mediaPlayer.seekTo(v)
            }

        fun play() {
            Log.d(TAG, "play $prepared")
            if (prepared && !playing) {
                playing = true
                mediaPlayer.start()
                syncPlaybackStatusIfNeeds()
            }
        }

        fun pause() {
            Log.i(TAG, "pause $prepared")
            if (prepared && playing) {
                playing = false
                mediaPlayer.pause()
                syncPlaybackStatusIfNeeds()
            }
        }

        fun setScreenOnWhilePlaying(screenOn: Boolean) {
            AppContext.application.applicationContext?.let {
                Log.i(TAG, "setScreenOnWhilePlaying $screenOn")
                if (screenOn) {
                    mediaPlayer.setWakeMode(it, android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK)
                } else {
                    mediaPlayer.setWakeMode(it, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                }
            } ?: Log.i(TAG, "setScreenOnWhilePlaying $screenOn, get context failed")
        }

        fun setOnCompletionListener(listener: (() -> Unit)?) {
            playToEndListener = listener
        }

        fun setOnPlaybackStateChangedListener(listener: ((time: Float) -> Unit)?) {
            playbackStateChangedListener = listener
        }

        fun setOnErrorListener(listener: ((desc: String) -> Unit)?) {
            errorListener = listener
        }

        fun release() {
            playToEndListener = null
            mediaPlayer.setOnCompletionListener(null)
            mediaPlayer.setOnInfoListener(null)
            errorListener = null
            mediaPlayer.setOnErrorListener(null)
            mediaPlayer.setOnPreparedListener(null)
            playbackStateChangedListener = null

            mediaPlayer.stop()
            mediaPlayer.release()
        }

        private fun syncPlaybackStatusIfNeeds() {
            val time = currentTime
            if (time != lastTime) {
                lastTime = time
                Log.d(TAG, "sync playback statue time:$time")
                playbackStateChangedListener?.invoke(time)
            }
        }
    }
    class Client {
        private val map by lazy {
            mutableMapOf<String, Player>()
        }

        private var onCompleteListener: ((key: String) -> Unit)? = null
        private var onPlaybackStateChangedListener: ((key: String, time: Float) -> Unit)? = null
        private var onErrorListener: ((key: String, desc: String) -> Unit)? = null

        fun create(
            url: String,
            key: String,
            onComplete: ((key: String, player: Player) -> Unit)? = null,
            onError: ((desc: String?) -> Unit)? = null
        ) {
//            val key = generator.next
            val player = Player(url, { player ->
                onComplete?.invoke(key, player)
            }, { desc ->
                map.remove(key)?.release()
                onError?.invoke(desc)
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

        fun clear() {
            map.forEach {
                it.value.release()
            }
            map.clear()
        }
    }
}