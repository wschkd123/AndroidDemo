package com.qq.reader.component.logger;

import android.text.TextUtils;
import com.yuewen.ywlog.ILogger;
import com.yuewen.ywlog.YWLog;
import com.yuewen.ywlog.YWLogConfigBuilder;
import java.io.File;
import java.util.Map;

/**
 * Log日志工具
 */
public class Logger {

    public static final int LEVEL_VERBOSE = YWLog.LEVEL_VERBOSE;
    public static final int LEVEL_DEBUG = YWLog.LEVEL_DEBUG;
    public static final int LEVEL_INFO = YWLog.LEVEL_INFO;
    public static final int LEVEL_WARNING = YWLog.LEVEL_WARNING;
    public static final int LEVEL_ERROR = YWLog.LEVEL_ERROR;
    public static final int LEVEL_FATAL = YWLog.LEVEL_FATAL;
    public static final int LEVEL_NONE = YWLog.LEVEL_NONE;

    /**
     * 是否打印调用栈信息
     */
    private static boolean mPrintStackInfo = false;

    /**
     * 初始化Logger
     *
     * @param logConfiguration
     * @param callback
     */
    public synchronized static void init(final LogConfiguration logConfiguration,
            final InitCallback callback) {
        String logPre = logConfiguration.getLogPrefix();
        if (!TextUtils.isEmpty(logPre) && logPre.contains(":")) {
            logPre = logPre.replace(":", "_");
        }
        YWLogConfigBuilder.Builder builder = new YWLogConfigBuilder.Builder()
                .setLogPath(logConfiguration.getLogPath())
                .setCachePath(logConfiguration.getCachePath())
                .setLogPrefix(logPre)
                .setConsoleLogOpen(false)
                .setXLogLevel(logConfiguration.getXLogLevel())
                .setXlogPubKey(logConfiguration.getXlogPubKey())
                .setCacheDays(0);
        mPrintStackInfo = logConfiguration.isPrintStackInfo();
        YWLog.setCustomLogger(new ConsoleLog(logConfiguration.getConsoleLogLevel()));
        YWLog.InitCallback ywlogCallback = null;
        if (callback != null) {
            ywlogCallback = new YWLog.InitCallback() {
                @Override
                public void onSuccess() {
                    callback.onSuccess();
                }

                @Override
                public void onFailed(Throwable throwable) {
                    callback.onFailed(throwable);
                }
            };
        }
        YWLog.init(builder, ywlogCallback);
    }

    private static class ConsoleLog implements ILogger {

        private final int level;

        public ConsoleLog(int level) {
            this.level = level;
        }

        @Override
        public void v(String tag, String msg) {
            if (level <= LEVEL_VERBOSE) {
                android.util.Log.v(tag, msg);
            }
        }

        @Override
        public void d(String tag, String msg) {
            if (level <= LEVEL_DEBUG) {
                android.util.Log.d(tag, msg);
            }
        }

        @Override
        public void i(String tag, String msg) {
            if (level <= LEVEL_INFO) {
                android.util.Log.i(tag, msg);
            }
        }

        @Override
        public void w(String tag, String msg) {
            if (level <= LEVEL_WARNING) {
                android.util.Log.w(tag, msg);
            }
        }

        @Override
        public void e(String tag, String msg) {
            if (level <= LEVEL_ERROR) {
                android.util.Log.e(tag, msg);
            }
        }

        @Override
        public void e(String tag, String msg, Throwable throwable) {
            if (level <= LEVEL_ERROR) {
                android.util.Log.e(tag, msg);
            }
        }
    }

    /**
     * 将日志从mmap推送到xlog文件中，isSync如果为true则为耗时方法
     *
     * @param isSync
     */
    public static void flush(boolean isSync) {
        YWLog.flush(isSync);
    }

