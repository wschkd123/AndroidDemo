package com.example.beyond.demo.ui.player

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.base.BaseFragment
import com.example.base.player.AudioController
import com.example.beyond.demo.databinding.FragmentExoPlayerBinding
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.base.util.YWFileUtil

/**
 * ExoPlayer播放
 *
 * @author wangshichao
 * @date 2024/6/15
 */
class ExoPlayerFragment: BaseFragment() {

    private var _binding: FragmentExoPlayerBinding? = null
    private val binding get() = _binding!!
    private val playbackStateListener: Player.Listener = playbackStateListener()
    private var player: Player? = null

    private var playWhenReady = true
    private var mediaItemIndex = 0
    private var playbackPosition = 0L
    private val mp3Url = "https://www.cambridgeenglish.org/images/153149-movers-sample-listening-test-vol2.mp3"
    private val mp3Path by lazy { YWFileUtil.getStorageFileDir(context).path + "/test.mp3" }


    private val audioController = AudioController()

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
            player?.clearMediaItems()
            //TODO 解决切换后监听的问题
            val chatSpeechHelper = SpeechStreamHelper(it.context, object : SpeechStreamListener {
                override fun onReceiveChunk(dataSource: MediaDataSource) {
                    replaceDataSource(dataSource)
                }

            })
            chatSpeechHelper.loadData("Server-Send Events")
        }

        binding.tvPlayStream2.setOnClickListener {
            player?.clearMediaItems()
            val chatSpeechHelper = SpeechStreamHelper(it.context, object : SpeechStreamListener {
                override fun onReceiveChunk(dataSource: MediaDataSource) {
                    replaceDataSource(dataSource)
                }

            })
            chatSpeechHelper.loadData("你好筑梦岛")
        }

        binding.tvPlayLocal.setOnClickListener {
            player?.apply {
                clearMediaItems()
                addMediaItem(MediaItem.fromUri(mp3Path))
                prepare()
                playWhenReady = playWhenReady
            }
        }

        binding.tvPlayNet.setOnClickListener {
            player?.apply {
                clearMediaItems()
                addMediaItem(MediaItem.fromUri(mp3Url))
                prepare()
                playWhenReady = playWhenReady
            }
        }
    }

    private fun replaceDataSource(dataSource: MediaDataSource) {
        if (dataSource.isEnd.not()) {
            Log.w(TAG, "receive stream path:${dataSource.chunkPath}")
            player?.addMediaItem(MediaItem.fromUri(dataSource.chunkPath))
            player?.prepare()
            player?.playWhenReady = playWhenReady
        }
    }


    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    public override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(binding.root.context)
            .build()
            .also { exoPlayer ->
//                binding.videoView.player = exoPlayer

                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.addListener(playbackStateListener)
            }
    }

    private fun releasePlayer() {
        player?.let { player ->
            playbackPosition = player.currentPosition
            mediaItemIndex = player.currentMediaItemIndex
            playWhenReady = player.playWhenReady
            player.removeListener(playbackStateListener)
            player.release()
        }
        player = null
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.i(TAG, "changed state to $stateString")
        }
    }

}