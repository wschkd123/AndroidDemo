package com.example.beyond.demo.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.ViewCompat

class ScalableContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val gestureDetector: GestureDetector
    private var scaleFactor = 1.0f // 当前缩放比例
    private val minScale = 0.8f // 最小缩放比例
    private val scaleSpeed = 0.005f // 缩放速度系数，根据滑动距离调整这个值可以改变缩放灵敏度

    init {
        gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    // 处理水平滑动
                    // distanceX 是上次调用 onScroll 到此次的滑动距离。向右滑为负，向左滑为正。
                    // 我们希望向右滑（distanceX为负）时缩小，向左滑（distanceX为正）时恢复放大。
                    val targetScale = scaleFactor + (-distanceX) * scaleSpeed
                    scaleFactor = targetScale.coerceIn(minScale, 1.0f) // 限制缩放范围

                    // 应用缩放到子View
                    getChildAt(0)?.let { child ->
                        child.scaleX = scaleFactor
                        child.scaleY = scaleFactor
                    }
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    // 如果需要根据快速滑动手势（Fling）来触发一个平滑的缩放动画，可以在这里处理
                    // 例如，快速向右滑时，平滑缩放到minScale；快速向左滑时，平滑恢复到1.0f
                    animateScale(velocityX < 0) // 一个简单的示例：velocityX方向判断目标缩放
                    return true
                }
            })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    /**
     * 平滑动画到目标缩放比例
     * @param toMinScale 如果为true，则动画缩放到minScale；否则恢复到1.0f
     */
    private fun animateScale(toMinScale: Boolean) {
        val targetScale = if (toMinScale) minScale else 1.0f
        ViewCompat.animate(getChildAt(0)!!)
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(300) // 动画时长
            .setInterpolator(DecelerateInterpolator())
            .start()
        scaleFactor = targetScale
    }
}