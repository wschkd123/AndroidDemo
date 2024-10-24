package com.example.beyond.demo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.base.BaseFragment
import com.example.beyond.demo.databinding.FragmentAudioTrackBinding

/**
 * 音频音轨播放动画
 *
 * @author wangshichao
 * @date 2024/10/18
 */
class AudioTrackFragment : BaseFragment() {

    private var _binding: FragmentAudioTrackBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAudioTrackBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.startTv.setOnClickListener {
            binding.musicView.start()

        }
        binding.endTv.setOnClickListener {
            binding.musicView.stop()
        }
    }
}