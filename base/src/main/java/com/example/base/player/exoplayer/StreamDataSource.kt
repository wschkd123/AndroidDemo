package com.example.base.player.exoplayer

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 支持边播边（外部）加载的数据源。参考 [ByteArrayDataSource]
 *
 * 1. 使用字节数组 [data] 读写数据
 * 2. 读 [read] 和写 [appendData] 在不同的线程，需要保证线程安全
 *
 * @author wangshichao
 * @date 2024/6/30
 */
internal class StreamDataSource(
    initData: ByteArray
) : BaseDataSource(false) {
    class Factory(byteArray: ByteArray, var listener: TransferListener? = null) : DataSource.Factory {
        val dataSource: StreamDataSource

        init {
            dataSource = StreamDataSource(byteArray)
        }

        override fun createDataSource(): DataSource {
            listener?.let {
                dataSource.addTransferListener(it)
            }
            return dataSource
        }
    }

    private val TAG = "Stream-ExoPlayer"
    private var uri: Uri? = null
    private var readPosition = 0
    private var bytesRemaining = AtomicLong(0L)
    private var opened = false
    private var noMoreData = AtomicBoolean(false)
    private val lock = Object()
    private var data: ByteArray = ByteArray(0)
    private val appendExecutor = Executors.newSingleThreadExecutor()

    init {
        data = initData
        bytesRemaining.set(data.size.toLong())
    }

    /**
     * 打开数据源以读取指定的数据
     */
    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        readPosition = dataSpec.position.toInt()
        synchronized(lock) {
            if (dataSpec.position > data.size) {
                throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
            }
            bytesRemaining.set(data.size - dataSpec.position)
        }
        if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            bytesRemaining.set(Math.min(bytesRemaining.get(), dataSpec.length))
        }
        Log.w(TAG, "open: readPosition=${readPosition} bytesRemaining=$bytesRemaining")
        opened = true
        transferStarted(dataSpec)
        return C.LENGTH_UNSET.toLong()
    }

    /**
     * 从输入中读取最多length字节的数据，从buffer的offset位置开始填充length长度。
     */
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        Log.i(TAG, "read: offset=$offset readLength=$length bytesRemaining=$bytesRemaining noMoreData=$noMoreData")
        val startTime = System.currentTimeMillis()
        var readLength = length
        if (readLength == 0) {
            return 0
        }
        // 没有可用数据且没有更多数据加载，输入结束
        if (bytesRemaining.get() == 0L && noMoreData.get()) {
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRemaining.get() == 0L) {
            return 0
        }

        // 从buffer的offset位置开始填充readLength长度的数据
        synchronized(lock) {
            readLength = Math.min(readLength, bytesRemaining.get().toInt())
            System.arraycopy(data, readPosition, buffer, offset, readLength)
            // 更新可用数据
            readPosition += readLength
            bytesRemaining.set(bytesRemaining.get() - readLength)
        }
        bytesTransferred(readLength)
        Log.i(TAG, "read: readPosition=${readPosition} bytesRemaining:${bytesRemaining} cost=${System.currentTimeMillis() - startTime}")
        return readLength
    }

    override fun getUri(): Uri? {
        return uri
    }

    /**
     * 关闭源
     */
    override fun close() {
        Log.w(TAG, "close")
        if (opened) {
            opened = false
            transferEnded()
        }
        uri = null
    }

    fun appendDataAsync(newData: ByteArray) {
        appendExecutor.execute {
            appendData(newData)
        }
    }

    /**
     * 追加数据
     *
     * 1. 可能在[open]之前执行
     * 2. 注意多线程同步
     */
    private fun appendData(newData: ByteArray) {
        val newLength = newData.size
        Log.i(TAG, "appendData: newData=${newLength} bytesRemaining=$bytesRemaining}")
        val startTime = System.currentTimeMillis()
        synchronized(lock) {
            val lastData = data
            data = lastData.plus(newData)
            bytesRemaining.set(bytesRemaining.get() + newLength)
        }
        Log.w(TAG, "appendData: newData=${newLength} bytesRemaining=$bytesRemaining cost=${System.currentTimeMillis() - startTime}")
    }

    fun noMoreData() {
        noMoreData.set(true)
        Log.w(TAG, "noMoreData")
    }
}