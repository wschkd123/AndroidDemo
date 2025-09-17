package com.example.beyond.demo.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper

/**
 * 支持日常模式聊天室的可滑动容器。可以从屏幕左侧边缘滑动出现切换聊天室按钮
 * 滑动分两阶段：
 * 1. 第一阶段：从左向右滑动时，子View从1.0缩放到0.8，松手后根据缩放比例决定回弹还是继续缩放到0.8
 * 2. 第二阶段：从0.8开始平移，最大平移距离为屏幕宽度的50%，松手后根据位置和滑动速度决定打开还是关闭
 *
 * @author wangshichao
 * @date 2025/9/15
 */
class ChatSwipeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val mDragHelper: ViewDragHelper

    // 最上层内容视图
    private var mContentView: View? = null

    // 最小滑动速度（用于判断快速滑动）
    private val mMinFlingVelocity: Int

    // 是否处于第一阶段
    private var mInFirstStage = true

    // 第一阶段最大滑动距离（缩放从1.0到0.8）
    private var mFirstStageMaxDistance = 0

    // 第二阶段最大滑动距离（屏幕宽度的50%）
    private var mSecondStageMaxDistance = 0

    private var mInitialX = 0f
    private var mLastX = 0f
    private var mReleaseScale = 1f
    private var mReleaseLeft = 0

    // 当前滑动状态
    private var mSwipeState = STATE_IDLE

    // 监听接口
    private var mSwipeListener: SwipeListener? = null

    /**
     * 滑动监听接口
     */
    interface SwipeListener {
        /**
         * @param slideOffset 滑动偏移比例（0~1）
         */
        fun onSwipe(slideOffset: Float)

        /**
         * 状态改变时回调
         *
         * @param newState 新状态
         */
        fun onSwipeStateChanged(newState: Int)

        /**
         * 完全打开时回调
         */
        fun onCompleteOpened()

        /**
         * 完全关闭时回调
         */
        fun onCompleteClosed()
    }

    init {
        val config = ViewConfiguration.get(context)
        mMinFlingVelocity = config.scaledMinimumFlingVelocity
        mDragHelper = ViewDragHelper.create(this, 100f, DragCallback())
        mDragHelper.minVelocity = mMinFlingVelocity.toFloat()
        // 允许边缘滑动
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT)
    }

    /**
     * 设置滑动监听器
     */
    fun setSwipeListener(listener: SwipeListener?) {
        mSwipeListener = listener
    }

    /**
     * 重置内容位置到左侧
     */
    fun resetContentLeft() {
        val contentLeft = mContentView?.left ?: 0
        if (mContentView != null && contentLeft > 0) {
            mContentView?.offsetLeftAndRight(-contentLeft)
        }
    }

    /**
     * 重置内容状态，包括位置和缩放
     */
    fun resetContentStatus() {
        Log.w(TAG, "resetContentStatus")
        mInFirstStage = true
        mReleaseLeft = 0
        mReleaseScale = MAX_SCALE
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        require(childCount > 0) { "ChatSwipeLayout must have at least one child" }
        mContentView = getChildAt(childCount - 1)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        Log.i(
            TAG,
            "onLayout changed=$changed left=$left width=${right - left} height=${bottom - top}"
        )
        updateSize()
    }

    /**
     * 重写触摸事件拦截逻辑，处理第一阶段
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            mInitialX = ev.x
            mLastX = 0f
        }
        Log.d(TAG, "onInterceptTouchEvent: action=" + ev.action + ", x=" + mInitialX)
        return mDragHelper.shouldInterceptTouchEvent(ev)
    }

    /**
     * 重写触摸事件处理，分离第一阶段逻辑
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_MOVE) {
            mLastX = event.x
            val deltaX = mLastX - mInitialX
            // 未拖动且最小缩放状态时，往左滑动保持第一阶段，往右滑动进入第二阶段
            if (mReleaseLeft == 0 && mReleaseScale == MIN_SCALE) {
                mInFirstStage = deltaX < 0
                Log.w(TAG, "onTouchEvent update stage, mInFirstStage=$mInFirstStage")
            }
            Log.d(
                TAG,
                "onTouchEvent deltaX=$deltaX mReleaseScale=$mReleaseScale mReleaseScale=$mReleaseScale"
            )
        }
        mDragHelper.processTouchEvent(event)
        return true
    }

    private fun updateSize() {
        // 初始化阶段最大滑动距离
        mFirstStageMaxDistance = width / 2
        mSecondStageMaxDistance = width / 2

        // 设置缩放中心为子View中心
        val centerX = mContentView!!.width / 2
        val centerY = mContentView!!.height / 2
        mContentView!!.pivotX = centerX.toFloat()
        mContentView!!.pivotY = centerY.toFloat()
    }

    /**
     * 计算第一阶段缩放比例
     */
    private fun getFirstStageScale(): Float {
        val deltaX = mLastX - mInitialX
        val slideRatio = Math.min(Math.abs(deltaX) / mFirstStageMaxDistance, 1f)
        val scale: Float = if (deltaX >= 0) {
            // 从左往右滑动
            1f - slideRatio * (1 - MIN_SCALE)
        } else if (mReleaseScale == MIN_SCALE) {
            // 从右往左滑动且已经到达最小缩放
            MIN_SCALE + slideRatio * (1 - MIN_SCALE)
        } else {
            MAX_SCALE
        }
        Log.i(
            TAG,
            "getFirstStageScale deltaX=$deltaX slideRatio=$slideRatio scale=$scale mReleaseScale=$mReleaseScale"
        )
        return scale
    }

    /**
     * 更新滑动状态并触发回调
     */
    private fun updateSwipeState(newState: Int) {
        if (mSwipeState == newState) return
        mSwipeState = newState
        if (mSwipeListener != null) {
            mSwipeListener?.onSwipeStateChanged(newState)

            // 触发打开/关闭回调
            if (newState == STATE_OPENED) {
                mSwipeListener?.onCompleteOpened()
            } else if (newState == STATE_CLOSED) {
                mSwipeListener?.onCompleteClosed()
            }
        }
    }


    /**
     * ViewDragHelper回调实现：控制第二阶段滑动规则
     */
    private inner class DragCallback : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return child === mContentView
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return if (mInFirstStage) {
                // 第一阶段不允许拖动
                0
            } else {
                Math.max(0, Math.min(left, mSecondStageMaxDistance))
            }
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return if (mInFirstStage) {
                // 第一阶段不允许拖动
                0
            } else {
                mSecondStageMaxDistance
            }
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            val scale: Float
            if (mInFirstStage) {
                // 第一阶段：根据滑动缩放
                scale = getFirstStageScale()
                changedView.scaleX = scale
                changedView.scaleY = scale
                Log.i(TAG, "onViewPositionChanged scale=$scale")
            } else {
                // 第二阶段：不缩放，只平移
                val slideRatio = left.toFloat() / mSecondStageMaxDistance
                scale = MIN_SCALE
                changedView.scaleX = scale
                changedView.scaleY = scale
                Log.i(
                    TAG,
                    "onViewPositionChanged left=$left dx=$dx slideRatio=$slideRatio scale=$scale"
                )
                mSwipeListener?.onSwipe(slideRatio)
            }
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            val currentLeft = releasedChild.left
            val finalLeft: Int
            if (mInFirstStage) {
                updateSwipeState(STATE_SETTLING)
                val halfScale = (1 + MIN_SCALE) / 2
                val scale: Float = releasedChild.scaleX
                mReleaseScale = if (scale > halfScale) {
                    releasedChild.animate().scaleX(MAX_SCALE).scaleY(MAX_SCALE).start()
                    MAX_SCALE
                } else {
                    releasedChild.animate().scaleX(MIN_SCALE)
                        .scaleY(MIN_SCALE).start()
                    MIN_SCALE
                }
                mReleaseLeft = 0
                Log.i(TAG, "onViewReleased firstStage mReleaseScale=$mReleaseScale")
            } else {
                mReleaseScale = MIN_SCALE
                finalLeft =
                    if (currentLeft > mSecondStageMaxDistance / 2 || xvel > mMinFlingVelocity) {
                        mSecondStageMaxDistance
                    } else {
                        0
                    }
                mReleaseLeft = finalLeft
                mDragHelper.settleCapturedViewAt(finalLeft, 0)
                invalidate()
                Log.i(
                    TAG,
                    "onViewReleased second currentLeft=$currentLeft mReleaseLeft=$mReleaseLeft"
                )
            }
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            Log.d(TAG, "onViewDragStateChanged state=$state")
            if (state == ViewDragHelper.STATE_IDLE) {
                // 空闲状态时判断最终状态
                if (mReleaseScale < MAX_SCALE) {
                    updateSwipeState(STATE_OPENED)
                } else {
                    updateSwipeState(STATE_CLOSED)
                }
            } else if (state == ViewDragHelper.STATE_DRAGGING) {
                updateSwipeState(STATE_DRAGGING)
            } else if (state == ViewDragHelper.STATE_SETTLING) {
                updateSwipeState(STATE_SETTLING)
            }
        }
    }

    override fun computeScroll() {
        super.computeScroll()
        if (mDragHelper.continueSettling(true)) {
            invalidate()
        }
    }

    companion object {
        private const val TAG = "ChatSwipeLayout"
        private const val MIN_SCALE = 0.8f
        private const val MAX_SCALE = 1.0f

        // 滑动状态常量
        private const val STATE_IDLE = 0
        private const val STATE_DRAGGING = 1
        private const val STATE_SETTLING = 2
        private const val STATE_OPENED = 3
        private const val STATE_CLOSED = 4
    }
}