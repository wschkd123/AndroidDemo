package com.example.base.player.audiotrack;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 使用 AudioTrack 播放音频
 *
 * @author Richie on 2018.10.26
 */
public class AudioTracker {
    private static final String TAG = "AudioTracker";
    // 采样率 44100Hz，所有设备都支持
    private final static int SAMPLE_RATE = 44100;
    // 单声道，所有设备都支持
    private final static int CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    // 位深 16 位，所有设备都支持
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    private int mBufferSizeInBytes;
    // 播放对象
    private AudioTrack mAudioTrack;
    // 状态
    private volatile Status mStatus = Status.STATUS_NO_READY;
    // 单任务线程池
    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private AudioPlayListener mAudioPlayListener;

    public void createAudioTrack() throws IllegalStateException {
        mBufferSizeInBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL, AUDIO_FORMAT);
        if (mBufferSizeInBytes <= 0) {
            throw new IllegalStateException("AudioTrack is not available " + mBufferSizeInBytes);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAudioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(mBufferSizeInBytes)
                    .build();
        } else {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL, AUDIO_FORMAT,
                    mBufferSizeInBytes, AudioTrack.MODE_STREAM);
        }
        mStatus = Status.STATUS_READY;
    }

    /**
     * 开始播放
     *
     * @throws IllegalStateException
     */
    public void start(byte[] byteArray) throws IllegalStateException {
        if (mStatus == Status.STATUS_NO_READY || mAudioTrack == null) {
            throw new IllegalStateException("播放器尚未初始化");
        }
        if (mStatus == Status.STATUS_START) {
            throw new IllegalStateException("正在播放...");
        }
        Log.d(TAG, "===start===");
        mStatus = Status.STATUS_START;
//        mExecutorService.execute(new Runnable() {
//            @Override
//            public void run() {
                try {
                    playAudioData(byteArray);
                } catch (IOException e) {
                    Log.e(TAG, "playAudioData: ", e);
                    if (mAudioPlayListener != null) {
                        mAudioPlayListener.onError(e.getMessage());
                    }
                }
//            }
//        });
    }

    /**
     * 播放 PCM 音频
     *
     * @throws IOException
     */
    private void playAudioData(byte[] bytes) throws IOException {
        try  {
            if (mAudioPlayListener != null) {
                mAudioPlayListener.onStart();
            }
            mAudioTrack.play();
            mAudioTrack.write(bytes, 0, bytes.length);
        } finally {
            mAudioTrack.stop();
            if (mAudioPlayListener != null) {
                mAudioPlayListener.onStop();
            }
        }
    }

    public void appendAudioData(byte[] bytes) {
        mAudioTrack.write(bytes, 0, bytes.length);
    }

    /**
     * 停止播放
     *
     * @throws IllegalStateException
     */
    public void stop() throws IllegalStateException {
        Log.d(TAG, "===stop===");
        if (mStatus == Status.STATUS_NO_READY || mStatus == Status.STATUS_READY) {
            throw new IllegalStateException("播放尚未开始");
        } else {
            mStatus = Status.STATUS_STOP;
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "==release===");
        mStatus = Status.STATUS_NO_READY;
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    public void setAudioPlayListener(AudioPlayListener audioPlayListener) {
        mAudioPlayListener = audioPlayListener;
    }

    /**
     * 播放对象的状态
     */
    public enum Status {
        //未开始
        STATUS_NO_READY,
        //预备
        STATUS_READY,
        //播放
        STATUS_START,
        //停止
        STATUS_STOP
    }

    /**
     * invoked on work thread
     */
    public interface AudioPlayListener {

        /**
         * 开始
         */
        void onStart();

        /**
         * 结束
         */
        void onStop();

        /**
         * 发生错误
         *
         * @param message
         */
        void onError(String message);
    }
}