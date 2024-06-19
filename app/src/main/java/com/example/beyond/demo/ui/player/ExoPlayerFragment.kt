package com.example.beyond.demo.ui.player

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.base.BaseFragment
import com.example.base.download.FileDownloadListener
import com.example.base.download.FileDownloadManager
import com.example.base.download.TTSFileUtil
import com.example.base.player.AudioFocusManager
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.R
import com.example.beyond.demo.databinding.FragmentExoPlayerBinding
import com.example.beyond.demo.ui.player.data.MediaDataSource
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
//            AudioDownloadManager.download("mp3Url", ttsKey, object : AudioProgressListener {
//                override fun onSuccess(url: String, fileName: String, path: String) {
//                    Log.i(TAG, "download url:$url, clickTtsKey:${currentTtsKey} ttsKey:${ttsKey}")
//                    if (currentTtsKey == fileName) {
//                        ExoPlayerManager.addMediaItem(path)
//                    }
//                }
//
//                override fun onError(url: String, msg: String?) {
//                    Log.i(TAG, "download url:${url} msg:${msg}")
//                }
//
//            })
            val cachePath = TTSFileUtil.getCacheFile(ttsKey)?.path
            if (cachePath != null) {
                Log.i(TAG, "exist cache cachePath:${cachePath}")
                ExoPlayerManager.addMediaItem(cachePath)
                return@setOnClickListener
            }

            FileDownloadManager.download(mp3Url, currentTtsKey!!, object : FileDownloadListener {

                override fun onProgress(
                    url: String,
                    bytesRead: Long,
                    contentLength: Long,
                    done: Boolean
                ) {
                    Log.i(
                        TAG,
                        "onProgress $url progress:${100 * bytesRead / contentLength} done:${done}"
                    )
                }

                override fun onSuccess(url: String, fileName: String, file: File) {
                    Log.i(TAG, "onSuccess $url file:${file}")
                    if (currentTtsKey == fileName) {
                        ExoPlayerManager.addMediaItem(file.path)
                    }
                }

                override fun onFail(url: String, errorMessage: String) {
                    Log.i(TAG, "onFail url:$url errorMessage:$errorMessage")
                }
            })
        }
    }

    private fun clickStartTTS(content: String) {
        ExoPlayerManager.clearMediaItems()
        val ttsKey = content.hashCode().toString()
        currentTtsKey = ttsKey
        Log.w(TAG, "click clickTtsKey:${ttsKey} content:${content}")
        val cacheFile = TTSFileUtil.getCacheFile(ttsKey)
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