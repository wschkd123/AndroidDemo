package com.example.base.player.exoplayer

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import java.io.IOException

/**
 * 支持边播边（外部）加载的数据源。
 * see [appendData]
 *
 * @author wangshichao
 * @date 2024/6/30
 */
internal class StreamDataSource(
    val data: MutableList<Byte>
) : BaseDataSource(false) {
    class Factory(data: ByteArray) : DataSource.Factory {
        val dataSource: StreamDataSource

        init {
            val list: MutableList<Byte> = mutableListOf()
            for (b in data) {
                list.add(b)
            }
            dataSource = StreamDataSource(list)
        }

        override fun createDataSource(): DataSource {
            return dataSource
        }
    }

    private val TAG = "ExoPlayer-DataSource"
    private var uri: Uri? = null
    private var readPosition = 0
    private var bytesRemaining = 0
    private var opened = false
    private var noMoreData = false

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        if (dataSpec.position > data.size) {
            throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
        }
        readPosition = dataSpec.position.toInt()
        bytesRemaining = data.size - dataSpec.position.toInt()
        Log.w(
            TAG, "open: readLength=" + dataSpec.position + " bytesRemaining=" + bytesRemaining
        )
        if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            bytesRemaining = Math.min(bytesRemaining.toLong(), dataSpec.length).toInt()
        }
        opened = true
        transferStarted(dataSpec)
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        var readLength = length
        Log.i(
            TAG,
            "read: offset=$offset readLength=$readLength bytesRemaining=$bytesRemaining noMoreData=$noMoreData"
        )
        if (readLength == 0) {
            return 0
        }
        // 没有剩余数据且写入已完成，才返回结束
        if (bytesRemaining == 0 && noMoreData) {
            return C.RESULT_END_OF_INPUT
        }
        readLength = Math.min(readLength, bytesRemaining)
        Log.i(TAG, "read: dataLength=" + data.size + " bufferLength:" + buffer.size)
        for (i in 0 until readLength) {
            buffer[offset + i] = data[readPosition + i]
        }
        readPosition += readLength
        bytesRemaining -= readLength
        bytesTransferred(readLength)
        return readLength
    }

    override fun getUri(): Uri? {
        return uri
    }

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
        uri = null
    }

    /**
     * 追加数据
     */
    fun appendData(newData: ByteArray) {
        val newLength = newData.size
        for (newDatum in newData) {
            data.add(newDatum)
        }
        bytesRemaining += newLength
        Log.w(TAG, "appendBytes: newData=" + newData.size + " bytesRemaining=" + bytesRemaining)
    }

    fun noMoreData() {
        noMoreData = true
        Log.w(TAG, "noMoreData")
    }
}