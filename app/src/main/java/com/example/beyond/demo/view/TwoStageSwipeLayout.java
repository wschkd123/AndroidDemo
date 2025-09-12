package com.example.beyond.demo.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 两阶段滑动布局：支持缩放→平移的两阶段滑动效果
 */
public class TwoStageSwipeLayout extends ViewGroup {
    // 状态常量
    public static final int STATE_IDLE = 0;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;

    // 阶段常量
    private static final int STAGE_SCALE = 0;  // 缩放阶段
    private static final int STAGE_TRANSLATE = 1;  // 平移阶段

    // 缩放参数
    private static final float SCALE_MIN = 0.8f;
    private static final float SCALE_MAX = 1.0f;

    // 平移参数（屏幕宽度的比例）
    private static final float TRANSLATE_MAX_RATIO = 0.5f;

    private final ViewDragHelper mDragHelper;
    private View mContentView;  // 唯一的内容视图
    private int mScreenWidth;
    private int mCurrentStage = STAGE_SCALE;  // 当前阶段
    private float mScale = SCALE_MAX;  // 当前缩放值
    private float mTranslateX = 0;  // 当前X轴平移量
    private int mDrawerState = STATE_IDLE;
    private final int mMinFlingVelocity;

    // 监听器相关
    private final List<DrawerListener> mListeners = new ArrayList<>();
    private Rect mChildHitRect;
    private Matrix mChildInvertedMatrix;
    private Object mLastInsets;
    private boolean mFirstLayout = true;
    private static final boolean SET_DRAWER_SHADOW_FROM_ELEVATION = true;
    private float mDrawerElevation = 8f;
    private static final int mMinDrawerMargin = 0;

    // 接口定义（模仿DrawerListener）
    public interface DrawerListener {
        void onDrawerSlide(@NonNull View drawerView, float slideOffset);
        void onDrawerOpened(@NonNull View drawerView);
        void onDrawerClosed(@NonNull View drawerView);
        void onDrawerStateChanged(int newState);
    }

    public TwoStageSwipeLayout(Context context) {
        this(context, null);
    }

    public TwoStageSwipeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TwoStageSwipeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ViewConfiguration config = ViewConfiguration.get(context);
        mMinFlingVelocity = config.getScaledMinimumFlingVelocity();

