package com.example.base.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * 设备工具类
 */
public class YWDeviceUtil {

    private static final String TAG = "YWDeviceUtil";

    /**
     * 获取状态栏高度
     */
    public static int getStatusBarHeight() {
        int result = 0;
        Resources resources = Resources.getSystem();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        if (result <= 0) {
            result = YWCommonUtil.dp2px(25);
        }
        return result;
    }

    /**
     * 获取导航啦高度
     */
    public static int getNavigationBarHeight() {
        try {
            Resources resources = Resources.getSystem();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            int height = resources.getDimensionPixelSize(resourceId);
            Log.v("dbw", "Navi height:" + height);
            return height;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }


    /**
     * 获取屏幕宽度xt
     *
     * @return
     */
    public static int getScreenWidth() {
        Resources resources = Resources.getSystem();
        DisplayMetrics dm = resources.getDisplayMetrics();
        return dm.widthPixels;
    }

    /**
     * 获取屏幕宽度dp
     *
     * @return 返回屏幕的宽度，单位是dp
     */
    public static int getScreenWidthDp() {
        Resources resources = Resources.getSystem();
        DisplayMetrics dm = resources.getDisplayMetrics();
        float density = dm.density;
        return (int) (dm.widthPixels / density);
    }

    /**
     * 获取屏幕高度。一般不包含虚拟导航栏和状态栏，部分设备(HUAWEI P9)包含可隐藏虚拟导航栏
     */
    public static int getScreenHeight() {
        Resources resources = Resources.getSystem();
        DisplayMetrics dm = resources.getDisplayMetrics();
        return dm.heightPixels;
    }

    /**
     * 获取屏幕高度
     */
    public static int getScreenHeightWithStatusBar(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowMgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowMgr.getDefaultDisplay().getRealMetrics(metrics);
        return metrics.heightPixels;
    }

    /**
     * 获取当前路径大小
     */
    public static long getAvailableSize(String path) {
        try {
            final StatFs statFs = new StatFs(path);
            long blockSize = 0;
            long availableBlocks = 0;
            blockSize = statFs.getBlockSizeLong();
            availableBlocks = statFs.getAvailableBlocksLong();
            return availableBlocks * blockSize;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static final long SPACE_LEFT = 1024 * 1024 * 2;

    /**
     * 判断是否有足够的空间供下载
     */
    public static boolean hasEnoughSDCardSpace(Context context) {
        try {
            StatFs statFs = new StatFs(YWFileUtil.getStorageFileDir(context).getPath());
            // sd卡可用分区数
            long avCounts = statFs.getAvailableBlocksLong();
            // 一个分区数的大小
            long blockSize = statFs.getBlockSizeLong();
            // sd卡可用空间
            long spaceLeft = avCounts * blockSize;
            if (spaceLeft < SPACE_LEFT) {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }

        return true;
    }

    /**
     * 判断虚拟按键栏是否重写
     *
     * @return
     */
    private static String getNavBarOverride() {
        String sNavBarOverride = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                Class c = Class.forName("android.os.SystemProperties");
                Method m = c.getDeclaredMethod("get", String.class);
                m.setAccessible(true);
                sNavBarOverride = (String) m.invoke(null, "qemu.hw.mainkeys");
            } catch (Throwable e) {
            }
        }
        return sNavBarOverride;
    }

    /**
     * 获取系统属性
     *
     * @param propName
     * @return
     */
    public static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(
                    new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (Exception e) {
            Log.e("getSystemPropertyError", e.getMessage());
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e("getSystemPropertyError", e.getMessage());
                }
            }
        }
        return line;
    }

    /**
     * 存储空间不足
     *
     * @param context context
     */
    public static boolean isStorageSpaceNotEnough(Context context) {
        String dataDir = context.getApplicationInfo().dataDir;
        File externalCacheDir = context.getExternalCacheDir();
        File externalFileDir = context.getExternalFilesDir(null);
        File cacheDir = context.getCacheDir();
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        Log.i(TAG, "getStorageInfo dataDir = " + dataDir + " externalCacheDir = " + externalCacheDir.getPath()
                + " cacheDir = " + cacheDir.getPath() + " externalStorageDirectory = " + externalStorageDirectory
                + " externalFileDir = " + externalFileDir);
        StatFs storageStatFs = new StatFs(externalStorageDirectory.getPath());
        long storageAvCounts = storageStatFs.getAvailableBytes();

        Log.i(TAG, "getStorageInfo cacheAvCounts = " + storageAvCounts);
        long SPACE_THRESHOLD = 1024 * 1024 * 5;
        if (storageAvCounts < SPACE_THRESHOLD) {
            return true;
        }
        return false;
    }

}
