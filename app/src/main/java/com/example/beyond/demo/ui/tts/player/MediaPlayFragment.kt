package com.example.beyond.demo.ui.tts.player

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.base.AppContext
import com.example.base.BaseFragment
import com.example.base.player.AudioController
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.databinding.FragmentPlayBinding

/**
 * MediaPlayer 播放器
 *
 * @author wangshichao
 * @date 2024/6/12
 */
class MediaPlayFragment : BaseFragment() {

    private var _binding: FragmentPlayBinding? = null
    private val binding get() = _binding!!
    private val audioController = AudioController()

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

        binding.tvPlay1.setOnClickListener {
            val path = YWFileUtil.getStorageFileDir(AppContext.application)?.path + "/Audio/" + "test.mp3"
            preparePlay(path)
        }

        binding.tvPlay2.setOnClickListener {
            preparePlay("https://www.cambridgeenglish.org/images/153149-movers-sample-listening-test-vol2.mp3")
        }

        binding.tvPlay.setOnClickListener {
            audioController.play()
        }

        binding.tvStop.setOnClickListener {
            audioController.pause()
        }

    }

    private fun preparePlay(url: String) {
        Log.i(TAG, "initPlayer url:$url")
        audioController.prepare(url, {
            Log.i(TAG, "onReady")
        }, { desc ->
            Log.i(TAG, "onError $desc")
        })

        audioController.setOnCompletionListener {
            Log.i(TAG, "complete")
        }

        audioController.setOnPlaybackStateChangedListener {
            Log.i(TAG, "playbackState $it")
        }

        audioController.setOnErrorListener { desc ->
            Log.i(TAG, "error desc:$desc")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        AudioFocusManager.requestAudioFocus()
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop")
        audioController.release()
        AudioFocusManager.abandonAudioFocus()
    }


}