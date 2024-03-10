package com.example.beyond.demo.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.graphics.drawable.StateListDrawable;

import androidx.annotation.Nullable;


public class ColorDrawableUtils {

    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
    private static final int COLOR_DRAWABLE_DIMENSION = 2;

    /**
     * 从Drawable中获取Bitmap
     *
     * @param drawable 源
     * @return Bitmap
     */
    public static Bitmap getBitmapFromDrawable(@Nullable Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof StateListDrawable) {
            drawable = drawable.getCurrent();
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = null;
        try {
            if (drawable instanceof ColorDrawable) {
                bitmap = Bitmap.createBitmap(COLOR_DRAWABLE_DIMENSION, COLOR_DRAWABLE_DIMENSION,
                        BITMAP_CONFIG);
                bitmap.eraseColor(((ColorDrawable) drawable).getColor());
            } else if (drawable.getBounds().isEmpty()) {
                bitmap = Bitmap.createBitmap(Math.max(drawable.getIntrinsicWidth(), 2),
                        Math.max(drawable.getIntrinsicHeight(), 2), BITMAP_CONFIG);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            } else {
                bitmap = Bitmap.createBitmap(Math.max(drawable.getIntrinsicWidth(), 2),
                        Math.max(drawable.getIntrinsicHeight(), 2), BITMAP_CONFIG);
                Canvas canvas = new Canvas(bitmap);
                canvas.translate(-drawable.getBounds().left, -drawable.getBounds().top);
                drawable.draw(canvas);
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 动态生成Shape样式的Drawable
     *
     * @return
     */
    public static class ShapeDrawableBuilder {

        int corner;
        int strokeWidth, strokeColor;
        int solidColor;
        int[] solidColors;
        Orientation orientation;
        float[] corners;

        public ShapeDrawableBuilder setCorner(int corner) {
            this.corner = corner;
            return this;
        }

        public ShapeDrawableBuilder setCornerRadii(float tlcorner, float trcorner, float brcorner,
                                                   float blcorner) {
            this.corners = new float[]{tlcorner, tlcorner, trcorner, trcorner, brcorner, brcorner,
                    blcorner, blcorner};
            return this;
        }

        public ShapeDrawableBuilder setStrokeWidth(int strokeWidth) {
            this.strokeWidth = strokeWidth;
            return this;
        }

        public ShapeDrawableBuilder setStrokeColor(int strokeColor) {
            this.strokeColor = strokeColor;
            return this;
        }

        public ShapeDrawableBuilder setSolidColor(int solidColor) {
            this.solidColor = solidColor;
            return this;
        }

        public ShapeDrawableBuilder setSolidColors(int[] solidColors, Orientation orientation) {
            this.solidColors = solidColors;
            this.orientation = orientation;
            return this;
        }

        public GradientDrawable build() {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setCornerRadius(corner);
            if (corners != null) {
                gradientDrawable.setCornerRadii(corners);
            }
            gradientDrawable.setStroke(strokeWidth, strokeColor);
            if (solidColors != null) {
                gradientDrawable.setColors(solidColors);
                gradientDrawable.setOrientation(orientation);
            } else {
                gradientDrawable.setColor(solidColor);
            }
            return gradientDrawable;
        }

    }

}
