package com.example.base.player

import android.app.Activity
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.base.AppContext

/**
 * 音频焦点管理
 *
 * 1.进入前台获取音频焦点
 * 2.进入后台停止播放并放弃音频焦点
 *
 * @author wangshichao
 * @date 2024/6/12
 */
object AudioFocusManager {
    private const val TAG = "AudioFocusManager"
    private var audioManager: AudioManager =
        AppContext.application.getSystemService(Activity.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var listener: AudioManager.OnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            Log.i(TAG, "onAudioFocusChange focusChange:$focusChange")
            when (focusChange) {
                // 获取焦点
                AudioManager.AUDIOFOCUS_GAIN -> {

                }

                // 失去焦点
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {

                }
            }
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attribute = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setWillPauseWhenDucked(true)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(listener, handler)
                .setAudioAttributes(attribute)
                .build()
        }
    }

    fun requestAudioFocus() {
        val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            audioManager.requestAudioFocus(
                listener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        Log.i(TAG, "requestAudioFocus $res")
    }

    fun abandonAudioFocus() {
        val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            audioManager.abandonAudioFocus(listener)
        }
        Log.i(TAG, "abandonAudioFocus $res")
    }

}