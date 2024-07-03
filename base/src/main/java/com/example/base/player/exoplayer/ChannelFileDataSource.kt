package com.example.base.player.exoplayer

import android.net.Uri
import android.system.ErrnoException
import android.system.OsConstants
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.Assertions
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.FileDataSource.FileDataSourceException
import androidx.media3.datasource.TransferListener
import com.example.base.util.YWFileUtil
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


/**
 * 支持边播边（外部）加载的数据源。参考 [FileDataSource]
 *
 * 1. 使用 FileChannel 读写数据
 * 2. 读 [read] 和写 [appendData] 在不同的线程，需要保证线程安全
 *
 * @author wangshichao
 * @date 2024/6/30
 */
internal class ChannelFileDataSource(
    path: String,
    initData: ByteArray,
): BaseDataSource(false) {
    class Factory(path: String, byteArray: ByteArray, var listener: TransferListener? = null) : DataSource.Factory {
        val dataSource = ChannelFileDataSource(path, byteArray)

        override fun createDataSource(): DataSource {
            listener?.let {
                dataSource.addTransferListener(it)
            }
            return dataSource
        }
    }

    private val TAG = "ExoPlayer-DataSource"
    private var file: RandomAccessFile? = null
    private var fileChannel: FileChannel?= null
    private var uri: Uri? = null
    private var readPosition = 0L
    private val bytesRemaining = AtomicLong(0L)
    private var opened = false
    private val noMoreData = AtomicBoolean(false)

    init {
        initFileWithData(path, initData)
        bytesRemaining.set(initData.size.toLong())
    }

    /**
     * 打开数据源以读取指定的数据
     */
    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        readPosition = dataSpec.position
        Log.w(TAG, "open: readPosition=${readPosition} bytesRemaining=${bytesRemaining}")
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

        // 从上次读取文件位置开始
        fileChannel?.position(readPosition)

        // 从ChannelFile中读取readLength长度的数据填充到buffer中
        readLength = Math.min(readLength.toLong(), bytesRemaining.get()).toInt()
        if (readLength > 0) {
            val readData = FileChannelUtils.read(fileChannel, readLength)
            if (readData.size < readLength) {
                readLength = readData.size
                Log.e(TAG, "read: error readLength=$readLength")
            }
            System.arraycopy(readData, 0, buffer, offset, readLength)

            // 更新可用数据
            readPosition += readLength
            bytesRemaining.set(bytesRemaining.get() - readLength)
            bytesTransferred(readLength)
        }
        Log.i(TAG, "read: readPosition=${readPosition} bytesRemaining:${bytesRemaining} cost=${System.currentTimeMillis() - startTime}")
        return readLength
    }

    override fun getUri(): Uri? {
        return uri
    }

    /**
     * 关闭源
     */
    @Throws(FileDataSourceException::class)
    override fun close() {
        Log.w(TAG, "close")
        uri = null
        try {
            fileChannel?.close()
            file?.close()
        } catch (e: IOException) {
            throw FileDataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        } finally {
            file = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    /**
     * 追加数据
     *
     * 1. 可能在[open]之前执行
     * 2. 注意多线程同步
     */
    fun appendData(newData: ByteArray) {
        val newLength = newData.size
        Log.i(TAG, "appendData: newData=${newLength} bytesRemaining=$bytesRemaining}")
        val startTime = System.currentTimeMillis()

        // 文件位置移到末尾，并写入新数据
        fileChannel?.position(fileChannel?.size() ?: 0)
        FileChannelUtils.write(fileChannel, newData)

        bytesRemaining.set(bytesRemaining.get() + newLength)
        Log.w(TAG, "appendData: newData=${newLength} bytesRemaining=$bytesRemaining file=${fileChannel?.size()} cost=${System.currentTimeMillis() - startTime}")
    }

    fun noMoreData() {
        noMoreData.set(true)
        Log.w(TAG, "noMoreData")
    }

    /**
     * 初始化文件并写入数据
     */
    @Throws(FileDataSourceException::class)
    private fun initFileWithData(path: String?, byteArray: ByteArray) {
        if (file != null) {
            Log.e(TAG, "initFileWithData: file is init")
            return
        }
        YWFileUtil.createNewFile(path) ?: return
        try {
            file = RandomAccessFile(Assertions.checkNotNull(path), "rw")
            fileChannel = file!!.channel
            val startTime = System.currentTimeMillis()
            FileChannelUtils.write(fileChannel, byteArray)
            Log.w(TAG, "initFileWithData: write=${fileChannel!!.size()} cost=${System.currentTimeMillis() - startTime}")
        } catch (e: FileNotFoundException) {
            throw FileDataSourceException(
                e,
                if (isPermissionError(e.cause)) PlaybackException.ERROR_CODE_IO_NO_PERMISSION else PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
            )
        } catch (e: SecurityException) {
            throw FileDataSourceException(e, PlaybackException.ERROR_CODE_IO_NO_PERMISSION)
        } catch (e: RuntimeException) {
            throw FileDataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }
    }

    private fun isPermissionError(e: Throwable?): Boolean {
        return e is ErrnoException && e.errno == OsConstants.EACCES
    }

}