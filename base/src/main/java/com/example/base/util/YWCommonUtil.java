package com.example.base.util;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.base.Init;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

/**
 * 通用型工具
 */
public class YWCommonUtil {

    /**
     * dp 转 px 工具
     *
     * @param dipValue dp 值
     * @return px 值
     */
    public static int dp2px(float dipValue) {
        //Resources resources = Resources.getSystem();
        Resources resources = Init.application.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dipValue
                , resources.getDisplayMetrics());
    }

    /**
     * dp 转 px 工具
     *
     * @param dipValue dp 值
     * @return px 值
     */
    public static float dp2pxFloat(float dipValue) {
        Resources resources = Init.application.getResources();
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dipValue
                , resources.getDisplayMetrics());
    }

    /**
     * 版本名称 eg:7.5.5
     *
     * @param context 上下文
     * @return 版本名称
     */
    public static String getVersionName(Context context) {
        String versionName = "";
        try {
            versionName = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }

    /**
     * 版本号 eg:169
     *
     * @param context 上下文
     * @return 版本号
     */
    public static int getVersionCode(Context context) {
        int versionCode = 0;
        try {
            versionCode = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 判断某个App是否存在
     *
     * @param packageName app包名
     * @return true：已安装 false：未安装
     */
    public static boolean isAppExist(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(
                    packageName, 0);
        } catch (NameNotFoundException e) {
            Log.e("isAppExist", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 展示软键盘
     *
     * @param view 目标 View
     * @param mContext 上下文
     * @return 是否展示成功
     */
    public static boolean showKeyBoard(View view, Context mContext) {
        boolean b = false;
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) mContext
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                b = inputMethodManager.showSoftInput(view, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * 展示软键盘
     *
     * @param view 目标 View
     * @param mContext 上下文
     * @param flags 标志位
     * @return 是否展示成功
     */
    public static boolean showKeyBoard(View view, Context mContext, int flags,
            ResultReceiver resultReceiver) {
        boolean b = false;
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) mContext
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                b = inputMethodManager.showSoftInput(view, flags, resultReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * 隐藏软键盘
     *
     * @param token 目标 token
     * @param mContext 上下文
     * @return 是否展示成功
     */
    public static boolean hideKeyBoard(IBinder token, Context mContext) {
        boolean b = false;
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) mContext
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null && inputMethodManager.isActive()) {
                b = inputMethodManager.hideSoftInputFromWindow(
                        token, 0);
            }
        } catch (Exception e) {
            //ignore for bad token
        }
        return b;
    }

    /**
     * 隐藏软键盘
     *
     * @param token 目标 token
     * @param mContext 上下文
     * @param flags 标志位
     * @return 是否展示成功
     */
    public static boolean hideKeyBoard(IBinder token, Context mContext, int flags,
            ResultReceiver resultReceiver) {
        boolean b = false;
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) mContext
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null && inputMethodManager.isActive()) {
                b = inputMethodManager.hideSoftInputFromWindow(
                        token, flags, resultReceiver);
            }
        } catch (Exception e) {
            //ignore for bad token
        }
        return b;
    }


    /**
     * 设置 View 大小
     *
     * @param view 要设置大小的 View
     * @param width 宽度
     * @param height 高度
     */
    public static void setViewSize(@NonNull final View view, final int width, final int height) {
        final ViewGroup.LayoutParams layoutParams;
        if (view.getLayoutParams() != null) {
            layoutParams = view.getLayoutParams();
            layoutParams.width = width;
            layoutParams.height = height;
        } else {
            final ViewParent parent = view.getParent();
            if (parent instanceof LinearLayout) {
                layoutParams = new LinearLayout.LayoutParams(width, height);
            } else if (parent instanceof RelativeLayout) {
                layoutParams = new RelativeLayout.LayoutParams(width, height);
            } else if (parent instanceof FrameLayout) {
                layoutParams = new FrameLayout.LayoutParams(width, height);
            } else if (parent instanceof RecyclerView) {
                layoutParams = new RecyclerView.LayoutParams(width, height);
            } else if (parent instanceof AbsListView) {
                layoutParams = new AbsListView.LayoutParams(width, height);
            } else {
                layoutParams = new ViewGroup.LayoutParams(width, height);
            }
        }
        view.setLayoutParams(layoutParams);
    }

    /**
     * 金额按千分割
     *
     * @param amount amount
     * @return formatted amount
     */
    public static String formatAmount(Integer amount) {
        if (amount == null) {
            return "";
        }
        if (amount < 0) {
            return String.valueOf(amount);
        }
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        return decimalFormat.format(amount);
    }


    /**
     * 设置 view 灰色
     *
     * @param view
     * @param enableGrey
     */
    public static void grayView(View view, boolean enableGrey) {
        if (view == null) {
            return;
        }
        Paint paint = new Paint();
        ColorMatrix matrix = new ColorMatrix();
        if (enableGrey) {
            matrix.setSaturation(0f);
        } else {
            matrix.setSaturation(1f);
        }
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    public static String calculateMD5(String input) {
        try {
            // 创建 MessageDigest 对象并指定算法为 MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 将输入字符串转换为字节数组
            byte[] inputBytes = input.getBytes();

            // 使用指定的字节数组更新摘要
            md.update(inputBytes);

            // 完成摘要计算，返回结果的字节数组
            byte[] digestBytes = md.digest();

            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digestBytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


}
