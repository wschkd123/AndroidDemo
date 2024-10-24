package com.example.beyond.demo.view;

import android.animation.FloatEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.beyond.demo.R;

import java.util.Random;

public class CloudMusicLoadingView extends View implements Runnable {
    /**
     * 随机数
     */
    private static Random mRandom = new Random();

    /**
     * 默认4条音轨
     */
    private static final int DEFAULT_RAIL_COUNT = 4;

    /**
     * 控件宽
     */
    private int mViewWidth;
    /**
     * 控件高
     */
    private int mViewHeight;
    /**
     * 画笔
     */
    private Paint mPaint;
    /**
     * 音轨数量
     */
    private int mRailCount;
    /**
     * 音轨颜色
     */
    private int mRailColor;
    /**
     * 每条音轨的线宽
     */
    private float mRailLineWidth;
    /**
     * Float类型估值器，用于在指定数值区域内进行估值
     */
    private FloatEvaluator mFloatEvaluator;

    public CloudMusicLoadingView(Context context) {
        this(context, null);
    }

    public CloudMusicLoadingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CloudMusicLoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        initAttr(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setColor(mRailColor);
        mPaint.setStrokeWidth(mRailLineWidth);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAntiAlias(true);
        mFloatEvaluator = new FloatEvaluator();
    }

    private void initAttr(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CloudMusicLoadingView, defStyleAttr, 0);
        mRailCount = array.getInt(R.styleable.CloudMusicLoadingView_cmlv_rail_count, DEFAULT_RAIL_COUNT);
        mRailColor = array.getColor(R.styleable.CloudMusicLoadingView_cmlv_rail_color, Color.argb(255, 255, 255, 255));
        mRailLineWidth = array.getDimension(R.styleable.CloudMusicLoadingView_cmlv_line_width, dip2px(context, 1f));
        array.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            mViewWidth = w;
            mViewHeight = h;
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //计算可用高度
        float totalAvailableHeight = mViewHeight - getPaddingBottom() - getPaddingTop();
        //计算每条音轨平分宽度后的位置
        float averageBound = (mViewWidth - getPaddingStart() - getPaddingEnd() * 1.0f) / mRailCount;
        //计算每条音轨的x坐标位置
        float x = averageBound - mRailLineWidth / 2f;
        float y = getPaddingBottom();

        //保存画布
        canvas.save();
        for (int i = 1; i <= mRailCount; i++) {
            //估值x坐标
            float fraction = mRandom.nextFloat();
            float evaluateY = (mFloatEvaluator.evaluate(fraction, 0.5f, 0.9f)) * totalAvailableHeight;
            // 需要移动到垂直居中的位置，计算偏移
            float offset = (totalAvailableHeight - (evaluateY - y))/2f;
            //第一个不需要偏移
            if (i == 1) {
                canvas.drawLine(x, y+offset, x, evaluateY+offset, mPaint);
            } else {
                //后续，每个音轨都固定偏移间距后，再画
                canvas.translate(averageBound, 0);
                canvas.drawLine(x, y+offset, x, evaluateY+offset, mPaint);
            }
        }
        //恢复画布
        canvas.restore();

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    @Override
    public void run() {
        invalidate();
        postDelayed(this, 100);
    }

    public void start() {
        postDelayed(this, 700);
    }

    public void stop() {
        removeCallbacks(this);
    }

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

}
