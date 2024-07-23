package com.example.beyond.demo.ui.transformer.overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.opengl.Matrix;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.media3.common.C;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.DrawableOverlay;
import androidx.media3.effect.OverlaySettings;

import com.example.beyond.demo.R;

import java.lang.reflect.Field;

/**
 * @author wangshichao
 * @date 2024/7/10
 */
@UnstableApi
public class ImageOverlay extends DrawableOverlay {
    private static final String TAG = ImageOverlay.class.getSimpleName();
    private Context context;
    public OverlaySettings overlaySettings;

    public ImageOverlay(Context context) {
        this.context = context;
        float[] translateMatrix = GlUtil.create4x4IdentityMatrix();
        // 0，0在视频中心，1，1在右上角
        Matrix.translateM(translateMatrix, /* mOffset= */ 0, /* x= */ 0f, /* y= */ 0f, /* z= */ 1);
        overlaySettings = new OverlaySettings.Builder()
                .setMatrix(translateMatrix)
                // -1 -1 在原覆盖物右上角的位置，1 1 在原覆盖物左下角的位置（接近覆盖物宽高，但是不超过）
                .setAnchor(0f, 1f)
                .build();
    }

    public static DrawableOverlay createOverlay(
            Context context, OverlaySettings overlaySettings) {
        return new ImageOverlay(context) {
            @Override
            public OverlaySettings getOverlaySettings(long presentationTimeUs) {
                return overlaySettings;
            }
        };
    }

    @Override
    public Drawable getDrawable(long presentationTimeUs) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.character_bg);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    }

    @Override
    public Bitmap getBitmap(long presentationTimeUs) {
        float progress = (float) presentationTimeUs / (C.MICROS_PER_SECOND * 5);
        Log.i(TAG, "getBitmap: progress=" + progress + " presentationTimeMs=" + presentationTimeUs/1000);
        if (progress >= 0 && progress <= 1) {
            modifyFiled("alpha", progress);
        }
        return super.getBitmap(presentationTimeUs);
    }

    @Override
    public OverlaySettings getOverlaySettings(long presentationTimeUs) {
        return overlaySettings;
    }

    private void modifyFiled(String fieldName, float value) {
        try {
            Field field = overlaySettings.getClass().getField(fieldName);
            Log.i(TAG, "modifyFiled: oldValue=" + field.get(overlaySettings) + " newValue=" + value);
            field.setAccessible(true);
            field.set(overlaySettings, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
