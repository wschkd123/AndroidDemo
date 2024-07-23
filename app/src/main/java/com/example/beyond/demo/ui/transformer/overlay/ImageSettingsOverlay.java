package com.example.beyond.demo.ui.transformer.overlay;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.opengl.Matrix;

import androidx.core.content.ContextCompat;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.DrawableOverlay;
import androidx.media3.effect.OverlaySettings;

import com.example.beyond.demo.R;

/**
 * @author wangshichao
 * @date 2024/7/10
 */
@UnstableApi
public class ImageSettingsOverlay extends DrawableOverlay {

    private Context context;
    public OverlaySettings overlaySettings;

    public ImageSettingsOverlay(Context context) {
        this.context = context;
        float[] translateMatrix = GlUtil.create4x4IdentityMatrix();
        Matrix.translateM(translateMatrix, /* mOffset= */ 0, /* x= */ 0f, /* y= */ 1f, /* z= */ 1);
        overlaySettings = new OverlaySettings.Builder()
                .setMatrix(translateMatrix)
                // 向下偏移
                .setAnchor(0f, 1f)
                .build();
    }

    public static DrawableOverlay createOverlay(
            Context context, OverlaySettings overlaySettings) {
        return new ImageSettingsOverlay(context) {
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
    public OverlaySettings getOverlaySettings(long presentationTimeUs) {
        return overlaySettings;
    }
}
