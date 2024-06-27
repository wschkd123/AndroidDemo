package com.example.base.player.audiotrack;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

/**
 * @author wangshichao
 * @date 2024/6/27
 */
public class AudioTrackerWrapper {

    public static final String TAG = "AudioTrackerWrapper";
    private AudioTracker audioTracker = new AudioTracker();

    /**
     * 解码并播放mp3字节数组
     */
    public void startPlay(byte[] originByte) {
        byte[] decodeArray = MP3Decoder.decodeMP3(originByte);
        playDecodeAudio(decodeArray);
    }

    public void appendPlay(byte[] originByte) {
        byte[] decodeArray = MP3Decoder.decodeMP3(originByte);
        audioTracker.appendAudioData(decodeArray);
    }

    public void playDecodeAudio(byte[] byteArray) {
        audioTracker.createAudioTrack();
        audioTracker.setAudioPlayListener(new AudioTracker.AudioPlayListener() {
            public void onStart() {
                Log.w(TAG, "播放开始");
            }

            public void onStop() {
                audioTracker.release();
                Log.w(TAG, "播放完成");
            }

            public void onError(@NotNull final String message) {
                Log.e(TAG, "播放错误 " + message);
            }
        });
        audioTracker.start(byteArray);
    }
}
