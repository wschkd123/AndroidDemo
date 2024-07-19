package com.example.beyond.demo.ui.transformer.overlay;

import static androidx.media3.common.util.Assertions.checkNotNull;
import static androidx.media3.common.util.Util.SDK_INT;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import androidx.annotation.RequiresApi;
import androidx.media3.common.C;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.OverlaySettings;
import androidx.media3.effect.TextureOverlay;

/**
 * A {@link TextureOverlay} that displays a "time elapsed" timer in the bottom left corner of the
 * frame.
 */
@UnstableApi
public class ChatContentOverlay extends TextOverlay {
  private static final String TAG = "ChatContentOverlay";
  private final OverlaySettings overlaySettings;

  private final static String  TEST_STRING = "毒鸡汤大魔王，会收集负面情绪，贱贱毒舌却又心地善良的好哥哥，也是持之以恒、霸气侧漏的灵气复苏时代的最强王者、星图战神。\n"
      + "吕树，别名为第九天罗，依靠毒鸡汤成为大魔王。身世成谜，自小在福利院中长大，16岁后脱离福利院，与吕小鱼相依为命，通过卖煮鸡蛋维持生计。擅长怼人、噎人、气人，却从不骂人。平时说话贱贱的，被京都天罗地网同仁称为“贱圣”，但从不骂人，喜欢用讲道理却不似道理的话怼人。\n"
      + "无父无母，从小吃了很多苦，刚开始卖煮鸡蛋时卖不出去，边哭边吃煮鸡蛋，不停地被蛋黄干磕到。因此对金钱数字特别敏感，但为人有自己绝对的道德底线。";
  private long period = C.MICROS_PER_SECOND * 2;
  private int overlayWidth = 300;
  private int overlayHeight = 300;

  public ChatContentOverlay() {
    overlaySettings =
        new OverlaySettings.Builder()
//            .setOverlayFrameAnchor(/* x= */ 0.5f, /* y= */ 0.5f)
//            .setBackgroundFrameAnchor(/* x= */ -1f, /* y= */ 1f)
            .build();
  }

  @Override
  public SpannableString getText(long presentationTimeUs) {
    int index = (int) (presentationTimeUs / period);
    SpannableString result;
    if (index > TEST_STRING.length() || index <= 0){
      result = new SpannableString(TEST_STRING);
      result.setSpan(
          new ForegroundColorSpan(Color.WHITE),
          /* start= */ 0,
          TEST_STRING.length(),
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    } else {
      String shownText = TEST_STRING.substring(0, index);
      result = new SpannableString(shownText);
      result.setSpan(
          new ForegroundColorSpan(Color.WHITE),
          /* start= */ 0,
          shownText.length(),
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return result;
  }

  @Override
  public Bitmap getBitmap(long presentationTimeUs) {
    Log.i(TAG, "getBitmap presentationTimeUs=" + presentationTimeUs);
    SpannableString overlayText = getText(presentationTimeUs);
    if (!overlayText.equals(lastText)) {
      lastText = overlayText;
      TextPaint textPaint = new TextPaint();
      textPaint.setTextSize(30);
//      int textWidth = getSpannedTextWidth(overlayText, textPaint);
      StaticLayout staticLayout =
          createStaticLayout(overlayText, textPaint, overlayWidth);
      if (lastBitmap == null
          || lastBitmap.getWidth() != staticLayout.getWidth()
          || lastBitmap.getHeight() != staticLayout.getHeight()) {
        lastBitmap =
            Bitmap.createBitmap(
                staticLayout.getWidth(), overlayHeight, Bitmap.Config.ARGB_8888);
      }
      Canvas canvas = new Canvas(checkNotNull(lastBitmap));
      canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
      staticLayout.draw(canvas);
    }
    return checkNotNull(lastBitmap);
  }

  @Override
  public OverlaySettings getOverlaySettings(long presentationTimeUs) {
    return overlaySettings;
  }

  @SuppressLint("InlinedApi") // Inlined Layout constants.
  private StaticLayout createStaticLayout(SpannableString text, TextPaint textPaint, int width) {
    return SDK_INT >= 23
        ? getStaticLayout(text, textPaint, width)
        : new StaticLayout(
            text,
            textPaint,
            width,
            Layout.Alignment.ALIGN_CENTER,
            Layout.DEFAULT_LINESPACING_MULTIPLIER,
            Layout.DEFAULT_LINESPACING_ADDITION,
            /* includepad= */ true);
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  public static StaticLayout getStaticLayout(
      SpannableString text, TextPaint textPaint, int width) {
    return StaticLayout.Builder.obtain(
            text, /* start= */ 0, /* end= */ text.length(), textPaint, width)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .build();
  }
}
