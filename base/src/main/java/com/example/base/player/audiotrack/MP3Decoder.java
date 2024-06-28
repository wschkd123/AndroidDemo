package com.example.base.player.audiotrack;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MP3Decoder {
    private static final String TAG = "MP3Decoder-ExoPlayer";
    public static byte[] decodeMP3(byte[] mp3Data) {
        try {
            long startTime = System.currentTimeMillis();
            // 创建临时文件
            File tempFile = File.createTempFile("temp", ".mp3");
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            fileOutputStream.write(mp3Data);
            fileOutputStream.close();

            // MediaExtractor 提取音频数据
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(tempFile.getPath());
            int trackIndex = selectTrack(extractor);
            extractor.selectTrack(trackIndex);
            MediaFormat format = extractor.getTrackFormat(trackIndex);
            String mime = format.getString(MediaFormat.KEY_MIME);
            //TODO 采样率给播放器
            Log.i(TAG, "decodeMP3: format=" + format);
            if (!MediaFormat.MIMETYPE_AUDIO_MPEG.equals(mime)) {
                Log.w(TAG, "decodeMP3: not support " + mime);
                return mp3Data;
            }

            // MediaCodec 解码mp3
            MediaCodec codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();

            ByteBuffer[] inputBuffers = codec.getInputBuffers();
            ByteBuffer[] outputBuffers = codec.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            boolean isEOS = false;
            long presentationTimeUs = 0;

            while (!isEOS) {
                int inputBufferIndex = codec.dequeueInputBuffer(10000);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }

                int outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
                if (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    byte[] chunk = new byte[info.size];
                    outputBuffer.get(chunk);
                    outputBuffer.clear();
                    outputStream.write(chunk);
                    codec.releaseOutputBuffer(outputBufferIndex, false);
                }

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isEOS = true;
                }
            }

            codec.stop();
            codec.release();
            extractor.release();
            tempFile.delete(); // 删除临时文件
            outputStream.close();
            Log.w(TAG, "decodeMP3: cost time=" + (System.currentTimeMillis() - startTime));
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                Log.i(TAG, "selectTrack: mime=" + mime);
                return i;
            }
        }
        return -1;
    }
}