package com.example.beyond.demo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.customview.widget.ViewDragHelper;

/**
 * 支持日常模式聊天室的容器。可以通过手势从左侧边缘滑动打开切换日常聊天室和普通聊天室
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
    // 抽屉状态监听器
    private DrawerListener mDrawerListener;
    // 当前抽屉状态
    private int mDrawerState = STATE_CLOSED;
    // 是否启用两阶段滑动模式
    private boolean mEnableTwoStages = true;

    // 抽屉状态常量
    public static final int STATE_IDLE = 0;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;
    public static final int STATE_OPENED = 3;
    public static final int STATE_CLOSED = 4;

    /**
     * 抽屉状态监听器接口
     */
    public interface DrawerListener {
        /**
         * 抽屉滑动时回调
         * @param slideOffset 滑动偏移比例（0-1）
         */
        void onDrawerSlide(float slideOffset);

        /**
         * 抽屉状态改变时回调
         * @param newState 新状态
         */
        void onDrawerStateChanged(int newState);

        /**
         * 抽屉完全打开时回调
         */
        void onDrawerOpened();

        /**
         * 抽屉完全关闭时回调
         */
        void onDrawerClosed();
    }

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
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
    }

    /**
     * 设置抽屉监听器
     */
    public void setDrawerListener(DrawerListener listener) {
        mDrawerListener = listener;
    }

    /**
     * 设置是否启用两阶段滑动
     */
    public void setEnableTwoStages(boolean enable) {
        mEnableTwoStages = enable;
    }

    /**
     * 判断是否启用两阶段滑动
     */
    public boolean isEnableTwoStages() {
        return mEnableTwoStages;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 1) {
            throw new IllegalArgumentException("ChatSwipeLayout must have exactly one child");
        }
        mContentView = getChildAt(0);
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
        mContentView.layout(0, 0, r - l, b - t);

        int centerX = mContentView.getWidth() / 2;
        int centerY = mContentView.getHeight() / 2;
        mContentView.setPivotX(centerX);
        mContentView.setPivotY(centerY);
    }

    /**
     * 更新抽屉状态并通知监听器
     */
    private void updateDrawerState(int newState) {
        if (mDrawerState == newState) return;

        int oldState = mDrawerState;
        mDrawerState = newState;

        if (mDrawerListener != null) {
            mDrawerListener.onDrawerStateChanged(newState);

            // 状态转换处理
            if (oldState != STATE_OPENED && newState == STATE_OPENED) {
                mDrawerListener.onDrawerOpened();
            } else if (oldState != STATE_CLOSED && newState == STATE_CLOSED) {
                mDrawerListener.onDrawerClosed();
            }
        }
    }

    private class DragCallback extends ViewDragHelper.Callback {
        private float mInitialScale = 1.0f;
        // 第一阶段最大滑动距离（屏幕宽度的20%）
        private int mFirstStageMaxDistance;
        // 第二阶段最大滑动距离（屏幕宽度的50% - 20%）
        private int mSecondStageMaxDistance;

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            switch (state) {
                case ViewDragHelper.STATE_IDLE:
                    // 静止状态，判断是完全打开还是关闭
                    int left = mContentView.getLeft();
                    if (left >= mScreenWidth / 2) {
                        updateDrawerState(STATE_OPENED);
                    } else {
                        updateDrawerState(STATE_CLOSED);
                    }
                    break;
                case ViewDragHelper.STATE_DRAGGING:
                    updateDrawerState(STATE_DRAGGING);
                    break;
                case ViewDragHelper.STATE_SETTLING:
                    updateDrawerState(STATE_SETTLING);
                    break;
            }
        }

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child == mContentView;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            // 计算阶段距离（确保在onMeasure之后执行）
            if (mScreenWidth > 0) {
                mFirstStageMaxDistance = (int) (mScreenWidth * 0.2f);
                mSecondStageMaxDistance = mScreenWidth / 2 - mFirstStageMaxDistance;
            }

            // 根据模式限制滑动范围
            int maxRange = mEnableTwoStages ? mScreenWidth / 2 : mFirstStageMaxDistance;
            return Math.max(0, Math.min(left, maxRange));
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            return mScreenWidth;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);

            float slideRatio;
            float scale;

            if (mEnableTwoStages) {
                // 两阶段模式
                if (left <= mFirstStageMaxDistance) {
                    // 第一阶段：0~20%屏幕宽度，缩放从1.0到0.8
                    slideRatio = (float) left / mFirstStageMaxDistance;
                    scale = mInitialScale - (slideRatio * 0.2f);
                } else {
                    // 第二阶段：20%~50%屏幕宽度，保持0.8缩放
                    scale = 0.8f;
                    // 计算整体滑动比例（0~1）
                    slideRatio = 0.2f + (float)(left - mFirstStageMaxDistance) / mSecondStageMaxDistance * 0.8f;
                }
            } else {
                // 单阶段模式：0~20%屏幕宽度，缩放从1.0到0.8
                slideRatio = (float) left / mFirstStageMaxDistance;
                scale = mInitialScale - (slideRatio * 0.2f);
            }

            // 应用缩放
            changedView.setScaleX(scale);
            changedView.setScaleY(scale);
            Log.d(TAG, "onViewPositionChanged: left=" + left + ", scale=" + scale + ", slideRatio=" + slideRatio);

            // 通知滑动事件
            if (mDrawerListener != null) {
                mDrawerListener.onDrawerSlide(slideRatio);
            }
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            int currentLeft = releasedChild.getLeft();
            int finalLeft;

            if (mEnableTwoStages) {
                // 两阶段模式的释放判断
                if (currentLeft > mScreenWidth / 4 || xvel > mMinFlingVelocity) {
                    finalLeft = mScreenWidth / 2;
                } else {
                    finalLeft = 0;
                }
            } else {
                // 单阶段模式的释放判断（超过一半第一阶段距离则保持缩放）
                if (currentLeft > mFirstStageMaxDistance / 2 || xvel > mMinFlingVelocity) {
                    finalLeft = mFirstStageMaxDistance;
                } else {
                    finalLeft = 0;
                }
            }

            mDragHelper.settleCapturedViewAt(finalLeft, 0);
            invalidate();
        }

        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            super.onEdgeTouched(edgeFlags, pointerId);
            Log.d(TAG, "onEdgeTouched");
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

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