package com.example.beyond.demo.ui.trackanimation

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import com.example.base.BaseFragment
import com.example.base.util.ext.dpToPxFloat
import com.example.beyond.demo.databinding.FragmentDrawerLayoutBinding


/**
 * 音频音轨播放动画
 *
 * @author wangshichao
 * @date 2024/10/18
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
    private var innerDrawerListener = object : DrawerLayout.DrawerListener {
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
//        binding.drawerLayout.addDrawerListener(innerDrawerListener)
        binding.openTv.setOnClickListener {
//            if (drawerOpened) {
//                binding.drawerLayout.closeDrawers()
//            } else {
//                binding.drawerLayout.openDrawer(Gravity.LEFT)
//            }
            startAnimation2()
        }
        startAnimation2()
    }

    private fun startAnimation2() {
        binding.root.post {
            val rotationAnimatorY =
                ObjectAnimator.ofFloat(binding.characterIv, "rotationY", 0f, 180f)
            rotationAnimatorY.setDuration(5000) // 设置动画时长1秒
            rotationAnimatorY.start() // 启动动画

            // 调整相机距离。如果不修改, 则会超出屏幕高度
            binding.characterIv.cameraDistance = 10000.dpToPxFloat()
        }
    }
}