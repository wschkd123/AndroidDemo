package com.example.beyond.demo.ui.transformer.overlay;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.DrawableOverlay;
import androidx.media3.effect.OverlaySettings;

import com.example.beyond.demo.R;

/**
 * @author wangshichao
 * @date 2024/7/10
 */
@UnstableApi
public class TestSettingsOverlay extends DrawableOverlay {

  private Context context;
  public OverlaySettings overlaySettings;

  public TestSettingsOverlay(Context context) {
    this.context = context;
    overlaySettings = new OverlaySettings.Builder()
        // Place the logo in the bottom left corner of the screen with some padding from the
        // edges.
//        .setScale(/* x= */ 2, /* y= */ 2)
//        .setAlphaScale(0.5f)
//        .setRotationDegrees(90f)
//        .setOverlayFrameAnchor(/* x= */ 0f, /* y= */ 1f)
//        .setBackgroundFrameAnchor(/* x= */ -0.95f, /* y= */ -0.95f)
        .build();
  }

  public static DrawableOverlay createOverlay(
      Context context, OverlaySettings overlaySettings) {
    return new TestSettingsOverlay(context) {
      @Override
      public OverlaySettings getOverlaySettings(long presentationTimeUs) {
        return overlaySettings;
      }
    };
  }

  @Override
  public Drawable getDrawable(long presentationTimeUs) {
    Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher);
    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    return drawable;
  }

  @Override
  public OverlaySettings getOverlaySettings(long presentationTimeUs) {
    return overlaySettings;
  }
}
