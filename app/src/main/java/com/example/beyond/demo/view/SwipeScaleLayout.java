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
public class SwipeScaleLayout extends ViewGroup {
    // 滑动辅助工具
    private final ViewDragHelper mDragHelper;
    // 唯一的子View
    private View mContentView;
    // 屏幕宽度（用于计算滑动比例）
    private int mScreenWidth;
    // 最小滑动速度（用于判断快速滑动）
    private final int mMinFlingVelocity;

    public SwipeScaleLayout(Context context) {
        this(context, null);
    }

    public SwipeScaleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeScaleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
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
            throw new IllegalArgumentException("SwipeScaleLayout must have exactly one child");
        }
        mContentView = getChildAt(0);
        // 设置子View为全屏（占满容器）
        mContentView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 强制容器为精确尺寸（全屏）
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        // 测量子View为全屏
        mScreenWidth = width;
        int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        mContentView.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // 子View初始位置：左上角对齐容器
        mContentView.layout(0, 0, r - l, b - t);
    }

    /**
     * ViewDragHelper回调实现：控制滑动规则和缩放效果
     */
    private class DragCallback extends ViewDragHelper.Callback {
        // 记录初始缩放值
        private float mInitialScale = 1.0f;

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            // 只允许捕获我们的内容View
            return child == mContentView;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            // 限制水平滑动范围：只能向右滑动（0到屏幕宽度的50%为例）
            return Math.max(0, Math.min(left, mScreenWidth / 2));
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            // 定义水平拖动范围（用于计算滑动比例）
            return mScreenWidth;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            // 计算滑动比例（0~1）：left越大，比例越高
            float slideRatio = (float) left / (mScreenWidth / 2);
            
            // 根据滑动比例计算缩放值（1.0~0.8）：滑动越远，缩放越小
            float scale = mInitialScale - (slideRatio * 0.2f);
            changedView.setScaleX(scale);
            changedView.setScaleY(scale);
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            // 滑动结束后：根据位置和速度决定是否回到初始位置
            int currentLeft = releasedChild.getLeft();
            // 超过一半宽度或向右的速度足够快，则保持打开状态；否则回弹
            if (currentLeft > mScreenWidth / 4 || xvel > mMinFlingVelocity) {
                mDragHelper.settleCapturedViewAt(mScreenWidth / 2, 0);
            } else {
                mDragHelper.settleCapturedViewAt(0, 0);
            }
            invalidate(); // 触发动画
        }
    }

    // 以下是触摸事件分发和滑动动画的必要实现

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 让ViewDragHelper判断是否拦截事件
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 将触摸事件交给ViewDragHelper处理
        mDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        // 处理滑动动画
        if (mDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    // 简化的布局参数（仅支持MATCH_PARENT）
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