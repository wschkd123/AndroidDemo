package com.example.base.player.exoplayer;

import static java.lang.Math.min;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.datasource.BaseDataSource;
import androidx.media3.datasource.ByteArrayDataSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceException;
import androidx.media3.datasource.DataSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  参考 {@link ByteArrayDataSource}
 */
public final class StreamDataSource extends BaseDataSource {

    public static final class Factory implements DataSource.Factory {
        private final @NonNull StreamDataSource dataSource;

        public Factory(byte[] data) {
            List<Byte> list = new ArrayList<>();
            for (byte b: data) {
                list.add(b);
            }
            dataSource = new StreamDataSource(list);
        }

        @NonNull
        @Override
        public DataSource createDataSource() {
            return dataSource;
        }

        @NonNull
        public StreamDataSource getDataSource() {
            return dataSource;
        }
    }

    private final List<Byte> data = Collections.synchronizedList(new ArrayList<Byte>());

    @Nullable
    private Uri uri;
    private int readPosition;
    private int bytesRemaining;
    private boolean opened;

    /**
     * @param data The data to be read.
     */
    public StreamDataSource(List<Byte> data) {
        super(false);
        this.data.addAll(data);
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        uri = dataSpec.uri;
        transferInitializing(dataSpec);
        if (dataSpec.position > data.size()) {
            throw new DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE);
        }
        readPosition = (int) dataSpec.position;
        bytesRemaining = data.size() - (int) dataSpec.position;
        Log.w("ExoPlayer", "open: readLength=" + dataSpec.position + " bytesRemaining=" + bytesRemaining);
        if (dataSpec.length != C.LENGTH_UNSET) {
            Log.e("ExoPlayer", "open: readLength=" + dataSpec.position + " bytesRemaining=" + bytesRemaining);
            bytesRemaining = (int) min(bytesRemaining, dataSpec.length);
        }
        opened = true;
        transferStarted(dataSpec);
        return C.LENGTH_UNSET;
    }

    @Override
    public int read(@NonNull byte[] buffer, int offset, int readLength) {
        Log.i("ExoPlayer", "read: offset=" + offset + " readLength=" + readLength + " bytesRemaining=" + bytesRemaining);
        if (readLength == 0) {
            return 0;
        }
        if (bytesRemaining == 0) {
            //TODO 处理写入慢的问题
            return C.RESULT_END_OF_INPUT;
        }

        readLength = min(readLength, bytesRemaining);
        Log.i("ExoPlayer", "read: dataLength=" + data.size() + " bufferLength:" + buffer.length);
        for (int i = 0; i < readLength; i++) {
            buffer[offset + i] = data.get(readPosition + i);
        }
        readPosition += readLength;
        bytesRemaining -= readLength;
        bytesTransferred(readLength);
        return readLength;
    }

    @Override
    @Nullable
    public Uri getUri() {
        return uri;
    }

    @Override
    public void close() {
        if (opened) {
            opened = false;
            transferEnded();
        }
        uri = null;
    }

    /**
     * 追加数据
     */
    public void appendBytes(byte[] newData) {
        int newLength = newData.length;
        for (byte newDatum : newData) {
            data.add(newDatum);
        }
        bytesRemaining += newLength;
        Log.w("ExoPlayer", "appendBytes: newData=" + newData.length + " bytesRemaining=" + bytesRemaining);
    }
}