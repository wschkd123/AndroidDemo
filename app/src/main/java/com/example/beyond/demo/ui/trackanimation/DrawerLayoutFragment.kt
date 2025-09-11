package com.example.beyond.demo.ui.trackanimation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.base.BaseFragment
import com.example.beyond.demo.databinding.FragmentDrawerLayoutBinding


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

    var drawerOpened = false
        private set
    private var innerDrawerListener = object : com.example.beyond.demo.view.DrawerLayout.DrawerListener {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            drawerOpened = slideOffset == 1f
            Log.i(TAG, "onDrawerSlide: $slideOffset")
        }

        override fun onDrawerOpened(drawerView: View) {
            drawerOpened = true
            Log.i(TAG, "onDrawerOpened")
        }

        override fun onDrawerClosed(drawerView: View) {
            val lp = drawerView.layoutParams as ViewGroup.MarginLayoutParams
            Log.i(TAG, "onDrawerClosed")
        }

        override fun onDrawerStateChanged(newState: Int) {
            Log.i(TAG, "onDrawerStateChanged newState=$newState")
        }
    }

    private fun initView() {
        binding.drawerLayout.addDrawerListener(innerDrawerListener)
//        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
//        binding.openTv.setOnClickListener {
//            if (drawerOpened) {
//                binding.drawerLayout.closeDrawers()
//            } else {
//                binding.drawerLayout.openDrawer(Gravity.LEFT)
//            }
//            startRotationAnimation()
//        }
    }

    private fun startRotationAnimation() {
//        binding.root.post {
//            val rotationAnimatorY =
//                ObjectAnimator.ofFloat(binding.characterIv, "rotationY", 0f, 180f)
//            rotationAnimatorY.setDuration(5000) // 设置动画时长1秒
//            rotationAnimatorY.start() // 启动动画
//
//            // 调整相机距离。如果不修改, 则会超出屏幕高度
//            binding.characterIv.cameraDistance = 10000.dpToPxFloat()
//        }
    }
}