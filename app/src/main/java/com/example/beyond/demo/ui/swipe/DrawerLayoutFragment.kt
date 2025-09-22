package com.example.beyond.demo.ui.swipe

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.base.BaseFragment
import com.example.base.util.ext.dpToPxFloat
import com.example.beyond.demo.databinding.FragmentDrawerLayoutBinding
import com.example.beyond.demo.ui.swipe.view.ChatSwipeLayout
import com.example.beyond.demo.ui.swipe.view.ChatView


/**
 * 日常模式聊天室
 *
 * @author wangshichao
 * @date 2025/9/10
 */
class DrawerLayoutFragment : BaseFragment() {

    private var _binding: FragmentDrawerLayoutBinding? = null
    private val binding get() = _binding!!
    private var chatView: ChatView? = null

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
        initListener()
        chatView = ChatView(requireContext())
        binding.swipeLayout.setDragView(chatView, true)
    }

    /**
     * 旋转动画
     */
    private fun initListener() {
        binding.swipeLayout.setSwipeListener(object : ChatSwipeLayout.SwipeListener {

            override fun onSwipe(swipeRatio: Float, inFirstStage: Boolean) {
                if (inFirstStage) {
                    chatView?.setProgress(swipeRatio)
                    Log.i(TAG, "onDrawerSlide inFirstStage swipeRatio=$swipeRatio")
                } else {
                    Log.i(TAG, "onDrawerSlide: swipeRatio=$swipeRatio")
                }
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

        })

        binding.openTv.setOnClickListener {
            // 重置内容位置
            binding.swipeLayout.resetContentLeft()

            val dragView = chatView!!
            // 1. 正面贴上截图
            //TODO 异步截图
            val screenShotView = generateScreenShotView(dragView)
            captureView(dragView)?.let { bitmap ->
                screenShotView.setImageBitmap(bitmap)
                binding.swipeLayout.addView(screenShotView)
            }

            // 1. 截图视图旋转90度
            dragView.rotationY = -90f
            rotationAnimation(screenShotView, 0f, 90f) {
                // 移除截图视图
                binding.swipeLayout.removeView(screenShotView)

                // 2. 内容视图从-90度旋转到0度
                rotationAnimation(dragView, -90f, 0f) {

                    // 3. 内容视图恢复未缩放态
                    dragView.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(500)
                        .setUpdateListener {
                            val progress = it.animatedValue as Float
                            chatView?.setProgress(1 - progress)
                        }.start()
                    binding.swipeLayout.resetContentStatus()
                }
            }
        }
    }

    /**
     * 创建截图视图
     */
    private fun generateScreenShotView(view: View): ImageView {
        val imageView = ImageView(context).apply {
            scaleX = view.scaleX
            scaleY = view.scaleY
            rotationY = 0f
            layoutParams = ViewGroup.LayoutParams(view.width, view.height)
        }
        return imageView
    }

    private fun captureView(view: View): Bitmap? {
        if (view.width <= 0 || view.height <= 0) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }
        if (view.width <= 0 || view.height <= 0) {
            return null
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun rotationAnimation(view: View, startRotation: Float = 0f, endRotation: Float = 90f, endInvoke: (() -> Unit)? = null) {
        // 调整相机距离。如果不修改, 则会超出屏幕高度
        view.cameraDistance = 10000.dpToPxFloat()
        val rotationAnimator =
            ObjectAnimator.ofFloat(view, "rotationY", startRotation, endRotation)
        rotationAnimator.apply {
            duration = 1000
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    endInvoke?.invoke()
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    endInvoke?.invoke()
                }
            })
            start()
        }
    }
}