package com.example.beyond.demo.ui.pag

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.base.dialog.BaseDialogFragment
import com.example.beyond.demo.databinding.DialogConfessionAnimationBinding
import org.libpag.PAGView

/**
 * 表白全屏动画
 *
 * @author wangshichao
 * @date 2024/9/12
 */
class ConfessionAnimationDialog: BaseDialogFragment() {

    private var _binding: DialogConfessionAnimationBinding? = null
    private val binding get() = _binding!!

    init {
        isFullScreen = true
        dimBehindAlpha = 0.8f
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogConfessionAnimationBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "start")
        binding.closeIv.setOnClickListener { dismissAllowingStateLoss() }
        binding.pagView.setRepeatCount(0)
        binding.pagView.setPathAsync("https://pag.io/file/like.pag") { pagFile ->
            Log.i(TAG, "pagFile=${pagFile}")
            binding.pagView.play()
        }

        binding.pagView.addListener(object : PAGView.PAGViewListener {
            override fun onAnimationStart(view: PAGView?) {
                Log.i(TAG, "onAnimationStart")
            }

            override fun onAnimationEnd(view: PAGView?) {
                Log.i(TAG, "onAnimationEnd")
            }

            override fun onAnimationCancel(view: PAGView?) {
                Log.i(TAG, "onAnimationCancel")
            }

            override fun onAnimationRepeat(view: PAGView?) {
                Log.i(TAG, "onAnimationRepeat")
            }

            override fun onAnimationUpdate(pagView: PAGView?) {
                Log.i(TAG, "onAnimationUpdate progress=${pagView?.progress}")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.pagView.play()
    }

    override fun onStop() {
        super.onStop()
        binding.pagView.pause()
    }

}