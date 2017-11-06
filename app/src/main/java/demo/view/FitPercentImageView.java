package demo.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by fishyu on 2017/11/3.
 */

@SuppressLint("AppCompatCustomView")
public class FitPercentImageView extends ImageView {

    public FitPercentImageView(Context context) {
        super(context);
    }

    public FitPercentImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FitPercentImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
    }

    protected void scaleToPercent() {

    }

}
