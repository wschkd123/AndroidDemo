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

import com.example.base.util.YWDeviceUtil;

/**
 * 支持日常模式聊天室的容器。可以通过手势从左侧边缘滑动打开切换日常聊天室和普通聊天室
 */
public class ChatSwipeLayout extends ViewGroup {
    private static final String TAG = "ChatSwipeLayout";
    private final ViewDragHelper mDragHelper;
    // 唯一的子View
    private View mContentView;
    // 最小滑动速度（用于判断快速滑动）
    private final int mMinFlingVelocity;
    // 第一阶段是否激活
    private boolean mInFirstStage = true;

    // 第一阶段最大滑动距离（缩放从1.0到0.8）
    private int mFirstStageMaxDistance;
    // 第二阶段最大滑动距离（屏幕宽度的50%）
    private int mSecondStageMaxDistance;

    private float mInitialX, mLastX;
    private float mLastScale = 1f;

    private static final float MIN_SCALE = 0.8f;

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
         *
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
        // 允许边缘滑动
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
    }

    /**
     * 设置抽屉监听器
     */
    public void setDrawerListener(DrawerListener listener) {
        mDrawerListener = listener;
    }

    //TODO 首次拖动缩放到0.8，松手后自动到右边
    public void setEnableSecondStage(boolean enable) {
//        mEnableSecondStage = enable;
//        // 如果关闭第二阶段且当前在第二阶段，强制回弹到第一阶段最大位置
//        if (!enable && mContentView != null && mContentView.getLeft() > mFirstStageMax) {
//            mDragHelper.settleCapturedViewAt(mFirstStageMax, 0);
//            invalidate();
//        }
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

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 确保只有一个子View
        if (getChildCount() != 1) {
            throw new IllegalArgumentException("ChatSwipeLayout must have exactly one child");
        }
        mContentView = getChildAt(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        mFirstStageMaxDistance = width / 2;
        mSecondStageMaxDistance = width / 2;
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
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mLastX = 0f;
            mInitialX = ev.getX();
        }
        Log.d(TAG, "onInterceptTouchEvent: action=" + ev.getAction() + ", x=" + mInitialX);
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    /**
     * 重写触摸事件处理，分离第一阶段逻辑
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            mLastX = event.getX();
            float deltaX = mLastX - mInitialX;
            // 未拖动且最小缩放状态时，往左滑动保持第一阶段，往右滑动进入第二阶段
            if (mContentView.getLeft() == 0 && mLastScale == MIN_SCALE) {
                mInFirstStage = (deltaX < 0);
            }
            Log.w(TAG, "onTouchEvent start deltaX=" + deltaX + " mLastScale=" + mLastScale + " dragLeft=" + mContentView.getLeft() + " mInFirstStage=" + mInFirstStage);
        }
        Log.d(TAG, "onTouchEvent: action=" + event.getAction() + ", eventX=" + mLastX);
        mDragHelper.processTouchEvent(event);
        return true;
    }

    private float getFirstStageScale() {
        float deltaX = mLastX - mInitialX;
        float slideRatio = Math.min(Math.abs(deltaX) / mFirstStageMaxDistance, 1);
        float scale;
        if (deltaX >= 0) {
            // 从左往右滑动
            scale = 1f - slideRatio * (1 - MIN_SCALE);
        } else if (mLastScale == MIN_SCALE) {
            // 从右往左滑动且已经到达最小缩放
            scale = MIN_SCALE + slideRatio * (1 - MIN_SCALE);
        } else {
            scale = 1;
        }

        Log.i(TAG, "getFirstStageScale deltaX=" + deltaX + " slideRatio=" + slideRatio + " scale=" + scale + " mLastScale=" + mLastScale + " mInFirstStage=" + mInFirstStage);
        return scale;
    }

    /**
     * ViewDragHelper回调实现：控制第二阶段滑动规则
     */
    private class DragCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child == mContentView;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            if (mInFirstStage) {
                // 第一阶段不允许拖动
                return 0;
            } else {
                return Math.max(0, Math.min(left, mSecondStageMaxDistance));
            }
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            if (mInFirstStage) {
                // 第一阶段不允许拖动
                return 0;
            } else {
                return YWDeviceUtil.getScreenWidth();
            }
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            float scale;

            if (mInFirstStage) {
                // 第一阶段：根据滑动缩放
                scale = getFirstStageScale();
                changedView.setScaleX(scale);
                changedView.setScaleY(scale);
                Log.i(TAG, "onViewPositionChanged mInFirstStage scale=" + scale);
            } else {
                // 第二阶段：不缩放，只平移
                float slideRatio = (float) left / mSecondStageMaxDistance;
                scale = MIN_SCALE;
                changedView.setScaleX(scale);
                changedView.setScaleY(scale);
                Log.i(TAG, "onViewPositionChanged mInSecondStage left= " + left + " dx= " + dx + " scale=" + scale + " slideRatio=" + slideRatio);
                if (mDrawerListener != null) {
                    mDrawerListener.onDrawerSlide(slideRatio);
                }
            }
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            int currentLeft = releasedChild.getLeft();
            int finalLeft;

            if (mInFirstStage) {
                updateDrawerState(STATE_SETTLING);
                float halfScale = (1 + MIN_SCALE) / 2;
                float scale = getFirstStageScale();
                Log.i(TAG, "onViewReleased mInFirstStage scale=" + scale + " xvel=" + xvel + " currentLeft=" + currentLeft);
                if (scale > halfScale) {
                    releasedChild.animate().scaleX(1).scaleY(1).start();
                    mLastScale = 1f;
                } else {
                    releasedChild.animate().scaleX(MIN_SCALE).scaleY(MIN_SCALE).start();
                    mLastScale = MIN_SCALE;
                }
            } else {
                mLastScale = MIN_SCALE;
                if (currentLeft > mSecondStageMaxDistance / 2 || xvel > mMinFlingVelocity) {
                    finalLeft = mSecondStageMaxDistance;
                } else {
                    finalLeft = 0;
                }
                Log.i(TAG, "onViewReleased currentLeft=" + currentLeft + " xvel=" + xvel + " finalLeft=" + finalLeft);
                mDragHelper.settleCapturedViewAt(finalLeft, 0);
                invalidate();
            }
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (state == ViewDragHelper.STATE_IDLE) {
                // 空闲状态时判断最终状态
                if (mContentView.getLeft() >= mSecondStageMaxDistance) {
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
}