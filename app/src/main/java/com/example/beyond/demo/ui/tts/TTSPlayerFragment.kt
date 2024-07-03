package com.example.beyond.demo.ui.tts

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.base.download.FileDownloadManager
import com.example.base.player.AudioFocusManager
import com.example.base.player.OnPlayerListener
import com.example.base.player.PlayState
import com.example.base.player.exoplayer.ExoPlayerWrapper
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.R
import com.example.beyond.demo.databinding.FragmentExoPlayerBinding
import com.example.beyond.demo.ui.tts.data.ChunkDataSource
import com.yuewen.baseutil.ext.getFragmentManager
import java.io.File

/**
 * tts文本转语音并播放
 *
 * @author wangshichao
 * @date 2024/6/21
 */
class TTSPlayerFragment : Fragment(), OnPlayerListener, TTSStreamListener, View.OnClickListener {
    private var _binding: FragmentExoPlayerBinding? = null
    private val binding get() = _binding!!
    private val mp3Path by lazy { YWFileUtil.getStorageFileDir(context)?.path + "/test.mp3" }
    private val player = ExoPlayerWrapper()
    private var currentTtsKey: String? = ""
    private val ttsListenerList: MutableList<OnTTSListener> = mutableListOf()

    companion object {
        private const val TAG = "TTSExoPlayerFragment"
        private const val mp3Url =
            "https://www.cambridgeenglish.org/images/153149-movers-sample-listening-test-vol2.mp3"
        private const val mp3Url1 = "http://music.163.com/song/media/outer/url?id=447925558.mp3"
        private const val longStr = "毒鸡汤大魔王，会收集负面情绪，贱贱毒舌却又心地善良的好哥哥，也是持之以恒、霸气侧漏的灵气复苏时代的最强王者、星图战神。吕树，别名为第九天罗，依靠毒鸡汤成为大魔王。身世成谜，自小在福利院中长大，16岁后脱离福利院，与吕小鱼相依为命，通过卖煮鸡蛋维持生计。擅长怼人、噎人、气人，却从不骂人。平时说话贱贱的，被京都天罗地网同仁称为“贱圣”，但从不骂人，喜欢用讲道理却不似道理的话怼人。"
        private const val shortStr = "毒鸡汤大魔王，会收集负面情绪"

        @JvmStatic
        fun findFragment(context: Context?): TTSPlayerFragment? {
            val fragmentManager = context?.getFragmentManager() ?: return null
            val ttsPlayerFragment = fragmentManager.findFragmentByTag(TAG)
            return ttsPlayerFragment as? TTSPlayerFragment
        }

        fun attachActivity(context: Context?) {
            val fragmentManager = context?.getFragmentManager() ?: return
            fragmentManager.beginTransaction().apply {
                add(TTSPlayerFragment(), TAG)
            }.commitAllowingStateLoss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player.addPlayerListener(this)
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
        binding.tvPlayStream1.setOnClickListener(this)
        binding.tvPlayStream2.setOnClickListener(this)
        binding.tvPlayLocal.setOnClickListener(this)
        binding.tvCleanCache.setOnClickListener(this)
        binding.tvPlayNet.setOnClickListener(this)
        binding.tvDownUrl.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart $this")
        TTSStreamManager.addTTSStreamListener(this)
    }

    /**
     * 开始tts
     */
    private fun startTTS(ttsKey: String, content: String) {
        Log.w(TAG, "ttsKey=${ttsKey}")
        // 如果播放中，停止播放
        if (player.isPlaying(ttsKey)) {
            Log.w(TAG, "already playing, stop")
            stopTTS()
            return
        }

        player.clearMediaItems()
        currentTtsKey = ttsKey

        // 有缓存直接播放
        val cacheFile = TTSFileUtil.checkCacheFileFromKey(ttsKey)
        if (cacheFile != null) {
            Log.w(TAG, "exist cache ${cacheFile.path}")
            player.addMediaItem(cacheFile.path, ttsKey)
            return
        }

        // tts流式请求分片播放
        TTSStreamManager.addTTSStreamListener(this)
        TTSStreamManager.startConnect(ttsKey, content)
    }

    /**
     * 停止tts。取消tts监听并停止播放，tts请求不取消以便缓存
     */
    fun stopTTS() {
        Log.i(TAG, "stopTTS")
        TTSStreamManager.removeTTSStreamListener(this)
        player.stop()
        currentTtsKey = null
        AudioFocusManager.abandonAudioFocus()
    }

    fun addTTSListener(listener: OnTTSListener) {
        if (!ttsListenerList.contains(listener)) {
            ttsListenerList.add(listener)
            Log.i(TAG, "add listener=$listener size=${ttsListenerList.size}")
        }
    }

    fun removeTTSListener(listener: OnTTSListener) {
        Log.i(TAG, "remove $listener")
        ttsListenerList.remove(listener)
    }

    override fun onClick(v: View?) {
        when(v) {
            binding.tvPlayStream1 -> {
                player.clearMediaItems()
                val content = shortStr
                val ttsKey = content.hashCode().toString()
                currentTtsKey = ttsKey
                // 有缓存直接播放
                val cachePath = TTSFileUtil.checkCacheFileFromKey(ttsKey)?.path
                if (cachePath != null) {
                    Log.i(TAG, "exist cache cachePath:${cachePath}")
                    player.addMediaItem(cachePath, ttsKey)
                    return
                }
                TTSStreamManager.startWithMockData(ttsKey, content)
            }

            binding.tvPlayStream2 -> {
                player.clearMediaItems()
                val content = longStr
                val ttsKey = content.hashCode().toString()
                startTTS(ttsKey, content)
            }

            binding.tvPlayLocal -> {
                player.clearMediaItems()
                player.addMediaItem(mp3Path)
            }

            binding.tvCleanCache -> {
                val startTime = System.currentTimeMillis()
                val deleteResult = File(TTSFileUtil.ttsDir).deleteRecursively()
                Log.w(
                    TAG,
                    "deleteChunkFile cost ${System.currentTimeMillis() - startTime} deleteResult:$deleteResult"
                )
            }

            binding.tvPlayNet -> {
                val url = mp3Url1
                val key = url.hashCode().toString()
                currentTtsKey = key
                if (player.isPlaying(key)) {
                    return
                }
                player.clearMediaItems()
                // 有缓存直接播放
                val cachePath = TTSFileUtil.checkCacheFileFromKey(key)?.path
                if (cachePath != null) {
                    Log.i(TAG, "exist cache cachePath:${cachePath}")
                    player.addMediaItem(cachePath, key)
                    return
                }
                player.addMediaItem(url, key)
            }

            binding.tvDownUrl -> {
                val url = mp3Url
                val key = url.hashCode().toString()
                currentTtsKey = key
                if (player.isPlaying(key)) {
                    return
                }
                player.clearMediaItems()

                // 有缓存直接播放
                val cachePath = TTSFileUtil.checkCacheFileFromKey(key)?.path
                if (cachePath != null) {
                    Log.i(TAG, "exist cache cachePath:${cachePath}")
                    player.addMediaItem(cachePath, key)
                    return
                }
                // 在线播放
                player.addMediaItem(url, key)

                // 离线下载
                val file = TTSFileUtil.createCacheFileFromUrl(key, url)
                FileDownloadManager.download(url, file.path)
            }
        }
    }

    override fun onReceiveCompleteUrl(ttsKey: String, url: String) {
        Log.i(TAG, "onReceiveCompleteUrl clickTtsKey=${currentTtsKey} ttsKey=${ttsKey}")
        if (currentTtsKey == ttsKey) {
            player.addMediaItem(url, ttsKey)
        } else {
            resetTTSState(ttsKey)
        }
    }

    override fun onReceiveChunk(dataSource: ChunkDataSource) {
        val ttsKey = dataSource.ttsKey
        Log.i(
            TAG,
            "onReceiveChunk clickTtsKey=${currentTtsKey} ttsKey=$ttsKey"
        )
        if (currentTtsKey == ttsKey) {
            player.addChunk(dataSource.audioData, ttsKey)
        } else {
            resetTTSState(ttsKey)
        }
    }

    override fun onRateLimit(ttsKey: String, code: Int, msg: String) {
        Log.w(TAG, "onReceiveLimit ttsKey=$ttsKey code=${code} msg=$msg")
        Toast.makeText(context, "您点的太快啦", Toast.LENGTH_SHORT).show()
        resetTTSState(ttsKey)
    }

    override fun onLoginInvalid(ttsKey: String, code: Int, msg: String) {
        Toast.makeText(context, "登录态失效，请重新登录", Toast.LENGTH_SHORT).show()
        resetTTSState(ttsKey)
    }

    override fun onNetError(ttsKey: String, msg: String?) {
        Toast.makeText(
            context,
            msg ?: context?.getString(R.string.net_error_toast),
            Toast.LENGTH_SHORT
        ).show()
        resetTTSState(ttsKey)
    }

    override fun onPlaybackStateChanged(playKey: String, playState: Int) {
        ttsListenerList.forEach {
            it.onTTSStateChanged(playKey, playState)
        }
    }

    override fun onPlayerError(uri: String, playKey: String, desc: String) {
        // 离线播放失败，删除缓存。方便下次通过在线播放
        if (YWFileUtil.isLocalPath(uri)) {
            File(uri).delete()
        }
        Toast.makeText(
            context,
            context?.getString(R.string.net_error_toast),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {
        super.onResume()
        AudioFocusManager.requestAudioFocus()
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop $this")
        // 进入后台取消tts监听并停止播放
        stopTTS()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        ttsListenerList.clear()
        player.removePlayerListener(this)
        player.release()
    }

    private fun resetTTSState(ttsKey: String) {
        ttsListenerList.forEach {
            it.onTTSStateChanged(ttsKey, PlayState.IDLE)
        }
    }


}