    /**
     * 计算 XLog 日志文件大小（不含 mmap 文件），耗时操作
     *
     * @return KB
     */
    public static float getLogsTotalSize() {
        String mLogDirPath = YWLog.getXLogPath();
        long totalLength;
        if (!TextUtils.isEmpty(mLogDirPath)) {
            try {
                File logDirFile = new File(mLogDirPath);
                if (logDirFile.exists()) {
                    totalLength = getFolderSize(logDirFile);
                    return totalLength / 1024f;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.w("Logger.getLogsTotalSize", e.getMessage());
            }
        }

        return 0;
    }

    /**
     * @param file file
     * @return size Byte
     * @throws Exception
     */
    private static long getFolderSize(File file) {
        long size = 0;
        if (file == null) {
            return 0;
        }

        if (!file.isDirectory()) {
            return file.length();
        }

        File[] fileList = file.listFiles();
        for (File aFileList : fileList) {
            try {
                if (aFileList.isDirectory()) {
                    size = size + getFolderSize(aFileList);
                } else {
                    size = size + aFileList.length();
                }
            } catch (Exception e) {
                Logger.w("Logger.getFolderSize", e.getMessage());
            }
        }

        return size;
    }

    public static void v(String tag, final String log) {
        YWLog.v(tag, getLogMsg(log));
    }

    public static void v(String tag, final String log, boolean isForceWrite2File) {
        YWLog.v(tag, getLogMsg(log), isForceWrite2File);
    }

    public static void d(String tag, final String log) {
        YWLog.d(tag, getLogMsg(log));
    }

    public static void d(String tag, final String log, boolean isForceWrite2File) {
        YWLog.d(tag, getLogMsg(log), isForceWrite2File);
    }

    public static void i(String tag, final String log) {
        YWLog.i(tag, getLogMsg(log));
    }

    public static void i(String tag, final String log, boolean isForceWrite2File) {
        YWLog.i(tag, getLogMsg(log), isForceWrite2File);
    }

    public static void w(String tag, final String log) {
        YWLog.w(tag, getLogMsg(log));
    }

    public static void w(String tag, final String log, boolean isForceWrite2File) {
        YWLog.w(tag, getLogMsg(log), isForceWrite2File);
    }

    public static void e(String tag, final String log) {
        YWLog.e(tag, getLogMsg(log));
    }

    public static void e(String tag, final String log, boolean isForceWrite2File) {
        YWLog.e(tag, getLogMsg(log), isForceWrite2File);
    }

    public static void e(String tag, final Exception e, boolean isForceWrite2File) {
        YWLog.e(tag, buildException(e), isForceWrite2File);
    }

    private static String buildException(Exception e) {
        if (e == null) {
            return "buildException failed.";
        }

        StackTraceElement[] elements = e.getStackTrace();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            stringBuilder.append(elements[i]);
            stringBuilder.append('\n');
        }

        return stringBuilder.toString();
    }

    public static String getStackTrace() {
        Map<Thread, StackTraceElement[]> ts = Thread.getAllStackTraces();
        if (ts == null) {
            return "Thread all stackTraces is error StackTraceElement Maps == null ";
        }
        StackTraceElement[] ste = ts.get(Thread.currentThread());
        if (ste == null) {
            return "Thread all stackTraces is error StackTraceElement == null ";
        }
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < ste.length; ++i) {
            if (i >= 5) {
                sb.append(ste[i].toString()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 格式化XLog 可以根据需求统一输出
     *
     * @param methodName 方法名
     * @param action 操作或事件
     * @param information log信息
     */
    @Deprecated
    public static String formatLogMsg(String methodName, String action, String information) {
        return new StringBuilder()
                .append("(").append(methodName).append(")")
                .append("[").append(action).append("]")
                .append("{").append(information).append("}")
                .toString();
    }

    private static String getLogMsg(String msg) {
        if (msg == null) {
            msg = "";
        }

        if (!mPrintStackInfo) {
            return msg;
        }

        StackTraceElement stackTraceElement = new Throwable().fillInStackTrace().getStackTrace()[2];

        String className = stackTraceElement.getClassName();
        if (className != null && className.contains(".")) {
            className = className.substring(className.lastIndexOf(".") + 1);
        }
        return new StringBuilder()
                .append("{")
                .append("T:" + Thread.currentThread().getName() + ",")
                .append(className + ":" + stackTraceElement.getMethodName()
                        + ":"
                        + stackTraceElement.getLineNumber()).append("} - ").append(msg).toString();
    }

    /**
     * 初始化回调
     */
    public interface InitCallback {

        /**
         * 初始化成功
         */
        void onSuccess();

        /**
         * 初始化失败
         *
         * @param throwable
         */
        void onFailed(Throwable throwable);
    }


}