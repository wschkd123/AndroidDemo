package com.example.beyond.demo.ui.player

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.base.BaseFragment
import com.example.base.download.AudioDownloadManager
import com.example.base.download.AudioProgressListener
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
    private val mp3Url1 = "http://music.163.com/song/media/outer/url?id=447925558.mp3"
    private val mp3Path by lazy { YWFileUtil.getStorageFileDir(context).path + "/test.mp3" }
    private var currentTtsKey: String? = ""

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

        binding.tvDownUrl.setOnClickListener {
            ExoPlayerManager.clearMediaItems()
            AudioDownloadManager.download("mp3Url", mp3Url1, object : AudioProgressListener{
                override fun onSuccess(ttsKey: String, path: String) {
                    if (currentTtsKey == ttsKey) {
                        ExoPlayerManager.addMediaItem(path)
                    }
                }

                override fun onError(code: Int, msg: String?) {
                    Log.i(TAG, "download $mp3Url1, Error code:${code} msg:${msg}")
                }

            })
        }
    }

    private val ttsStreamListener = object : TTSStreamListener {
        override fun onExistCache(ttsKey: String, cachePath: String) {
            Log.i(TAG, "onExistCache clickTtsKey:${currentTtsKey} ttsKey:${ttsKey}")
            if (currentTtsKey == ttsKey) {
                ExoPlayerManager.addMediaItem(cachePath)
            }
        }

        override fun onReceiveCompleteUrl(ttsKey: String, url: String) {
            Log.i(TAG, "onReceiveCompleteUrl clickTtsKey:${currentTtsKey} ttsKey:${ttsKey}")
            if (currentTtsKey == ttsKey) {
                ExoPlayerManager.addMediaItem(url)
            }
        }

        override fun onReceiveChunk(dataSource: MediaDataSource) {
            Log.i(
                TAG,
                "onReceiveChunk clickTtsKey:${currentTtsKey} ttsKey:${dataSource.ttsKey}"
            )
            // 仅播放最后一个被点击的内容
            if (currentTtsKey == dataSource.ttsKey) {
                ExoPlayerManager.addMediaItem(dataSource)
            }
        }

        override fun onRateLimit(code: Int, msg: String) {
            Log.w(TAG, "onReceiveLimit code:${code} msg:$msg")
            Toast.makeText(context, "您点的太快啦", Toast.LENGTH_SHORT).show()
        }

        override fun onNetError(msg: String) {
            Toast.makeText(context, getString(R.string.net_error_toast), Toast.LENGTH_SHORT).show()
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
        ExoPlayerManager.stop()
        AudioFocusManager.abandonAudioFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        ExoPlayerManager.release()
    }

}