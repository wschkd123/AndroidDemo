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
import com.example.base.player.OnPlayerListener
import com.example.base.player.PlayState
import com.example.base.player.audiotrack.AudioTrackManager
import com.example.base.player.exoplayer.ExoPlayerWrapper
import com.example.base.util.ThreadUtil
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.R
import com.example.beyond.demo.databinding.FragmentExoPlayerBinding
import com.example.beyond.demo.ui.tts.data.ChunkDataSource
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

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
    private val longStr = "毒鸡汤大魔王，会收集负面情绪，贱贱毒舌却又心地善良的好哥哥，也是持之以恒、霸气侧漏的灵气复苏时代的最强王者、星图战神。吕树，别名为第九天罗，依靠毒鸡汤成为大魔王。身世成谜，自小在福利院中长大，16岁后脱离福利院，与吕小鱼相依为命，通过卖煮鸡蛋维持生计。擅长怼人、噎人、气人，却从不骂人。平时说话贱贱的，被京都天罗地网同仁称为“贱圣”，但从不骂人，喜欢用讲道理却不似道理的话怼人。"
    private val shortStr = "毒鸡汤大魔王，会收集负面情绪"
    private val player = ExoPlayerWrapper()
    private var currentTtsKey: String? = ""
    private val executorService = Executors.newSingleThreadExecutor(object : ThreadFactory {
        private val mCount = AtomicInteger(1)
        override fun newThread(r: Runnable): Thread {
            return Thread(r, "Decode #" + mCount.getAndIncrement())
        }
    })

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
            player.clearMediaItems()
//            startTTSReq(shortStr)
            val content = longStr
            val ttsKey = content.hashCode().toString()
            currentTtsKey = ttsKey
            TTSStreamManager.startWithMockData(ttsKey, content)
        }

        binding.tvPlayStream2.setOnClickListener {
//            startTTSReq(shortStr)
            AudioTrackManager.getInstance().stopPlay()
            val content = longStr
            val ttsKey = content.hashCode().toString()
            currentTtsKey = ttsKey
            TTSStreamManager.startWithCompleteData(ttsKey, content)
        }

        binding.tvPlayLocal.setOnClickListener {
            player.clearMediaItems()
//            audioTrackerWrapper.startPlay(MockData.decodeHex(MockData.mp3Data))
//            AudioTrackManager.getInstance().write(MockData.decodeHex(MockData.mp3Data))
//            player.addMediaItemWithByteArray(MockData.decodeHex(MockData.mp3Data), "")
        }

        binding.tvPlayNet.setOnClickListener {
            val key = mp3Url
            val url = mp3Url
            currentTtsKey = key
            if (verifyPlaying(key)) {
                return@setOnClickListener
            }
            player.clearMediaItems()
            player.addMediaItem(url, key)
        }

        binding.tvDownUrl.setOnClickListener {
            val key = "mp3Url"
            currentTtsKey = key
            if (verifyPlaying(key)) {
                return@setOnClickListener
            }
            player.clearMediaItems()

            // 有缓存直接播放
            val cachePath = TTSFileUtil.checkCacheFileFromKey(key)?.path
            if (cachePath != null) {
                Log.i(TAG, "exist cache cachePath:${cachePath}")
                player.addMediaItem(cachePath, key)
                return@setOnClickListener
            }
            // 在线播放
            player.addMediaItem(mp3Url, key)

            // 离线下载
            val file = TTSFileUtil.createCacheFileFromUrl(key, mp3Url)
            FileDownloadManager.download(mp3Url, file.path)
        }
        AudioTrackManager.getInstance().prepareAudioTrack()
    }

    private fun startTTSReq(content: String) {
        val ttsKey = content.hashCode().toString()
        Log.w(TAG, "click clickTtsKey:${ttsKey} content:${content}")
        // 如果播放中，停止播放
        if (player.isPlaying(ttsKey)) {
            Log.w(TAG, "already playing, stop")
            player.stop()
            currentTtsKey = null
            TTSStreamManager.cancelConnect(ttsKey)
            return
        }

        player.clearMediaItems()
        currentTtsKey = ttsKey

        // 有缓存直接播放
//        val cacheFile = TTSFileUtil.checkCacheFileFromKey(ttsKey)
//        if (cacheFile != null) {
//            Log.w(TAG, "exist cache ${cacheFile.path}")
//            player.addMediaItem(cacheFile.path, ttsKey)
//            return
//        }

        // tts流式请求分片播放
        TTSStreamManager.startConnect(content, ttsKey)
    }

    private fun verifyPlaying(ttsKey: String): Boolean {
        // 如果播放中，停止播放
        if (player.isPlaying(ttsKey)) {
            Log.w(TAG, "already playing, stop")
            player.stop()
            return true
        }
        return false
    }

    private val ttsStreamListener = object : TTSStreamListener {

        override fun onReceiveCompleteUrl(ttsKey: String, url: String) {
            Log.i(TAG, "onReceiveCompleteUrl clickTtsKey:${currentTtsKey} ttsKey:${ttsKey}")
            if (currentTtsKey == ttsKey) {
                player.addMediaItem(url, ttsKey)
            }
        }

        override fun onReceiveChunk(dataSource: ChunkDataSource) {
            val ttsKey = dataSource.ttsKey
            Log.i(
                TAG,
                "onReceiveChunk clickTtsKey:${currentTtsKey} ttsKey:$ttsKey"
            )
            // 仅播放最后一个被点击的内容
            if (currentTtsKey == ttsKey) {
                val originByte = dataSource.audioData
                //TODO 播放
                // ExoPlayer 播放
                ThreadUtil.runOnUiThread {
                    player.addMediaItemWithByteArray(originByte, ttsKey)
                }

                // AudioTrack 播放
//                executorService.execute {
//                    val decodeData = MP3Decoder.decodeMP3(originByte) ?: return@execute
//                    Log.i(TAG, "decodeData=$originByte")
//                    AudioTrackManager.getInstance().write(originByte)
//                }

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
        player.addPlayerListener(object : OnPlayerListener {
            override fun onPlaybackStateChanged(playKey: String, playState: Int) {
                when (playState) {
                    PlayState.LOADING -> {
//                    binding.tvPlayStatus.text = "loading"
                    }
                    PlayState.PLAYING -> {
//                    binding.tvPlayStatus.text = "playing"
                    }
                    PlayState.IDLE -> {
//                    binding.tvPlayStatus.text = "default"
                    }
                }
            }

            override fun onPlayerError(uri: String, playKey: String, desc: String) {
                // 离线播放失败，删除缓存。方便下次通过在线播放
                if (YWFileUtil.isLocalPath(uri)) {
                    File(uri).delete()
                }
                Toast.makeText(context, getString(R.string.net_error_toast), Toast.LENGTH_SHORT).show()
            }

        })
    }

    override fun onResume() {
        super.onResume()
        AudioFocusManager.requestAudioFocus()
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop")
        TTSStreamManager.listener = null
        player.stop()
        AudioFocusManager.abandonAudioFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

}