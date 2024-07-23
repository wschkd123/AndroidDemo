package com.example.beyond.demo.ui.transformer.overlay;

import static androidx.media3.common.util.Assertions.checkNotNull;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media3.common.C;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.DrawableOverlay;
import androidx.media3.effect.OverlaySettings;

import com.example.beyond.demo.R;
import com.example.beyond.demo.ui.transformer.TransformerConstant;

/**
 * @author wangshichao
 * @date 2024/7/9
 */
@UnstableApi
public class BgOverlay extends DrawableOverlay {
    protected String TAG = getClass().getSimpleName();

    private Context context;
    private @NonNull Bitmap lastBitmap;
    private @NonNull Drawable lastDrawable;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private OverlaySettings overlaySettings;
    private long period = C.MILLIS_PER_SECOND * TransformerConstant.REFRESH_PERIOD;


    public BgOverlay(Context context) {
        this.context = context;
        screenWidth = getScreenWidth(context);
        screenHeight = getScreenHeight(context);

        float[] positioningMatrix = GlUtil.create4x4IdentityMatrix();
        // 0，0在视频中心，1，1在右上角
        Matrix.translateM(
                positioningMatrix, /* mOffset= */ 0, /* x= */ 1f, /* y= */ 1f, /* z= */ 1);
        overlaySettings = new OverlaySettings.Builder()
                .setMatrix(positioningMatrix)
                .setAlpha(1f)
                // -1 -1 在原覆盖物右上角的位置，1 1 在原覆盖物左下角的位置
//                .setAnchor(-1f, -1f)
                .build();
    }

//  @Override
//  public Size getTextureSize(long presentationTimeUs) {
//    return new Size(screenWidth, screenHeight);
//  }

    @Override
    public Drawable getDrawable(long presentationTimeUs) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    public Bitmap getBitmap(long presentationTimeUs) {
        float index = presentationTimeUs * 1f / period;
        Log.i(TAG, "getBitmap presentationTimeUs=" + presentationTimeUs + " index=" + index);
        Drawable overlayDrawable = getDrawable(presentationTimeUs);
        // TODO(b/227625365): Drawable doesn't implement the equals method, so investigate other methods
        //   of detecting the need to redraw the bitmap.
        if (index >= 0 && index <= 300) {
//      overlaySettings.scale = Pair.create(index / 100, index / 100);

        }
        if (!overlayDrawable.equals(lastDrawable)) {
            lastDrawable = overlayDrawable;
            if (lastBitmap == null
                    || lastBitmap.getWidth() != lastDrawable.getIntrinsicWidth()
                    || lastBitmap.getHeight() != lastDrawable.getIntrinsicHeight()) {
                lastBitmap =
                        Bitmap.createBitmap(
                                lastDrawable.getIntrinsicWidth(),
                                lastDrawable.getIntrinsicHeight(),
                                Bitmap.Config.ARGB_8888);
            }
            Canvas canvas = new Canvas(lastBitmap);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            lastDrawable.draw(canvas);
            Log.w(TAG, "getBitmap draw presentationTimeUs=" + presentationTimeUs / C.MILLIS_PER_SECOND);
        } else {
//      lastBitmap
            int dy = 100;
//      lastBitmap =
//          Bitmap.createBitmap(
//              lastDrawable.getIntrinsicWidth() - 100,
//              lastDrawable.getIntrinsicHeight(),
//              Bitmap.Config.ARGB_8888);
//      Canvas canvas = new Canvas(lastBitmap);
//      canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//      lastDrawable.draw(canvas);
        }
        return checkNotNull(lastBitmap);
    }

    @Override
    public OverlaySettings getOverlaySettings(long presentationTimeUs) {
        return overlaySettings;
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        return metric.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        return metric.heightPixels;
    }
}
