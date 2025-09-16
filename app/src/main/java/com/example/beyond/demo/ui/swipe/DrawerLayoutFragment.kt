package com.example.beyond.demo.ui.swipe

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.base.BaseFragment
import com.example.base.util.ext.dpToPxFloat
import com.example.beyond.demo.databinding.FragmentDrawerLayoutBinding
import com.example.beyond.demo.view.ChatSwipeLayout


/**
 * 日常模式聊天室
 *
 * @author wangshichao
 * @date 2025/9/10
 */
class DrawerLayoutFragment : BaseFragment() {

    private var _binding: FragmentDrawerLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDrawerLayoutBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private var swipeLayoutListener = object : ChatSwipeLayout.SwipeListener {

        override fun onSwipe(slideOffset: Float) {
            Log.i(TAG, "onDrawerSlide: $slideOffset")
        }

        override fun onSwipeStateChanged(newState: Int) {
            Log.i(TAG, "onSwipeStateChanged newState=$newState")
        }

        override fun onCompleteOpened() {
            Log.i(TAG, "onCompleteOpened")
        }

        override fun onCompleteClosed() {
            Log.i(TAG, "onCompleteClosed")
        }

    }

    private fun initView() {
        binding.drawerLayout.setSwipeListener(swipeLayoutListener)
        binding.drawerLayout.setEnableSecondStage(true)
        initRotationAnimation()
    }

    /**
     * 旋转动画
     */
    private fun initRotationAnimation() {
        binding.openTv.setOnClickListener {
            val rotationAnimatorY =
                ObjectAnimator.ofFloat(binding.contentLayout, "rotationY", 0f, 180f)
            rotationAnimatorY.setDuration(5000) // 设置动画时长1秒
            rotationAnimatorY.start() // 启动动画

            // 调整相机距离。如果不修改, 则会超出屏幕高度
            binding.contentLayout.cameraDistance = 10000.dpToPxFloat()
        }
    }
}