package com.example.beyond.demo.ui.swipe.view.test;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.customview.widget.ViewDragHelper;

public class SlideScaleLayout extends FrameLayout {
    private ViewDragHelper mDragHelper;
    private View mContentView;
    private float mStartX, mStartY;
    private int mTouchSlop;

    public SlideScaleLayout(Context context) {
        super(context);
        init();
    }

    public SlideScaleLayout(Context context, AttributeSet attrs) {
        super(context);
        init();
    }

    private void init() {
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mDragHelper = ViewDragHelper.create(this, 1.0f, new DragCallback());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            mContentView = getChildAt(0);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = ev.getX();
                mStartY = ev.getY();
                getParent().requestDisallowInterceptTouchEvent(true); // 防止父容器拦截
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - mStartX);
                if (dx > mTouchSlop) {
                    return true; // 关键：拦截事件
                }
                break;
        }
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event); // 关键：处理事件
        return true; // 关键：消费事件
    }

    private class DragCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mContentView; // 关键：捕获目标View
        }
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return left; // 简单的允许水平拖动
        }
    }
}