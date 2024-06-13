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
    private val audioController = AudioController()

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

        initPlayer("https://downsc.chinaz.net/Files/DownLoad/sound1/201906/11582.mp3", "aa")

        binding.tvPlay.setOnClickListener {
//            initPlayer("https://downsc.chinaz.net/Files/DownLoad/sound1/201906/11582.mp3", "bb")
//            mediaClient.release()
            audioController.play("aa")
        }

        binding.tvStop.setOnClickListener {
            audioController.pause("aa")
        }
    }

    private fun initPlayer(url: String, key: String) {
        audioController.create(url, key, { key, player ->
            Log.i(TAG, "$key onReady")
            audioController.play(key)
        }, { desc ->
            Log.i(TAG, "onError $desc")
        })

        audioController.setOnCompleteListener {
            Log.i(TAG, "complete $it")
        }

        audioController.setOnPlaybackStateChangedListener { key, time ->
            Log.i(TAG, "playback key:$key time:$time")
        }

        audioController.setOnErrorListener { key, desc ->
            Log.i(TAG, "error key:$key desc:$desc")
        }
    }

    override fun onResume() {
        super.onResume()
        AudioFocusManager.requestAudioFocus()
    }

    override fun onStop() {
        super.onStop()
//        mediaClient.pause("aa")
        AudioFocusManager.abandonAudioFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "onDestroyView")
        audioController.release()
    }

}