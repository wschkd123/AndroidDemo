package com.example.beyond.demo.ui.transformer.overlay;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.OverlaySettings;

/**
 * @author wangshichao
 * @date 2024/7/9
 */
@UnstableApi
public class CustomTextOverlay extends TextOverlay {

  private final OverlaySettings overlaySettings;

  public CustomTextOverlay() {
    overlaySettings =
        new OverlaySettings.Builder()
            // Place the timer in the bottom left corner of the screen with some padding from the
            // edges.
//            .setOverlayFrameAnchor(/* x= */ 1f, /* y= */ 1f)
//            .setBackgroundFrameAnchor(/* x= */ -0.7f, /* y= */ -0.95f)
            .build();
  }

  @Override
  public SpannableString getText(long presentationTimeUs) {
    String secondsString = String.valueOf(presentationTimeUs / C.MICROS_PER_SECOND);
    String timeString = secondsString + "s";
    SpannableString text = new SpannableString(timeString);

    // Following font styles are applied for consistent text rendering between devices.
    text.setSpan(
        new ForegroundColorSpan(Color.BLACK),
        /* start= */ 0,
        text.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    text.setSpan(
        new AbsoluteSizeSpan(/* size= */ 20),
        /* start= */ 0,
        text.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    text.setSpan(
        new TypefaceSpan(/* family= */ "sans-serif"),
        /* start= */ 0,
        text.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    // Following font styles increase pixel difference for the text it's applied on when
    // this text changes, but also may be implemented differently on different devices
    // or emulators, providing extraneous pixel differences. Only apply these styles to
    // the values we expect to change in the event of a failing test. Namely, only apply
    // these styles to the timestamp.
    int timestampStart = text.length() - timeString.length();
    int timestampEnd = timestampStart + secondsString.length();
    text.setSpan(
        new BackgroundColorSpan(Color.WHITE),
        timestampStart,
        timestampEnd,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    text.setSpan(
        new StyleSpan(Typeface.BOLD),
        timestampStart,
        timestampEnd,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    text.setSpan(
        new AbsoluteSizeSpan(/* size= */ 42),
        timestampStart,
        timestampEnd,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return text;
  }

  @Override
  public OverlaySettings getOverlaySettings(long presentationTimeUs) {
    return overlaySettings;
  }
}
