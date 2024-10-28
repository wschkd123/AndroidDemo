package com.example.beyond.demo.ui.trackanimation

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
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
        val trackView = TrackAnimationView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(1000, 1000)
            lineColor = Color.parseColor("#111111")
            setBackgroundColor(Color.parseColor("#f6f6f6"))
        }
        binding.trackContainer.addView(trackView)
//        for (i in 0 until 100) {
//            val trackView = TrackAnimationView(requireContext()).apply {
//                layoutParams = ViewGroup.LayoutParams(12.dpToPx(), 12.dpToPx())
//                lineColor = Color.parseColor("#111111")
//                setBackgroundColor(Color.parseColor("#f6f6f6"))
//            }
//            binding.trackContainer.addView(trackView)
//        }

        binding.startTv.setOnClickListener {
            binding.trackContainer.forEach {
                if (it is TrackAnimationView) {
                    it.play()
                }
            }

        }
        binding.endTv.setOnClickListener {
            binding.trackContainer.forEach {
                if (it is TrackAnimationView) {
                    it.stop()
                }
            }
        }
    }
}