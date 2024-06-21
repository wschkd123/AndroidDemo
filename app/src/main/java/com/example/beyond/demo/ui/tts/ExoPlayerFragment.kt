package com.example.beyond.demo.ui.tts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.base.BaseFragment
import com.example.base.download.FileDownloadManager
import com.example.base.player.AudioFocusManager
import com.example.base.player.ExoPlayerManager
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.R
import com.example.beyond.demo.databinding.FragmentExoPlayerBinding
import com.example.beyond.demo.ui.tts.data.ChunkDataSource
import java.io.File

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
    private val mp3Path by lazy { YWFileUtil.getStorageFileDir(context)?.path + "/test.mp3" }
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
            clickStartTTS("Events")
        }

        binding.tvPlayStream2.setOnClickListener {
            clickStartTTS("Hello")
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
            val ttsKey = "mp3Url"
            currentTtsKey = ttsKey
            ExoPlayerManager.clearMediaItems()

            // 有缓存直接播放
            val cachePath = TTSFileUtil.checkCacheFileFromKey(ttsKey)?.path
            if (cachePath != null) {
                Log.i(TAG, "exist cache cachePath:${cachePath}")
                ExoPlayerManager.addMediaItem(cachePath)
                return@setOnClickListener
            }
            // 在线播放
            ExoPlayerManager.addMediaItem(mp3Url)

            // 离线下载
            val file = TTSFileUtil.createCacheFileFromUrl(ttsKey, mp3Url)
            FileDownloadManager.download(mp3Url, file.path)
        }
    }

    private fun clickStartTTS(content: String) {
        ExoPlayerManager.clearMediaItems()
        val ttsKey = content.hashCode().toString()
        currentTtsKey = ttsKey
        Log.w(TAG, "click clickTtsKey:${ttsKey} content:${content}")
        val cacheFile = TTSFileUtil.checkCacheFileFromKey(ttsKey)
        if (cacheFile != null) {
            Log.w(TAG, "exist cache ${cacheFile.path}")
            ExoPlayerManager.addMediaItem(cacheFile.path)
            return
        }
        TTSStreamManager.startConnect(content, ttsKey)
    }

    private val ttsStreamListener = object : TTSStreamListener {

        override fun onReceiveCompleteUrl(ttsKey: String, url: String) {
            Log.i(TAG, "onReceiveCompleteUrl clickTtsKey:${currentTtsKey} ttsKey:${ttsKey}")
            if (currentTtsKey == ttsKey) {
                ExoPlayerManager.addMediaItem(url)
            }
        }

        override fun onReceiveChunk(dataSource: ChunkDataSource) {
            Log.i(
                TAG,
                "onReceiveChunk clickTtsKey:${currentTtsKey} ttsKey:${dataSource.ttsKey}"
            )
            // 仅播放最后一个被点击的内容
            if (currentTtsKey == dataSource.ttsKey) {
                ExoPlayerManager.addMediaItem(dataSource.chunkPath)
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
        ExoPlayerManager.onErrorListener = { uri: String, desc: String ->
            // 离线播放失败，删除缓存。方便下次通过在线播放
            if (YWFileUtil.isLocalPath(uri)) {
                File(uri).delete()
            }
            Toast.makeText(context, getString(R.string.net_error_toast), Toast.LENGTH_SHORT).show()
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