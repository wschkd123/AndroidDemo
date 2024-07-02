package com.example.base.player.exoplayer

import android.net.Uri
import android.system.ErrnoException
import android.system.OsConstants
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Util
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.FileDataSource.FileDataSourceException
import com.example.base.util.YWFileUtil
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


/**
 * 支持边播边（外部）加载的数据源。
 * see [appendData]
 * 参考 [FileDataSource] 解决读写线程不安全的问题
 *
 * @author wangshichao
 * @date 2024/6/30
 */
internal class ChannelFileDataSource: BaseDataSource(false) {
    class Factory : DataSource.Factory {
        val dataSource = ChannelFileDataSource()
        override fun createDataSource(): DataSource {
            return dataSource
        }
    }

    private val TAG = "ExoPlayer-DataSource"
    private var file: RandomAccessFile? = null
    private var fileChannel: FileChannel?= null
    private var uri: Uri? = null
    private var bytesRemaining = 0L
    private var readPosition = 0L
    private var opened = false
    private var noMoreData = false

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        this.uri = uri
        transferInitializing(dataSpec)
        Log.w(TAG, "open: readLength=${dataSpec.position} bytesRemaining=${bytesRemaining}b")
        opened = true
        transferStarted(dataSpec)
//        return bytesRemaining
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        Log.i(TAG, "read: offset=${offset} length=${length} bytesRemaining=${bytesRemaining} noMoreData=${noMoreData}")
        val startTime = System.currentTimeMillis()
        var readLength = length
        if (readLength == 0) {
            return 0
        }
        // 没有剩余数据且写入已完成，才返回结束
        if (bytesRemaining == 0L && noMoreData) {
            return C.RESULT_END_OF_INPUT
        }
        if (bytesRemaining == 0L) {
            return 0
        }

        // 上次读取位置
        fileChannel?.position(readPosition)

        // 读取channel数据后写入
        readLength = Math.min(readLength, bytesRemaining.toInt())
        if (readLength > 0) {
            val readData = FileChannelUtils.readFileChannel(fileChannel, 0, readLength)
            Log.i(TAG, "read: readDataLength=${readData.size}")
            System.arraycopy(readData, 0, buffer, offset, readLength)
            readPosition += readLength.toLong()
            bytesRemaining -= readLength.toLong()
            bytesTransferred(readLength)
        }
        Log.i(TAG, "read: readLength=${readLength} bytesRemaining:${bytesRemaining} cost=${System.currentTimeMillis() - startTime}")
        return readLength
    }

    override fun getUri(): Uri? {
        return uri
    }

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
     */
    fun appendData(newData: ByteArray, path: String) {
        initFileWriteData(path, newData)
        val channelSize = fileChannel?.size() ?: 0
        val newLength = newData.size
        Log.w(TAG, "appendData: channelSize=${channelSize}")
        // 移动指针末尾，并写入新数据
        fileChannel?.position(channelSize)
        val buffer = ByteBuffer.wrap(newData)
        fileChannel?.write(buffer) ?: 0L
        bytesRemaining += newLength
        Log.w(TAG, "appendData: newData=${newData.size}b bytesRemaining=${bytesRemaining}b file=${fileChannel?.size()}")
    }

    fun noMoreData() {
        noMoreData = true
        Log.w(TAG, "noMoreData")
    }

    @Throws(FileDataSourceException::class)
    private fun initFileWriteData(path: String?, byteArray: ByteArray) {
        if (file != null) {
            Log.i(TAG, "openLocalFile: file is init")
            return
        }
        YWFileUtil.createNewFile(path) ?: return
        try {
            file = RandomAccessFile(Assertions.checkNotNull(path), "rw")
            fileChannel = file!!.channel
            Log.i(TAG, "openLocalFile: fileChannelLength=${fileChannel!!.size()}")
            // 写入初始化数据
            val buffer = ByteBuffer.wrap(byteArray)
            fileChannel?.write(buffer) ?: 0L
            Log.i(TAG, "openLocalFile: write fileChannelLength=${fileChannel!!.size()}")
        } catch (e: FileNotFoundException) {
            throw FileDataSourceException(
                e,
                if (Util.SDK_INT >= 21 && isPermissionError(e.cause)) PlaybackException.ERROR_CODE_IO_NO_PERMISSION else PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
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