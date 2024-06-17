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
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.databinding.FragmentExoPlayerBinding
import com.example.beyond.demo.ui.player.ExoPlayerManager
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
    private lateinit var ttsStreamHelper: TTSStreamHelper
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
                Log.i(TAG, "handleMessage clickTtsKey:${currentTtsKey} ttsKey:${pair.first}")
                if (currentTtsKey == pair.first) {
                    ExoPlayerManager.addMediaItem(pair.second)
                }
            }

            else -> {
            }
        }
        false
    }

    companion object {
        /**
         * 音频片段成功
         */
        private const val WHAT_TTS_SUCCESS = 0x10
        /**
         * 音频片段达到限制
         */
        private const val WHAT_TTS_LIMIT=0x11

        /**
         * 存在缓存
         */
        private const val WHAT_TTS_CACHE=0x12

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
        ttsStreamHelper = TTSStreamHelper(object : TTSStreamListener {
            override fun onReceiveChunk(dataSource: MediaDataSource) {
                Log.i(TAG, "onReceiveChunk clickTtsKey:${currentTtsKey} ttsKey:${dataSource.ttsKey}")
                val message = handler.obtainMessage(WHAT_TTS_SUCCESS, dataSource)
                handler.sendMessage(message)
            }

            override fun onReceiveLimit(code: Int, msg: String) {
                Log.w(TAG, "onReceiveLimit code:${code} msg:$msg")
                val message = handler.obtainMessage(WHAT_TTS_LIMIT)
                handler.sendMessage(message)
            }

            override fun onExistCache(ttsKey: String, path: String) {
                val message = handler.obtainMessage(WHAT_TTS_CACHE, Pair(ttsKey, path))
                handler.sendMessage(message)
            }

        })

        binding.tvPlayStream1.setOnClickListener {
            ExoPlayerManager.clearMediaItems()
            val content = "Events"
            currentTtsKey = content.hashCode().toString()
            Log.w(TAG, "click clickTtsKey:${currentTtsKey} content:${content}")
            ttsStreamHelper.startConnect(content, currentTtsKey!!)
        }

        binding.tvPlayStream2.setOnClickListener {
            ExoPlayerManager.clearMediaItems()
            val content = "筑梦岛"
            currentTtsKey = content.hashCode().toString()
            Log.w(TAG, "click clickTtsKey:${currentTtsKey} content:${content}")
            ttsStreamHelper.startConnect(content, currentTtsKey!!)
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

    override fun onStop() {
        super.onStop()
        ExoPlayerManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        ExoPlayerManager.release()
    }

}