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
    // 第一阶段最大阈值（屏幕宽度的20%）
    private int mFirstStageMax;
    // 第一阶段是否激活
    private boolean mInFirstStage = true;
    // 初始触摸位置
    private float mInitialTouchX;
    // 是否已经处理第一阶段逻辑
    private boolean mFirstStageHandled = false;
    // 第二阶段开关（默认启用）
    private boolean mEnableSecondStage = true;
    // 标记是否处于第二阶段（已滑到最大位置）
    private boolean mIsInSecondStage = false;
    // 当前抽屉状态
    private int mDrawerState = STATE_CLOSED;
    // 监听接口
    private DrawerListener mDrawerListener;

    // 抽屉状态常量
    public static final int STATE_IDLE = 0;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;
    public static final int STATE_OPENED = 3;
    public static final int STATE_CLOSED = 4;

    /**
     * 抽屉监听接口
     */
    public interface DrawerListener {
        /**
         * 抽屉滑动时回调
         * @param slideOffset 滑动偏移比例（0~1）
         */
        void onDrawerSlide(float slideOffset);

        /**
         * 抽屉状态改变时回调
         * @param newState 新状态（STATE_*）
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
        // 允许边缘滑动
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
    }

    /**
     * 设置抽屉监听器
     */
    public void setDrawerListener(DrawerListener listener) {
        mDrawerListener = listener;
    }

    // 设置是否启用第二阶段滑动
    public void setEnableSecondStage(boolean enable) {
        mEnableSecondStage = enable;
        // 如果关闭第二阶段且当前在第二阶段，强制回弹到第一阶段最大位置
        if (!enable && mContentView != null && mContentView.getLeft() > mFirstStageMax) {
            mDragHelper.settleCapturedViewAt(mFirstStageMax, 0);
            invalidate();
        }
    }

    /**
     * 更新抽屉状态并触发回调
     */
    private void updateDrawerState(int newState) {
        if (mDrawerState == newState) return;
        mDrawerState = newState;
        if (mDrawerListener != null) {
            mDrawerListener.onDrawerStateChanged(newState);

            // 触发打开/关闭回调
            if (newState == STATE_OPENED) {
                mDrawerListener.onDrawerOpened();
            } else if (newState == STATE_CLOSED) {
                mDrawerListener.onDrawerClosed();
            }
        }
    }

    /**
     * 计算滑动偏移比例（0~1）
     */
    private float calculateSlideOffset(int left) {
        return (float) left / (mScreenWidth / 2);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 确保只有一个子View
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
        // 强制容器为全屏
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        mScreenWidth = width;
        mFirstStageMax = (int) (mScreenWidth * 0.2f);
        int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        mContentView.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mContentView.layout(0, 0, r - l, b - t);

        // 设置缩放中心为子View中心
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
        if (mIsInSecondStage) {
            return mDragHelper.shouldInterceptTouchEvent(ev);
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitialTouchX = ev.getX();
                mInFirstStage = true;
                mFirstStageHandled = false;
                //TODO 重置子View状态（确保初始状态正确）
//                mContentView.setScaleX(1.0f);
//                mContentView.setScaleY(1.0f);
//                mContentView.layout(0, 0, mContentView.getWidth(), mContentView.getHeight());
//                updateDrawerState(STATE_IDLE);
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

                        // 第一阶段滑动回调（偏移比例按总范围计算）
                        if (mDrawerListener != null) {
                            mDrawerListener.onDrawerSlide(ratio * 0.2f); // 第一阶段占总范围的20%
                        }

                        // 如果超过第一阶段阈值，激活第二阶段
                        if (dx >= mFirstStageMax) {
                            mInFirstStage = false;
                            mFirstStageHandled = true;
                            updateDrawerState(STATE_DRAGGING);
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
        if (mIsInSecondStage) {
            mDragHelper.processTouchEvent(event);
            return true;
        }
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

                        if (mDrawerListener != null) {
                            mDrawerListener.onDrawerSlide(ratio * 0.2f);
                        }

                        if (dx >= mFirstStageMax) {
                            mInFirstStage = false;
                            mFirstStageHandled = true;
                            mDragHelper.processTouchEvent(event);
                            updateDrawerState(STATE_DRAGGING);
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
                    if (mDrawerListener != null) {
                        mDrawerListener.onDrawerSlide(0);
                    }
                    updateDrawerState(STATE_CLOSED);
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
            int maxLeft = mEnableSecondStage ? mScreenWidth / 2 : mFirstStageMax;
            return Math.max(0, Math.min(left, maxLeft));
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

            // 更新第二阶段状态（滑动到95%以上视为已进入第二阶段）
            mIsInSecondStage = left >= mScreenWidth / 2 * 0.95f;

            // 计算总偏移比例（第一阶段20% + 第二阶段80%）
            float totalOffset = 0.2f + (calculateSlideOffset(left) * 0.8f);
            if (mDrawerListener != null) {
                mDrawerListener.onDrawerSlide(totalOffset);
            }
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            updateDrawerState(STATE_SETTLING);

            int currentLeft = releasedChild.getLeft();
            int targetLeft;

            // 根据第二阶段开关决定释放后的目标位置
            if (mEnableSecondStage) {
                // 启用第二阶段：正常判断是否滑到一半
                targetLeft = (currentLeft > mScreenWidth / 4 || xvel > mMinFlingVelocity)
                        ? mScreenWidth / 2
                        : 0;
            } else {
                // 关闭第二阶段：最多滑到第一阶段最大位置
                targetLeft = (currentLeft > mFirstStageMax / 2 || xvel > mMinFlingVelocity)
                        ? mFirstStageMax
                        : 0;
            }

            // 释放后更新第二阶段状态
            mIsInSecondStage = (targetLeft == mScreenWidth / 2);
            mDragHelper.settleCapturedViewAt(targetLeft, 0);
            invalidate();
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (state == ViewDragHelper.STATE_IDLE) {
                // 空闲状态时判断最终状态
                if (mContentView.getLeft() >= mScreenWidth / 2) {
                    updateDrawerState(STATE_OPENED);
                } else {
                    updateDrawerState(STATE_CLOSED);
                }
            } else if (state == ViewDragHelper.STATE_DRAGGING) {
                updateDrawerState(STATE_DRAGGING);
            } else if (state == ViewDragHelper.STATE_SETTLING) {
                updateDrawerState(STATE_SETTLING);
            }
        }
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