package com.example.beyond.demo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

public class ZoomTranslateLayout extends ViewGroup {
    private static final float SCALE_FACTOR = 0.8f; // 缩小比例
    private static final float TRANSLATE_FACTOR = 0.5f; // 平移到屏幕的一半
    
    private View mContent; // 内容视图
    private float mLastX; // 上次触摸的X坐标
    private float mCurrentScale = 1.0f; // 当前缩放比例
    private float mCurrentTranslateX = 0f; // 当前X轴平移距离
    private boolean mIsInAction = false; // 是否正在进行操作
    private int mTouchSlop; // 触摸阈值
    private Scroller mScroller; // 用于平滑滚动
    private Matrix mMatrix = new Matrix(); // 用于变换
    
    public ZoomTranslateLayout(Context context) {
        this(context, null);
    }
    
    public ZoomTranslateLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public ZoomTranslateLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScroller = new Scroller(context);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            mContent = getChildAt(0);
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mContent != null) {
            measureChild(mContent, widthMeasureSpec, heightMeasureSpec);
        }
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mContent != null) {
            int width = r - l;
            int height = b - t;
            mContent.layout(0, 0, width, height);
        }
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - mLastX;
                // 如果水平移动超过阈值，则拦截事件
                if (Math.abs(dx) > mTouchSlop) {
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = event.getX();
                mIsInAction = true;
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            
            case MotionEvent.ACTION_MOVE:
                if (!mIsInAction) break;
                
                float dx = event.getX() - mLastX;
                mLastX = event.getX();
                
                // 计算总滑动距离的比例 (0-1)
                float totalScrollRange = getWidth() * TRANSLATE_FACTOR;
                float targetTranslateX = Math.max(0, Math.min(mCurrentTranslateX + dx, totalScrollRange));
                float progress = targetTranslateX / totalScrollRange;
                
                // 更新缩放比例 (0-0.5进度时从1缩放到0.8)
                if (progress <= 0.5f) {
                    mCurrentScale = 1.0f - (1.0f - SCALE_FACTOR) * (progress / 0.5f);
                } else {
                    mCurrentScale = SCALE_FACTOR;
                }
                
                // 更新平移距离
                mCurrentTranslateX = targetTranslateX;
                
                // 应用变换
                applyTransform();
                break;
            
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mIsInAction) break;
                mIsInAction = false;
                
                // 判断应该恢复到哪个状态
                float totalScroll = getWidth() * TRANSLATE_FACTOR;
                if (mCurrentTranslateX > totalScroll * 0.5f) {
                    // 吸附到展开状态
                    smoothTo(SCALE_FACTOR, totalScroll);
                } else {
                    // 吸附到初始状态
                    smoothTo(1.0f, 0f);
                }
                break;
        }
        return true;
    }
    
    // 应用缩放和平移变换
    private void applyTransform() {
        if (mContent == null) return;
        
        mMatrix.reset();
        
        // 计算缩放后的中心点
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        // 先缩放
        mMatrix.postScale(mCurrentScale, mCurrentScale, centerX, centerY);
        
        // 再平移
        mMatrix.postTranslate(mCurrentTranslateX, 0);
        
        // 应用变换
        mContent.setPivotX(centerX);
        mContent.setPivotY(centerY);
        mContent.setScaleX(mCurrentScale);
        mContent.setScaleY(mCurrentScale);
        mContent.setTranslationX(mCurrentTranslateX);
    }
    
    // 平滑过渡到指定状态
    private void smoothTo(float targetScale, float targetTranslateX) {
        int duration = 300; // 动画持续时间
        
        float scaleDiff = targetScale - mCurrentScale;
        float translateDiff = targetTranslateX - mCurrentTranslateX;
        
        // 设置滚动参数
        mScroller.startScroll(
                0, 0, 
                (int) translateDiff, 
                0, 
                duration
        );
        
        // 使用ValueAnimator实现平滑缩放
        final long startTime = System.currentTimeMillis();
        post(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                float elapsedTime = Math.min((currentTime - startTime) / (float) duration, 1.0f);
                
                // 使用缓动函数使动画更自然
                float t = easeOutQuad(elapsedTime);
                
                mCurrentScale = mCurrentScale + scaleDiff * t;
                mCurrentTranslateX = mCurrentTranslateX + translateDiff * t;
                
                applyTransform();
                
                if (elapsedTime < 1.0f) {
                    post(this);
                }
            }
        });
    }
    
    // 缓动函数 - 二次方缓出
    private float easeOutQuad(float t) {
        return t * (2 - t);
    }
    
    // 重置到初始状态
    public void reset() {
        smoothTo(1.0f, 0f);
    }
    
    // 展开到最终状态
    public void expand() {
        smoothTo(SCALE_FACTOR, getWidth() * TRANSLATE_FACTOR);
    }
}