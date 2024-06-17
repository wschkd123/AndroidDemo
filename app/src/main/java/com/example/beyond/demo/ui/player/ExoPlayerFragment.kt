package com.example.beyond.demo.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.base.BaseFragment
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.databinding.FragmentExoPlayerBinding
import com.example.beyond.demo.ui.player.ExoPlayerManager.replaceDataSource
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
    private var chatSpeechHelper: SpeechStreamHelper? = null
    private val handler = Handler(Looper.getMainLooper()) { // 首帧清除数据
        when (it.what) {
            WHAT_AUDIO_CHUNK -> {
                val dataSource = it.obj as MediaDataSource
                replaceDataSource(dataSource)
                true
            }

            else -> {
                false
            }
        }
    }

    companion object {
        /**
         * 音频片段消息
         */
        private const val WHAT_AUDIO_CHUNK = 0x10
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

        chatSpeechHelper = SpeechStreamHelper(view.context, object : SpeechStreamListener {
            override fun onReceiveChunk(dataSource: MediaDataSource) {
                replaceDataSource(dataSource)
            }

        })

        binding.tvPlayStream1.setOnClickListener {
            ExoPlayerManager.clearMediaItems()
            chatSpeechHelper?.loadData("Events")
        }

        binding.tvPlayStream2.setOnClickListener {
            ExoPlayerManager.clearMediaItems()
            chatSpeechHelper?.loadData("筑梦岛")
        }

        binding.tvPlayLocal.setOnClickListener {
            ExoPlayerManager.replaceDataSource(mp3Path)
        }

        binding.tvPlayNet.setOnClickListener {
            ExoPlayerManager.replaceDataSource(mp3Url)
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