        // 初始化ViewDragHelper
        mDragHelper = ViewDragHelper.create(this, 1.0f, new DragCallback());
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT | ViewDragHelper.EDGE_RIGHT);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 确保只有一个子视图
        if (getChildCount() != 1) {
            throw new IllegalArgumentException("TwoStageSwipeLayout must have exactly one child");
        }
        mContentView = getChildAt(0);
        mContentView.setClickable(true);  // 确保能接收触摸事件
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // 强制精确测量模式
        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalArgumentException("TwoStageSwipeLayout must be measured with EXACTLY");
        }

        setMeasuredDimension(widthSize, heightSize);
        mScreenWidth = widthSize;

        // 测量子视图（全屏）
        LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
        int childWidthSpec = MeasureSpec.makeMeasureSpec(
                widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(
                heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
        mContentView.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mFirstLayout) {
            mFirstLayout = false;
            resetContentPosition();
        }
        LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
        mContentView.layout(
                lp.leftMargin,
                lp.topMargin,
                lp.leftMargin + mContentView.getMeasuredWidth(),
                lp.topMargin + mContentView.getMeasuredHeight()
        );
    }

    /**
     * 重置内容视图位置和状态
     */
    private void resetContentPosition() {
        mCurrentStage = STAGE_SCALE;
        mScale = SCALE_MAX;
        mTranslateX = 0;
        applyContentTransform();
    }

    /**
     * 应用缩放和平移变换
     */
    private void applyContentTransform() {
        mContentView.setPivotX(mContentView.getWidth() / 2.0f);
        mContentView.setPivotY(mContentView.getHeight() / 2.0f);
        mContentView.setScaleX(mScale);
        mContentView.setScaleY(mScale);
        mContentView.setTranslationX(mTranslateX);
        dispatchOnDrawerSlide(mContentView, getSlideOffset());
    }

    /**
     * 计算滑动偏移比例（0-1）
     */
    private float getSlideOffset() {
        if (mCurrentStage == STAGE_SCALE) {
            return (SCALE_MAX - mScale) / (SCALE_MAX - SCALE_MIN);
        } else {
            return 1f + (mTranslateX / (mScreenWidth * TRANSLATE_MAX_RATIO));
        }
    }

    /**
     * ViewDragHelper回调实现两阶段滑动逻辑
     */
    private class DragCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child == mContentView;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            // 根据当前阶段限制滑动范围
            if (mCurrentStage == STAGE_SCALE) {
                // 缩放阶段：限制在初始位置（通过dx计算缩放）
                return 0;
            } else {
                // 平移阶段：限制在0到屏幕一半之间
                return Math.max(0, Math.min(left, (int) (mScreenWidth * TRANSLATE_MAX_RATIO)));
            }
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            return mScreenWidth;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);

            if (mCurrentStage == STAGE_SCALE) {
                // 阶段1：根据水平滑动距离计算缩放（右滑dx为正，左滑dx为负）
                float scaleDelta = dx / (float) mScreenWidth * 2;  // 缩放敏感度
                mScale = Math.max(SCALE_MIN, Math.min(SCALE_MAX, mScale - scaleDelta));
                applyContentTransform();
            } else {
                // 阶段2：直接应用平移
                mTranslateX = left;
                applyContentTransform();
            }
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);

            if (mCurrentStage == STAGE_SCALE) {
                // 缩放阶段结束：判断是否进入平移阶段
                if (mScale <= SCALE_MIN || xvel > mMinFlingVelocity) {
                    // 进入平移阶段
                    mCurrentStage = STAGE_TRANSLATE;
                    mDragHelper.settleCapturedViewAt(0, 0);  // 重置位置，准备平移
                } else {
                    // 回弹到初始状态
                    mScale = SCALE_MAX;
                    applyContentTransform();
                }
            } else {
                // 平移阶段结束：判断是否复位
                float maxTranslate = mScreenWidth * TRANSLATE_MAX_RATIO;
                if (mTranslateX > maxTranslate / 2 || xvel > mMinFlingVelocity) {
                    // 停留在最大平移位置
                    mDragHelper.settleCapturedViewAt((int) maxTranslate, 0);
                } else {
                    // 回弹到缩放阶段
                    mCurrentStage = STAGE_SCALE;
                    mScale = SCALE_MIN;
                    mDragHelper.settleCapturedViewAt(0, 0);
                }
            }
            invalidate();
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            updateDrawerState(state);
        }
    }

    /**
     * 更新抽屉状态并通知监听器
     */
    private void updateDrawerState(int newState) {
        if (mDrawerState == newState) return;
        mDrawerState = newState;

        if (newState == STATE_IDLE) {
            if (mCurrentStage == STAGE_TRANSLATE && mTranslateX > 0) {
                dispatchOnDrawerOpened(mContentView);
            } else {
                dispatchOnDrawerClosed(mContentView);
            }
        }

        if (mListeners != null) {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                mListeners.get(i).onDrawerStateChanged(newState);
            }
        }
    }

    // 监听器分发方法
    private void dispatchOnDrawerSlide(View drawerView, float slideOffset) {
        if (mListeners != null) {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                mListeners.get(i).onDrawerSlide(drawerView, slideOffset);
            }
        }
    }

    private void dispatchOnDrawerOpened(View drawerView) {
        if (mListeners != null) {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                mListeners.get(i).onDrawerOpened(drawerView);
            }
        }
    }

    private void dispatchOnDrawerClosed(View drawerView) {
        if (mListeners != null) {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                mListeners.get(i).onDrawerClosed(drawerView);
            }
        }
    }

    // 对外接口
    public void addDrawerListener(@NonNull DrawerListener listener) {
        mListeners.add(listener);
    }

    public void removeDrawerListener(@NonNull DrawerListener listener) {
        mListeners.remove(listener);
    }

    // 触摸事件处理
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
        if (mDragHelper.continueSettling(true)) {
            if (mCurrentStage == STAGE_TRANSLATE) {
                mTranslateX = mContentView.getLeft();
            }
            applyContentTransform();
            invalidate();
        }
    }

    // 布局参数
    public static class LayoutParams extends MarginLayoutParams {
        public int gravity = Gravity.NO_GRAVITY;
        public int openState = 0;
        public static final int FLAG_IS_OPENED = 1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p.width, p.height);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    // 辅助方法（从原DrawerLayout复用）
    private boolean isInBoundsOfChild(float x, float y, View child) {
        if (mChildHitRect == null) {
            mChildHitRect = new Rect();
        }
        child.getHitRect(mChildHitRect);
        return mChildHitRect.contains((int) x, (int) y);
    }

    private boolean dispatchTransformedGenericPointerEvent(MotionEvent event, View child) {
        boolean handled;
        final android.graphics.Matrix childMatrix = child.getMatrix();
        if (!childMatrix.isIdentity()) {
            MotionEvent transformedEvent = getTransformedMotionEvent(event, child);
            handled = child.dispatchGenericMotionEvent(transformedEvent);
            transformedEvent.recycle();
        } else {
            final float offsetX = getScrollX() - child.getLeft();
            final float offsetY = getScrollY() - child.getTop();
            event.offsetLocation(offsetX, offsetY);
            handled = child.dispatchGenericMotionEvent(event);
            event.offsetLocation(-offsetX, -offsetY);
        }
        return handled;
    }

    private MotionEvent getTransformedMotionEvent(MotionEvent event, View child) {
        final float offsetX = getScrollX() - child.getLeft();
        final float offsetY = getScrollY() - child.getTop();
        final MotionEvent transformedEvent = MotionEvent.obtain(event);
        transformedEvent.offsetLocation(offsetX, offsetY);
        final android.graphics.Matrix childMatrix = child.getMatrix();
        if (!childMatrix.isIdentity()) {
            if (mChildInvertedMatrix == null) {
                mChildInvertedMatrix = new android.graphics.Matrix();
            }
            childMatrix.invert(mChildInvertedMatrix);
            transformedEvent.transform(mChildInvertedMatrix);
        }
        return transformedEvent;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 21) {
            mLastInsets = insets;
            return super.onApplyWindowInsets(insets);
        }
        return insets;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
    }
}