package demo.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by fishyu on 2017/11/3.
 */

@SuppressLint("AppCompatCustomView")
public class FitPercentImageView extends ImageView {

    private static final boolean DEBUG = false;


    private static final String TAG = FitPercentImageView.class.getSimpleName();

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
        if (DEBUG) Log.v(TAG, "onMeasure");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getDrawable() != null && getDrawable().getIntrinsicWidth() != 0 && getDrawable().getIntrinsicWidth() != 0) {
            final int width = getLayoutParams().width;
            final int height = getLayoutParams().height;
            if (DEBUG)
                Log.v(TAG, " width -> " + width + " height -> " + height + " drawableWidth -> " + (getDrawable() != null ? getDrawable().getIntrinsicWidth() : 0) + " drawableHeight -> " + (getDrawable() != null ? getDrawable().getIntrinsicHeight() : 0));
            if (height == ViewGroup.LayoutParams.WRAP_CONTENT && width != ViewGroup.LayoutParams.WRAP_CONTENT && width != ViewGroup.LayoutParams.MATCH_PARENT && width > 0) {
                if (DEBUG) Log.v(TAG, "\t using width for tag ");
                float factor = getDrawable().getIntrinsicHeight() / getDrawable().getIntrinsicWidth();
                int fixHeightValue = (int) (width * factor);
                fixHeightValue = resolveAdjustedSize(fixHeightValue, Integer.MAX_VALUE, heightMeasureSpec);
                if (DEBUG) Log.v(TAG, "\t max value for height -> " + fixHeightValue);
                if (getMeasuredHeight() != fixHeightValue) {
                    setMeasuredDimension(width, fixHeightValue);
                }
            } else if (width == ViewGroup.LayoutParams.WRAP_CONTENT && height != ViewGroup.LayoutParams.WRAP_CONTENT && height != ViewGroup.LayoutParams.MATCH_PARENT && height > 0) {
                if (DEBUG) Log.v(TAG, "\t using height for tag ");
                float factor = getDrawable().getIntrinsicWidth() / getDrawable().getIntrinsicHeight();
                int fixWidthValue = (int) (height * factor);
                fixWidthValue = resolveAdjustedSize(fixWidthValue, Integer.MAX_VALUE, widthMeasureSpec);
                if (DEBUG) Log.v(TAG, "\t max value for width -> " + fixWidthValue);
                if (getMeasuredWidth() != fixWidthValue) {
                    setMeasuredDimension(fixWidthValue, height);
                }
            }
        } else {
            if (DEBUG) Log.v(TAG, "\t drawable is null");
        }
    }


    /**
     * Copy from parent
     *
     * @param desiredSize
     * @param maxSize
     * @param measureSpec
     * @return
     */
    private int resolveAdjustedSize(int desiredSize, int maxSize,
                                    int measureSpec) {
        int result = desiredSize;
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                /* Parent says we can be as big as we want. Just don't be larger
                   than max size imposed on ourselves.
                */
                result = Math.min(desiredSize, maxSize);
                break;
            case MeasureSpec.AT_MOST:
                // Parent says we can be as big as we want, up to specSize.
                // Don't be larger than specSize, and don't be larger than
                // the max size imposed on ourselves.
                result = Math.min(Math.min(desiredSize, specSize), maxSize);
                break;
            case MeasureSpec.EXACTLY:
                // No choice. Do what we are told.
                result = specSize;
                break;
        }
        return result;
    }

}
