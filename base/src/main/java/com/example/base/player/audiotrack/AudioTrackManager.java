package com.example.base.player.audiotrack;

import static android.media.AudioTrack.PLAYSTATE_PLAYING;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class AudioTrackManager {

    private static final String TAG = "AudioTrack-ExoPlayer";
    private AudioTrack mAudioTrack;
    private volatile static AudioTrackManager mInstance;
    private long bufferCount;

    /**
     * 音频流类型
     */
    private static final int mStreamType = AudioManager.STREAM_MUSIC;
    /**
     * 指定采样率 （MediaRecoder 的采样率通常是8000Hz AAC的通常是44100Hz。
     * 设置采样率为44100，目前为常用的采样率，官方文档表示这个值可以兼容所有的设置）
     */
    //TODO 需要与源音频保持一致
    private static final int mSampleRateInHz = 32000;
    /**
     * 指定捕获音频的声道数目。在AudioFormat类中指定用于此的常量
     */
    private static final int mChannelConfig = AudioFormat.CHANNEL_OUT_MONO; //单声道

    /**
     * 指定音频量化位数 ,在AudioFormaat类中指定了以下各种可能的常量。
     * 通常我们选择ENCODING_PCM_16BIT和ENCODING_PCM_8BIT PCM代表的是脉冲编码调制，它实际上是原始音频样本。
     * 因此可以设置每个样本的分辨率为16位或者8位，16位将占用更多的空间和处理能力,表示的音频也更加接近真实。
     */
    private static final int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 指定缓冲区大小。调用AudioTrack类的getMinBufferSize方法可以获得。
     */
    private int mMinBufferSize;

    /**
     * STREAM的意思是由用户在应用程序通过write方式把数据一次一次得写到audiotrack中。
     * 这个和我们在socket中发送数据一样，
     * 应用层从某个地方获取数据，例如通过编解码得到PCM数据，然后write到audiotrack。
     */
    private static int mMode = AudioTrack.MODE_STREAM;

    private IAudioPlayStateListener iAudioPlayStateListener;
    private static final int BUFFER_CAPITAL = 10;

    private ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {

        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "AudioTrack #" + mCount.getAndIncrement());
        }
    });

    /**
     * 获取单例引用
     *
     * @return
     */
    public static AudioTrackManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioTrackManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioTrackManager();
                }
            }
        }
        return mInstance;
    }


    public AudioTrackManager() {
        initAudioTrack();
    }


    private void initAudioTrack() {
        //根据采样率，采样精度，单双声道来得到frame的大小。
        //计算最小缓冲区 *10
        //注意，按照数字音频的知识，这个算出来的是一秒钟buffer的大小。
        mMinBufferSize = AudioTrack.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat);
        Log.i(TAG, "initAudioTrack:  mMinBufferSize: " + mMinBufferSize * BUFFER_CAPITAL + " b");
        if (mMinBufferSize <= 0) {
            throw new IllegalStateException("AudioTrack is not available " + mMinBufferSize);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAudioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(mAudioFormat)
                            .setSampleRate(mSampleRateInHz)
                            .setChannelMask(mChannelConfig)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(mMinBufferSize * BUFFER_CAPITAL)
                    .build();
        } else {
            mAudioTrack = new AudioTrack(mStreamType, mSampleRateInHz, mChannelConfig,
                    mAudioFormat, mMinBufferSize * BUFFER_CAPITAL, mMode);
        }
    }


    public void addAudioPlayStateListener(IAudioPlayStateListener iAudioPlayStateListener) {
        this.iAudioPlayStateListener = iAudioPlayStateListener;
    }


    public void prepareAudioTrack() {
        bufferCount = 0;
        Log.i(TAG, "prepareAudioTrack:------> ");
        if (null == mAudioTrack) {
            return;
        }
        if (mAudioTrack.getState() == mAudioTrack.STATE_UNINITIALIZED) {
            initAudioTrack();
        }
        mAudioTrack.play();
        if (null != iAudioPlayStateListener) {
            iAudioPlayStateListener.onStart();
        }
    }

    public synchronized void writeAsync(@NonNull final byte[] bytes) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                write(bytes);
            }
        });
    }

    public synchronized void write(@NonNull final byte[] bytes) {
        if (null != mAudioTrack) {
            int byteSize = bytes.length;
            bufferCount += byteSize;
            Log.d(TAG, "write: threadName=" + Thread.currentThread().getName());
            Log.d(TAG, "write: 接收到数据 " + byteSize/1000 + " kb | 已写入 " + bufferCount/1000 + " kb");
            int write = mAudioTrack.write(bytes, 0, byteSize);
            Log.i(TAG, "write complete: 接收到数据 " + byteSize/1000 + " kb | 已写入 " + bufferCount/1000 + " kb");
            if (write == 0 && null != iAudioPlayStateListener) {
                //由于缓存的缘故，会先把缓存的bytes填满再播放，当write=0的时候存在没有播完的情况
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (iAudioPlayStateListener != null) {
                    iAudioPlayStateListener.onStop();
                }
            }
        }
    }


    public void stopPlay() {
        Log.i(TAG, "stopPlay: ");
        if (null == mAudioTrack) {
            return;
        }
        if (null != iAudioPlayStateListener) {
            iAudioPlayStateListener.onStop();
        }
        try {
            if (mAudioTrack.getPlayState() == PLAYSTATE_PLAYING) {
                mAudioTrack.stop();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "stop: " + e.toString());
            e.printStackTrace();
        }
    }

    public void release() {
        if (null == mAudioTrack) {
            return;
        }
        Log.i(TAG, "release: ");
        stopPlay();
        iAudioPlayStateListener = null;
        try {
            mAudioTrack.release();
            mAudioTrack = null;
        } catch (Exception e) {
            Log.e(TAG, "release: " + e.toString());
            e.printStackTrace();
        }
    }


    public void setBufferParams(int pcmFileSize) {
        //设置缓冲的大小 为PCM文件大小的10%
        Log.d(TAG, "setFileSize: PCM文件大小为：" + pcmFileSize + " b 最小缓存空间为 " + mMinBufferSize * BUFFER_CAPITAL + " b");
        if (pcmFileSize < mMinBufferSize * BUFFER_CAPITAL) {
            mAudioTrack = new AudioTrack(mStreamType, mSampleRateInHz, mChannelConfig,
                    mAudioFormat, mMinBufferSize, mMode);
            Log.d(TAG, "setFileSize: pcmFileSize 文件小于最小缓冲数据的10倍，修改为默认的1倍------>");
        } else {
            //缓存大小为PCM文件大小的10%，如果小于mMinBufferSize * BUFFER_CAPITAL，则按默认值设置
            int cacheFileSize = (int) (pcmFileSize * 0.1);
            int realBufferSize = (cacheFileSize / mMinBufferSize + 1) * mMinBufferSize;
            Log.d(TAG, "计算得到缓存空间为: " + realBufferSize + " b 最小缓存空间为 " + mMinBufferSize * BUFFER_CAPITAL + " b");
            if (realBufferSize < mMinBufferSize * BUFFER_CAPITAL) {
                realBufferSize = mMinBufferSize * BUFFER_CAPITAL;
            }
            mAudioTrack = new AudioTrack(mStreamType, mSampleRateInHz, mChannelConfig,
                    mAudioFormat, realBufferSize, mMode);
            Log.d(TAG, "setFileSize: 重置缓存空间为： " + realBufferSize + " b | " + realBufferSize / 1024 + " kb");
        }
        bufferCount = 0;
    }

    interface IAudioPlayStateListener {
        void onStart();

        void onStop();
    }

}