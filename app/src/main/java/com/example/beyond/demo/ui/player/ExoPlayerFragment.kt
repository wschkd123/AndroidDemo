package com.example.beyond.demo.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.base.BaseFragment
import com.example.base.player.AudioFocusManager
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.R
import com.example.beyond.demo.databinding.FragmentExoPlayerBinding
import com.example.beyond.demo.ui.player.data.MediaDataSource

/**
 * ExoPlayer播放
 *
 * @author wangshichao
 * @date 2024/6/15
 */
class ExoPlayerFragment : BaseFragment() {
    private var _binding: FragmentExoPlayerBinding? = null
    private val binding get() = _binding!!
    private val mp3Url =
        "https://www.cambridgeenglish.org/images/153149-movers-sample-listening-test-vol2.mp3"
    private val mp3Path by lazy { YWFileUtil.getStorageFileDir(context).path + "/test.mp3" }
    private var currentTtsKey: String? = ""
    private val handler = Handler(Looper.getMainLooper()) { // 首帧清除数据
        when (it.what) {
            WHAT_TTS_SUCCESS -> {
                val dataSource = it.obj as MediaDataSource
                Log.i(TAG, "handleMessage clickTtsKey:${currentTtsKey} ttsKey:${dataSource.ttsKey}")
                // 仅播放最后一个被点击的内容
                if (currentTtsKey == dataSource.ttsKey) {
                    ExoPlayerManager.addMediaItem(dataSource)
                }
            }

            WHAT_TTS_LIMIT -> {
                Toast.makeText(context, "您点的太快啦", Toast.LENGTH_SHORT).show()
            }

            WHAT_TTS_CACHE -> {
                val pair = it.obj as Pair<String, String>
                val ttsKey = pair.first
                val cachePath = pair.second
                Log.i(TAG, "handleMessage clickTtsKey:${currentTtsKey} ttsKey:$ttsKey")
                if (currentTtsKey == ttsKey) {
                    ExoPlayerManager.addMediaItem(cachePath)
                }
            }

            WHAT_TTS_NET_ERROR -> {
                Toast.makeText(context, getString(R.string.net_error_toast), Toast.LENGTH_SHORT).show()
            }

            else -> {
            }
        }
        false
    }

    companion object {

        /**
         * 存在缓存
         */
        private const val WHAT_TTS_CACHE = 0x10

        /**
         * 音频片段成功
         */
        private const val WHAT_TTS_SUCCESS = 0x11

        /**
         * 音频片段达到限制
         */
        private const val WHAT_TTS_LIMIT = 0x12

        /**
         * 网络错误
         */
        private const val WHAT_TTS_NET_ERROR = 0x13

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentExoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvPlayStream1.setOnClickListener {
            ExoPlayerManager.clearMediaItems()
            val content = "Events"
            currentTtsKey = content.hashCode().toString()
            Log.w(TAG, "click clickTtsKey:${currentTtsKey} content:${content}")
            TTSStreamManager.startConnect(content, currentTtsKey!!)
        }

        binding.tvPlayStream2.setOnClickListener {
            ExoPlayerManager.clearMediaItems()
            val content = "Server-Send Events"
            currentTtsKey = content.hashCode().toString()
            Log.w(TAG, "click clickTtsKey:${currentTtsKey} content:${content}")
            TTSStreamManager.startConnect(content, currentTtsKey!!)
        }

        binding.tvPlayLocal.setOnClickListener {
            ExoPlayerManager.clearMediaItems()
            ExoPlayerManager.addMediaItem(mp3Path)
        }

        binding.tvPlayNet.setOnClickListener {
            ExoPlayerManager.clearMediaItems()
            ExoPlayerManager.addMediaItem(mp3Url)
        }
    }

    private val ttsStreamListener = object : TTSStreamListener {
        override fun onExistCache(ttsKey: String, cachePath: String) {
            Log.i(TAG, "onExistCache clickTtsKey:${currentTtsKey} ttsKey:${ttsKey}")
            val message = handler.obtainMessage(WHAT_TTS_CACHE, Pair(ttsKey, cachePath))
            handler.sendMessage(message)
        }

        override fun onReceiveChunk(dataSource: MediaDataSource) {
            Log.i(
                TAG,
                "onReceiveChunk clickTtsKey:${currentTtsKey} ttsKey:${dataSource.ttsKey}"
            )
            val message = handler.obtainMessage(WHAT_TTS_SUCCESS, dataSource)
            handler.sendMessage(message)
        }

        override fun onReceiveLimit(code: Int, msg: String) {
            Log.w(TAG, "onReceiveLimit code:${code} msg:$msg")
            val message = handler.obtainMessage(WHAT_TTS_LIMIT)
            handler.sendMessage(message)
        }

        override fun onNetError(msg: String) {
            val message = handler.obtainMessage(WHAT_TTS_NET_ERROR)
            handler.sendMessage(message)
        }
    }

    override fun onStart() {
        super.onStart()
        TTSStreamManager.listener = ttsStreamListener
        ExoPlayerManager.onErrorListener = {
            Toast.makeText(context, "播放出错", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        AudioFocusManager.requestAudioFocus()
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop")
        TTSStreamManager.listener = null
        handler.removeCallbacksAndMessages(null)
        ExoPlayerManager.stop()
        AudioFocusManager.abandonAudioFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        ExoPlayerManager.release()
    }

}