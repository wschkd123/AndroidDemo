package com.example.beyond.demo.ui.player

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.beyond.demo.base.BaseFragment
import com.example.beyond.demo.databinding.FragmentPlayBinding

/**
 * 播放器
 *
 * @author wangshichao
 * @date 2024/6/12
 */
class PlayFragment : BaseFragment() {

    private var _binding: FragmentPlayBinding? = null
    private val binding get() = _binding!!
    private val mediaClient = MediaClient()

    companion object {
        private const val TAG = "PlayFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initPlayer()

        binding.tvPlay.setOnClickListener {
            mediaClient.play("aa")
        }

        binding.tvStop.setOnClickListener {
            mediaClient.pause("aa")
        }
    }

    private fun initPlayer() {

        mediaClient.create(
            "https://downsc.chinaz.net/Files/DownLoad/sound1/201906/11582.mp3",
            "aa",
            { key, player ->
                Log.i(TAG, "$key onReady")
                mediaClient.play("aa")
            },
            { desc ->
                Log.i(TAG, "onError $desc")
            })

        mediaClient.setOnCompleteListener {
            Log.i(TAG, "complete $it")
        }

        mediaClient.setOnPlaybackStateChangedListener { key, time ->
            Log.i(TAG, "playback key:$key time:$time")
        }

        mediaClient.setOnErrorListener { key, desc ->
            Log.i(TAG, "error key:$key desc:$desc")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "onDestroyView")
        mediaClient.release()
    }

}