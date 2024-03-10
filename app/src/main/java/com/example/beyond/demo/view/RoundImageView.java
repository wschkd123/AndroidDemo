package com.example.beyond.demo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.example.beyond.demo.R;
import com.example.beyond.demo.util.ColorDrawableUtils;

public class RoundImageView extends AppCompatImageView {

    private static final String TAG = "RoundImageView";

    public static final int ROUND_TYPE_LEFT_TOP = 1;//左上角
    public static final int ROUND_TYPE_RIGHT_TOP = 1 << 1;//右上角
    public static final int ROUND_TYPE_LEFT_BOTTOM = 1 << 2;//左下角
    public static final int ROUND_TYPE_RIGHT_BOTTOM = 1 << 3;//右下角
    public static final int ROUND_TYPE_DEFAULT =
            ROUND_TYPE_LEFT_TOP | ROUND_TYPE_RIGHT_TOP | ROUND_TYPE_LEFT_BOTTOM
                    | ROUND_TYPE_RIGHT_BOTTOM;

    private int mType = ROUND_TYPE_DEFAULT;

    private final RectF mViewRect = new RectF();
    private final RectF mBitmapRect = new RectF();
    private final RectF borderRect = new RectF();

    private final Matrix mShaderMatrix = new Matrix();
    private final Paint mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap mBitmap;
    private BitmapShader mBitmapShader;

    private ColorFilter mColorFilter;

    private ScaleType scaleType;
    private int borderColor = 0;
    private float borderWidth = 0;
    private float mRadius = 0;
    private float mWidthHeightRatio = 0;

    private Path mPath = new Path();
    private Path borderPath = new Path();
    private float mRadiusLeftTop;
    private float mRadiusLeftBottom;
    private float mRadiusRightTop;
    private float mRadiusRightBottom;

    public RoundImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public RoundImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public RoundImageView(Context context) {
        super(context);
        init(context, null, 0);
    }

    protected void init(Context context, AttributeSet attrs, int defStyle) {
        if (attrs != null) {
            TypedArray a = context
                    .obtainStyledAttributes(attrs, R.styleable.RoundImageView, defStyle, 0);
            mRadius = a.getDimension(R.styleable.RoundImageView_round_width, 0);
            mWidthHeightRatio = a.getFloat(R.styleable.RoundImageView_width_height_ratio, 0);
            mRadiusLeftTop = a.getDimension(R.styleable.RoundImageView_round_left_top, 0);
            mRadiusLeftBottom = a.getDimension(R.styleable.RoundImageView_round_left_bottom, 0);
            mRadiusRightTop = a.getDimension(R.styleable.RoundImageView_round_right_top, 0);
            mRadiusRightBottom = a.getDimension(R.styleable.RoundImageView_round_right_bottom, 0);
            borderColor = a.getColor(R.styleable.RoundImageView_border_color, borderColor);
            borderWidth = a.getDimension(R.styleable.RoundImageView_border_width, borderWidth);
        }
        if (scaleType == null) {
            scaleType = ScaleType.FIT_CENTER;
        }
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        this.scaleType = scaleType;
    }

    @Override
    public ScaleType getScaleType() {
        return scaleType;
    }

    /**
     * 设置圆角类型 可单独定制哪个角做圆角 默认4个角都是圆角
     *
     * @param type
     */
    @Deprecated
    public void setType(int type) {
        mType = type;
    }

    /**
     * 设置圆角大小
     *
     * @param radius
     */
    public void setRadius(float radius) {
        mRadius = radius;
    }

    /**
     * 设置宽高比
     *
     * @param widthHeightRatio
     */
    public void setWidthHeightRatio(float widthHeightRatio) {
        mWidthHeightRatio = widthHeightRatio;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public void setBorderWidth(float borderWidth) {
        this.borderWidth = borderWidth;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mWidthHeightRatio != 0) {
            int height = (int) (getMeasuredWidth() / mWidthHeightRatio);
            setMeasuredDimension(getMeasuredWidth(), height);
        }
    }

