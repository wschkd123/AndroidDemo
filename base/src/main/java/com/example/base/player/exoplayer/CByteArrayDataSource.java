package com.example.base.player.exoplayer;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.BaseDataSource;
import androidx.media3.datasource.DataSpec;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * How to play a wav file while we are filling out that file #8051
 * https://github.com/google/ExoPlayer/issues/8051
 */
public final class CByteArrayDataSource extends BaseDataSource {

    private final List<Byte> data;

    @Nullable
    private Uri uri;
    private int readPosition;
    private int bytesRemaining;
    private boolean opened;

    /**
     * @param data The data to be read.
     */
    public CByteArrayDataSource(List<Byte> data) {
        super(/* isNetwork= */ false);
        Assertions.checkNotNull(data);
        this.data = data;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        uri = dataSpec.uri;
        transferInitializing(dataSpec);
        readPosition = (int) dataSpec.position;
        bytesRemaining = (int) ((data.size() - dataSpec.position));
        opened = true;
        transferStarted(dataSpec);
        return C.LENGTH_UNSET;
    }

    @Override
    public int read(@NotNull byte[] buffer, int offset, int readLength) {

        if (readLength == 0 || bytesRemaining == 0)
            return C.RESULT_NOTHING_READ;

        readLength = Math.min(readLength, bytesRemaining);
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

    public void increaseBytesRemaining(int x) {
        bytesRemaining += x;
    }
}