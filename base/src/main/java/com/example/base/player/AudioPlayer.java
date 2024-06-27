package com.example.base.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioPlayer {
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 2 * AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioTrack audioTrack;
    private byte[] buffer;

    public AudioPlayer() {
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE, AudioTrack.MODE_STREAM);
        buffer = new byte[BUFFER_SIZE];
    }

    public void start() {
        audioTrack.play();
    }

    public void stop() {
        audioTrack.stop();
        audioTrack.release();
    }

    public void writeData(byte[] audioData) {
        int bytesWritten = 0;
        while (bytesWritten < audioData.length) {
            int bytesRemaining = audioData.length - bytesWritten;
            int bytesToWrite = Math.min(bytesRemaining, buffer.length);
            System.arraycopy(audioData, bytesWritten, buffer, 0, bytesToWrite);
            audioTrack.write(buffer, 0, bytesToWrite);
            bytesWritten += bytesToWrite;
        }
    }
}