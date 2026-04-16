package com.example.beyond.demo.ui.swipe.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;

import com.example.base.util.YWDeviceUtil;

public class SlideScaleLayout extends FrameLayout {
    private static final float MIN_SCALE = 0.8f;
    private static final int ANIM_DURATION = 300;
    private static final int TRIGGER_AREA_WIDTH = 300; // 触发区域宽度
    private final int MOVE_DISTANCE = YWDeviceUtil.getScreenWidth() / 2; // 平移距离
    private static final float MIN_VELOCITY_DP = 800; // 最小速度阈值
    
    private ViewDragHelper mDragHelper;
    private View mContentView;
    private float mDensity;
    private int mTouchSlop;
    
    // 状态管理
    private enum State {
        FULL,           // 全屏状态
        SCALED,         // 缩小状态(0.8倍)
        SCALED_MOVED    // 缩小并平移状态
    }
    
    private State mCurrentState = State.FULL;
    private float mScale = 1.0f;
    private float mTranslationX = 0;
    private float mStartX, mStartY;
    private boolean mIsInTriggerArea = false;

    public SlideScaleLayout(Context context) {
        this(context, null);
    }

    public SlideScaleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideScaleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mDensity = getResources().getDisplayMetrics().density;
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        
        mDragHelper = ViewDragHelper.create(this, 1.0f, new DragCallback());
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
        
        int minVelocityPx = (int) (MIN_VELOCITY_DP * mDensity);
        mDragHelper.setMinVelocity(minVelocityPx);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 1) {
            throw new IllegalStateException("SlideScaleLayout must have exactly one child");
        }
        mContentView = getChildAt(0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mContentView.layout(0, 0, getWidth(), getHeight());
    }

    // ==================== 触摸事件处理 ====================
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        float x = ev.getX();
        float y = ev.getY();
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mStartX = x;
                mStartY = y;
                mIsInTriggerArea = (x < TRIGGER_AREA_WIDTH);
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (!mIsInTriggerArea) {
                    return false;
                }
                
                float dx = Math.abs(x - mStartX);
                float dy = Math.abs(y - mStartY);
                
                if (dx > mTouchSlop || dy > mTouchSlop) {
                    if (dx > dy) {
                        return true;
                    }
                }
                break;
        }
        
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mStartX = event.getX();
                mStartY = event.getY();
                mIsInTriggerArea = (mStartX < TRIGGER_AREA_WIDTH);
                break;
                
            case MotionEvent.ACTION_UP:
                if (mIsInTriggerArea) {
                    float dx = Math.abs(event.getX() - mStartX);
                    float dy = Math.abs(event.getY() - mStartY);
                    
                    if (dx < mTouchSlop && dy < mTouchSlop) {
                        handleTapEvent();
                        return true;
                    }
                }
                break;
        }
        
        if (mIsInTriggerArea || mCurrentState == State.SCALED_MOVED) {
            mDragHelper.processTouchEvent(event);
            return true;
        }
        
        return false;
    }

    // ==================== 点击事件处理 ====================
    private void handleTapEvent() {
        if (mCurrentState == State.SCALED) {
            animateToFullState();
        }
    }

    // ==================== ViewDragHelper回调 ====================
    private class DragCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mContentView;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return 1;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            switch (mCurrentState) {
                case SCALED_MOVED:
                    return Math.max(0, Math.min(left, MOVE_DISTANCE));
                default:
                    return 0;
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (mCurrentState == State.SCALED_MOVED) {
                mTranslationX = left;
                applyScaleAndPosition();
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            switch (mCurrentState) {
                case FULL:
                    if (xvel > 0 || releasedChild.getLeft() > TRIGGER_AREA_WIDTH / 2) {
                        animateToScaledState();
                    } else {
                        mDragHelper.settleCapturedViewAt(0, 0);
                    }
                    break;
                    
                case SCALED:
                    if (xvel > 0 || releasedChild.getLeft() > TRIGGER_AREA_WIDTH / 2) {
                        animateToMovedState();
                    } else {
                        animateToFullState();
                    }
                    break;
                    
                case SCALED_MOVED:
                    if (xvel < 0 || mTranslationX < MOVE_DISTANCE / 2) {
                        animateToScaledState();
                    } else {
                        mDragHelper.settleCapturedViewAt(MOVE_DISTANCE, 0);
                    }
                    break;
            }
            invalidate();
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            if (edgeFlags == ViewDragHelper.EDGE_LEFT) {
                if (mCurrentState == State.FULL) {
                    animateToScaledState();
                } else if (mCurrentState == State.SCALED) {
                    mDragHelper.captureChildView(mContentView, pointerId);
                    animateToMovedState();
                }
            }
        }
    }

    // ==================== 动画方法 ====================
    private void animateToScaledState() {
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(mScale, MIN_SCALE);
        scaleAnimator.addUpdateListener(animation -> {
            mScale = (float) animation.getAnimatedValue();
            applyScaleAndPosition();
        });
        
        scaleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentState = State.SCALED;
                mTranslationX = 0;
            }
        });
        
        scaleAnimator.setDuration(ANIM_DURATION);
        scaleAnimator.start();
    }

    private void animateToMovedState() {
        ValueAnimator moveAnimator = ValueAnimator.ofFloat(mTranslationX, MOVE_DISTANCE);
        moveAnimator.addUpdateListener(animation -> {
            mTranslationX = (float) animation.getAnimatedValue();
            applyScaleAndPosition();
        });
        
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentState = State.SCALED_MOVED;
            }
        });
        
        moveAnimator.setDuration(ANIM_DURATION);
        moveAnimator.start();
    }

    private void animateToFullState() {
        ValueAnimator moveAnimator = ValueAnimator.ofFloat(mTranslationX, 0);
        moveAnimator.addUpdateListener(animation -> {
            mTranslationX = (float) animation.getAnimatedValue();
            applyScaleAndPosition();
        });
        
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(mScale, 1.0f);
        scaleAnimator.addUpdateListener(animation -> {
            mScale = (float) animation.getAnimatedValue();
            applyScaleAndPosition();
        });
        
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(moveAnimator, scaleAnimator);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentState = State.FULL;
                mScale = 1.0f;
                mTranslationX = 0;
            }
        });
        
        set.setDuration(ANIM_DURATION);
        set.start();
    }

    // ==================== 应用变换 ====================
    private void applyScaleAndPosition() {
        mContentView.setScaleX(mScale);
        mContentView.setScaleY(mScale);
        
        float scaledWidth = getWidth() * mScale;
        float scaledHeight = getHeight() * mScale;
        float offsetX = (getWidth() - scaledWidth) / 2;
        float offsetY = (getHeight() - scaledHeight) / 2;
        
        mContentView.setTranslationX(offsetX + mTranslationX);
        mContentView.setTranslationY(offsetY);
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    // ==================== 公共方法 ====================
    public boolean isInFullState() {
        return mCurrentState == State.FULL;
    }
    
    public boolean isInScaledState() {
        return mCurrentState == State.SCALED;
    }
    
    public boolean isInMovedState() {
        return mCurrentState == State.SCALED_MOVED;
    }
    
    public void resetToFullState() {
        animateToFullState();
    }
}