package com.qq.reader.component.logger;

/**
 * 日志配置
 * Created by dongxiaolong on 2017/6/9.
 */

public class LogConfiguration {

    /**
     * xlog 文件输出路径
     */
    private String mLogPath;
    /**
     * xlog cache路径
     */
    private String mCachePath;
    /**
     * xlog文件前缀
     */
    private String mLogPrefix;


    /**
     * XLog 日志等级
     */
    private int mXLogLevel = Logger.LEVEL_NONE;
    /**
     * Logcat 日志等级
     */
    private int mConsoleLogLevel = Logger.LEVEL_NONE;

    /**
     * 是否打印调用栈信息
     */
    private boolean mPrintStackInfo = false;

    /**
     * XLog 加密公钥
     */
    private String mXlogPubKey;

    private LogConfiguration() {

    }

    public String getLogPath() {
        return mLogPath;
    }

    public String getCachePath() {
        return mCachePath;
    }

    public String getLogPrefix() {
        return mLogPrefix;
    }

    public int getXLogLevel() {
        return mXLogLevel;
    }

    public int getConsoleLogLevel() {
        return mConsoleLogLevel;
    }

    public boolean isPrintStackInfo() {
        return mPrintStackInfo;
    }

    public String getXlogPubKey() {
        return mXlogPubKey;
    }

    public static final class Builder {

        private LogConfiguration mLogConfig;

        public Builder() {
            mLogConfig = new LogConfiguration();
        }

        public LogConfiguration build() {
            return mLogConfig;
        }

        public Builder setLogPath(String logPath) {
            mLogConfig.mLogPath = logPath;
            return this;
        }

        public Builder setCachePath(String cachePath) {
            mLogConfig.mCachePath = cachePath;
            return this;
        }

        public Builder setLogPrefix(String logPrefix) {
            mLogConfig.mLogPrefix = logPrefix;
            return this;
        }

        public Builder setConsoleLogLevel(int consoleLogLevel) {
            mLogConfig.mConsoleLogLevel = consoleLogLevel;
            return this;
        }

        public Builder setXLogLevel(int xLogLevel) {
            mLogConfig.mXLogLevel = xLogLevel;
            return this;
        }

        public Builder setPrintStackInfo(boolean printStackInfo) {
            mLogConfig.mPrintStackInfo = printStackInfo;
            return this;
        }

        public Builder setXlogPubKey(String xlogPubKey) {
            mLogConfig.mXlogPubKey = xlogPubKey;
            return this;
        }


    }

}