    @Override
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (adjustViewBounds) {
            throw new IllegalArgumentException("adjustViewBounds not supported.");
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap == null) {
            return;
        }

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(borderWidth * 2);
        if (mRadiusLeftTop + mRadiusRightTop + mRadiusLeftBottom + mRadiusRightBottom > 0) {
            canvas.drawPath(mPath, mBitmapPaint);
            canvas.drawPath(borderPath, borderPaint);
        } else {
            canvas.drawRoundRect(mBitmapRect, mRadius, mRadius, mBitmapPaint);
            canvas.drawRoundRect(borderRect, mRadius, mRadius, borderPaint);
        }
    }

    private void genPath() {
        mPath.reset();
        mPath.moveTo(0, mRadiusLeftTop);
        mPath.arcTo(new RectF(0, 0, 2 * mRadiusLeftTop, 2 * mRadiusLeftTop), 180, 90);
        mPath.lineTo(mViewRect.width() - mRadiusRightTop, 0);
        mPath.arcTo(new RectF(mViewRect.width() - mRadiusRightTop * 2, 0, mViewRect.width(),
                mRadiusRightTop * 2), 270, 90);
        mPath.lineTo(mViewRect.width(), mViewRect.height() - mRadiusRightBottom);
        mPath.arcTo(new RectF(mViewRect.width() - mRadiusRightBottom * 2,
                        mViewRect.height() - mRadiusRightBottom * 2, mViewRect.width(),
                        mViewRect.height()),
                0, 90);
        mPath.lineTo(mRadiusLeftBottom, mViewRect.height());
        mPath.arcTo(new RectF(0, mViewRect.height() - mRadiusLeftBottom * 2, mRadiusLeftBottom * 2,
                mViewRect.height()), 90, 90);
        mPath.close();

        final float inset = borderWidth / 2;
        borderPath.reset();
        borderPath.moveTo(inset, mRadiusLeftTop);
        borderPath.arcTo(new RectF(inset, inset, 2 * mRadiusLeftTop - inset,
                2 * mRadiusLeftTop - inset), 180, 90);
        borderPath.lineTo(mViewRect.width() - mRadiusRightTop, inset);
        borderPath.arcTo(new RectF(mViewRect.width() - mRadiusRightTop * 2 + inset, inset,
                mViewRect.width() - inset, mRadiusRightTop * 2 - inset), 270, 90);
        borderPath.lineTo(mViewRect.width() - inset, mViewRect.height() - mRadiusRightBottom);
        borderPath.arcTo(new RectF(mViewRect.width() - mRadiusRightBottom * 2 + inset,
                mViewRect.height() - mRadiusRightBottom * 2 + inset, mViewRect.width() - inset,
                mViewRect.height() - inset), 0, 90);
        borderPath.lineTo(mRadiusLeftBottom, mViewRect.height() - inset);
        borderPath.arcTo(new RectF(inset, mViewRect.height() - mRadiusLeftBottom * 2 + inset,
                mRadiusLeftBottom * 2 - inset, mViewRect.height() - inset), 90, 90);
        borderPath.close();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setup();
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        setup();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        initializeBitmap();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        super.setImageResource(resId);
        initializeBitmap();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        initializeBitmap();
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable dr) {
        super.invalidateDrawable(dr);
        initializeBitmap();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (cf == mColorFilter) {
            return;
        }

        mColorFilter = cf;
        applyColorFilter();
        invalidate();
    }

    @Override
    public ColorFilter getColorFilter() {
        return mColorFilter;
    }

    private void applyColorFilter() {
        if (mBitmapPaint != null) {
            mBitmapPaint.setColorFilter(mColorFilter);
        }
    }

    private void initializeBitmap() {
        mBitmap = ColorDrawableUtils.getBitmapFromDrawable(getDrawable());
        setup();
    }

    private void setup() {
        if (getWidth() == 0 && getHeight() == 0) {
            return;
        }

        if (mBitmap == null) {
            invalidate();
            return;
        }

        mBitmapShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setColor(Color.RED);
        mBitmapPaint.setShader(mBitmapShader);

        mBitmapRect.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());

        mViewRect.set(calculateBounds());

        applyColorFilter();
        updateShaderMatrix();
        invalidate();
        if (mRadiusLeftTop + mRadiusRightTop + mRadiusLeftBottom + mRadiusRightBottom > 0) {
            genPath();
        }
    }

    private RectF calculateBounds() {
        int availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        float left = getPaddingLeft();
        float top = getPaddingTop();

        return new RectF(left, top, left + availableWidth, top + availableHeight);
    }

    private void updateShaderMatrix() {
        float scaleX = 1, scaleY = 1;
        float offsetX = 0, offsetY = 0;

        mShaderMatrix.set(null);

        switch (scaleType) {
            case CENTER: {
                offsetX = (mViewRect.width() - mBitmapRect.width() * scaleX) * 0.5f;
                offsetY = (mViewRect.height() - mBitmapRect.height() * scaleY) * 0.5f;
                break;
            }
            case FIT_XY: {
                scaleX = mViewRect.width() / mBitmapRect.width();
                scaleY = mViewRect.height() / mBitmapRect.height();
                break;
            }
            case FIT_START: {
                scaleX = mViewRect.width() / mBitmapRect.width();
                scaleY = mViewRect.height() / mBitmapRect.height();
                if (scaleX < scaleY) {
                    scaleY = scaleX;
                } else {
                    scaleX = scaleY;
                }
                break;
            }
            case FIT_CENTER: {
                scaleX = mViewRect.width() / mBitmapRect.width();
                scaleY = mViewRect.height() / mBitmapRect.height();
                if (scaleX < scaleY) {
                    scaleY = scaleX;
                    offsetY = (mViewRect.height() - mBitmapRect.height() * scaleY) * 0.5f;
                } else {
                    scaleX = scaleY;
                    offsetX = (mViewRect.width() - mBitmapRect.width() * scaleX) * 0.5f;
                }
                break;
            }
            case FIT_END: {
                scaleX = mViewRect.width() / mBitmapRect.width();
                scaleY = mViewRect.height() / mBitmapRect.height();
                if (scaleX < scaleY) {
                    scaleY = scaleX;
                    offsetY = mViewRect.height() - mBitmapRect.height() * scaleY;
                } else {
                    scaleX = scaleY;
                    offsetX = mViewRect.width() - mBitmapRect.width() * scaleX;
                }
                break;
            }
            case CENTER_CROP: {
                scaleX = mViewRect.width() / mBitmapRect.width();
                scaleY = mViewRect.height() / mBitmapRect.height();
                if (scaleX < scaleY) {
                    scaleX = scaleY;
                    offsetX = (mViewRect.width() - mBitmapRect.width() * scaleX) * 0.5f;
                } else {
                    scaleY = scaleX;
                    offsetY = (mViewRect.height() - mBitmapRect.height() * scaleY) * 0.5f;
                }
                break;
            }
            case CENTER_INSIDE: {
                scaleX = mViewRect.width() / mBitmapRect.width();
                scaleY = mViewRect.height() / mBitmapRect.height();
                if (scaleX > 1 && scaleY > 1) {
                    scaleX = scaleY = 1;
                    offsetX = (mViewRect.width() - mBitmapRect.width() * scaleX) * 0.5f;
                    offsetY = (mViewRect.height() - mBitmapRect.height() * scaleY) * 0.5f;
                } else {
                    if (scaleX < scaleY) {
                        scaleY = scaleX;
                        offsetY = (mViewRect.height() - mBitmapRect.height() * scaleY) * 0.5f;
                    } else {
                        scaleX = scaleY;
                        offsetX = (mViewRect.width() - mBitmapRect.width() * scaleX) * 0.5f;
                    }
                }
                break;
            }
            default: {
                break;
            }
        }

        mShaderMatrix.setScale(scaleX, scaleY);
        mShaderMatrix.postTranslate(mViewRect.left + offsetX, mViewRect.top + offsetY);

        mBitmapShader.setLocalMatrix(mShaderMatrix);

        mBitmapRect.set(offsetX, offsetY, offsetX + mBitmapRect.width() * scaleX,
                offsetY + mBitmapRect.height() * scaleY);
        mBitmapRect.offset(mViewRect.left, mViewRect.top);
        mBitmapRect.intersect(mViewRect);

        borderRect.set(mBitmapRect);
        borderRect.inset(borderWidth / 2, borderWidth / 2);
    }

}
