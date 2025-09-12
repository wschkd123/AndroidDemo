package com.example.beyond.demo.view;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.customview.widget.ViewDragHelper;

/**
 * 简化版滑动缩放容器：仅包含一个全屏子View，支持滑动时的位置偏移和缩放效果
 */
public class ChatSwipeLayout extends ViewGroup {
    private static final String TAG = "ChatSwipeLayout";
    private final ViewDragHelper mDragHelper;
    // 唯一的子View
    private View mContentView;
    // 屏幕宽度（用于计算滑动比例）
    private int mScreenWidth;
    // 最小滑动速度（用于判断快速滑动）
    private final int mMinFlingVelocity;
    // 第一阶段最大阈值（屏幕宽度的20%）
    private int mFirstStageMax;
    // 第一阶段是否激活（用于触摸事件判断）
    private boolean mInFirstStage = true;
    // 初始触摸位置（用于计算第一阶段滑动距离）
    private float mInitialTouchX;
    // 是否已经处理第一阶段逻辑
    private boolean mFirstStageHandled = false;

    public ChatSwipeLayout(Context context) {
        this(context, null);
    }

    public ChatSwipeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChatSwipeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 初始化滑动参数
        ViewConfiguration config = ViewConfiguration.get(context);
        mMinFlingVelocity = config.getScaledMinimumFlingVelocity();

        // 创建ViewDragHelper，设置回调
        mDragHelper = ViewDragHelper.create(this, 1.0f, new DragCallback());
        mDragHelper.setMinVelocity(mMinFlingVelocity);
        // 允许边缘滑动（可选）
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 确保只有一个子View
        if (getChildCount() != 1) {
            throw new IllegalArgumentException("ChatSwipeLayout must have exactly one child");
        }
        mContentView = getChildAt(0);
        // 设置子View为全屏（占满容器）
        mContentView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
        // 设置缩放中心为子View中心
        mContentView.setPivotX(mContentView.getWidth() / 2);
        mContentView.setPivotY(mContentView.getHeight() / 2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 强制容器为精确尺寸（全屏）
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        // 测量子View为全屏
        mScreenWidth = width;
        mFirstStageMax = (int) (mScreenWidth * 0.2f); // 初始化第一阶段阈值
        int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        mContentView.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // 子View初始位置：左上角对齐容器
        mContentView.layout(0, 0, r - l, b - t);

        int centerX = mContentView.getWidth() / 2;
        int centerY = mContentView.getHeight() / 2;
        mContentView.setPivotX(centerX);
        mContentView.setPivotY(centerY);
    }

    /**
     * 重写触摸事件拦截逻辑，处理第一阶段
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitialTouchX = ev.getX();
                mInFirstStage = true;
                mFirstStageHandled = false;
                // 重置子View状态（确保初始状态正确）
                mContentView.setScaleX(1.0f);
                mContentView.setScaleY(1.0f);
                mContentView.layout(0, 0, mContentView.getWidth(), mContentView.getHeight());
                break;

            case MotionEvent.ACTION_MOVE:
                if (mInFirstStage) {
                    float currentX = ev.getX();
                    float dx = currentX - mInitialTouchX;
                    // 只处理向右滑动
                    if (dx > 0) {
                        // 计算第一阶段滑动比例
                        float ratio = Math.min(dx / mFirstStageMax, 1.0f);
                        // 应用缩放（1.0 -> 0.8）
                        float scale = 1.0f - (ratio * 0.2f);
                        mContentView.setScaleX(scale);
                        mContentView.setScaleY(scale);

                        // 如果超过第一阶段阈值，激活第二阶段
                        if (dx >= mFirstStageMax) {
                            mInFirstStage = false;
                            mFirstStageHandled = true;
                            // 传递事件给ViewDragHelper处理
                            return mDragHelper.shouldInterceptTouchEvent(ev);
                        }
                        // 第一阶段自己处理，不拦截
                        return false;
                    }
                }
                break;
        }
        // 第二阶段交给ViewDragHelper判断
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    /**
     * 重写触摸事件处理，分离第一阶段逻辑
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (mInFirstStage) {
                    float currentX = event.getX();
                    float dx = currentX - mInitialTouchX;
                    if (dx > 0) {
                        float ratio = Math.min(dx / mFirstStageMax, 1.0f);
                        float scale = 1.0f - (ratio * 0.2f);
                        mContentView.setScaleX(scale);
                        mContentView.setScaleY(scale);

                        if (dx >= mFirstStageMax) {
                            mInFirstStage = false;
                            mFirstStageHandled = true;
                            // 切换到第二阶段，将事件交给ViewDragHelper
                            mDragHelper.processTouchEvent(event);
                        }
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mInFirstStage && !mFirstStageHandled) {
                    // 第一阶段释放，回弹到初始状态
                    mContentView.setScaleX(1.0f);
                    mContentView.setScaleY(1.0f);
                    return true;
                }
                break;
        }
        // 第二阶段交给ViewDragHelper处理
        mDragHelper.processTouchEvent(event);
        return true;
    }

    /**
     * ViewDragHelper回调实现：控制第二阶段滑动规则
     */
    private class DragCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            // 只允许捕获我们的内容View，且仅在第二阶段
            return child == mContentView && !mInFirstStage;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            // 第二阶段滑动范围：从0到屏幕宽度的50%
            return Math.max(0, Math.min(left, mScreenWidth / 2));
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            return mScreenWidth;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            // 第二阶段保持缩放为0.8
            changedView.setScaleX(0.8f);
            changedView.setScaleY(0.8f);
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            int currentLeft = releasedChild.getLeft();
            // 判断是否需要回弹
            if (currentLeft > mScreenWidth / 4 || xvel > mMinFlingVelocity) {
                mDragHelper.settleCapturedViewAt(mScreenWidth / 2, 0);
            } else {
                mDragHelper.settleCapturedViewAt(0, 0);
                // 回到初始位置时重置缩放
                releasedChild.setScaleX(1.0f);
                releasedChild.setScaleY(1.0f);
            }
            invalidate();
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    // 布局参数相关代码保持不变
    public static class LayoutParams extends ViewGroup.LayoutParams {
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p.width, p.height);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }
}