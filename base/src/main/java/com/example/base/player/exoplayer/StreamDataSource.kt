package com.example.base.player.exoplayer

import android.net.Uri
import android.system.ErrnoException
import android.system.OsConstants
import android.text.TextUtils
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Util
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource.FileDataSourceException
import androidx.media3.datasource.FileDataSource
import com.example.base.util.YWFileUtil
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile

/**
 * 支持边播边（外部）加载的数据源。
 * see [appendData]
 * 参考 [FileDataSource] 解决读写线程不安全的问题
 *
 * @author wangshichao
 * @date 2024/6/30
 */
internal class StreamDataSource(
    private val initData: ByteArray
) : BaseDataSource(false) {
    class Factory(data: ByteArray) : DataSource.Factory {

        val dataSource: StreamDataSource = StreamDataSource(data)

        override fun createDataSource(): DataSource {
            return dataSource
        }
    }

    private val TAG = "ExoPlayer-DataSource"
    private var file: RandomAccessFile? = null
    private var uri: Uri? = null
//TODO 原子性    private var bytesRemaining = AtomicLong(0L)
    private var bytesRemaining = 0L
    private var opened = false
    private var noMoreData = false

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        this.uri = uri
        transferInitializing(dataSpec)
        file = openLocalFile(uri)
        val fileLength = file!!.length()
        Log.i(TAG, "open: file.length=${fileLength}")
        // 写入初始化数据
        file!!.seek(fileLength)
        file!!.write(initData)
        Log.i(TAG, "open: file.length=${fileLength}")
        bytesRemaining = try {
            file!!.seek(dataSpec.position)
            if (dataSpec.length == C.LENGTH_UNSET.toLong()) file!!.length() - dataSpec.position else dataSpec.length
        } catch (e: IOException) {
            throw FileDataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }
        Log.w(TAG, "open: readLength=${dataSpec.position} bytesRemaining=${bytesRemaining/1000}kb")
        if (bytesRemaining < 0) {
            throw FileDataSourceException( /* message= */
                null,  /* cause= */
                null,
                PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE
            )
        }

        opened = true
        transferStarted(dataSpec)

//        return bytesRemaining
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        Log.i(TAG, "read: offset=${offset} length=${length} bytesRemaining=${bytesRemaining} noMoreData=${noMoreData}")
        if (length == 0) {
            return 0
        }
        // 没有剩余数据且写入已完成，才返回结束
        if (bytesRemaining == 0L && noMoreData) {
            return C.RESULT_END_OF_INPUT
        }
        if (bytesRemaining == 0L) {
            return 0
        }

        file!!.seek(0)
        val bytesRead: Int = try {
            Util.castNonNull(file)
                .read(buffer, offset, Math.min(bytesRemaining, length.toLong()).toInt())
        } catch (e: IOException) {
            throw FileDataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }
        if (bytesRead > 0) {
            bytesRemaining -= bytesRead.toLong()
            bytesTransferred(bytesRead)
        }
        Log.i(TAG, "read: bytesRead=${bytesRead} bytesRemaining=${bytesRemaining}b")
        //TODO bytesRead等于-1，调用close
        return bytesRead
    }

    override fun getUri(): Uri? {
        return uri
    }

    @Throws(FileDataSourceException::class)
    override fun close() {
        Log.w(TAG, "close")
        uri = null
        try {
            if (file != null) {
                file!!.close()
            }
        } catch (e: IOException) {
            throw FileDataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        } finally {
            file = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
        //TODO 原文件，close后继续播放
    }

    /**
     * 追加数据
     */
    fun appendData(newData: ByteArray) {
        val fileLength = file?.length() ?: 0
        Log.w(TAG, "appendData: fileLength=${fileLength}")
        val newLength = newData.size
        file?.apply {
            seek(fileLength)
            write(newData)
        }
//        for (newDatum in newData) {
//            data.add(newDatum)
//        }
        bytesRemaining += newLength
        Log.w(TAG, "appendData: newData=${newData.size/1000}kb bytesRemaining=${bytesRemaining/1000}kb file=${file?.length()}")
    }

    fun noMoreData() {
        noMoreData = true
        Log.w(TAG, "noMoreData")
    }

    @Throws(FileDataSourceException::class)
    private fun openLocalFile(uri: Uri): RandomAccessFile? {
        val path = uri.path ?: return null
        YWFileUtil.createNewFile(path) ?: return null
        //TODO 首次打开应该是空的
        try {
            return RandomAccessFile(Assertions.checkNotNull(path), "rw")
        } catch (e: FileNotFoundException) {
            if (!TextUtils.isEmpty(uri.query) || !TextUtils.isEmpty(uri.fragment)) {
                throw FileDataSourceException(
                    String.format(
                        "uri has query and/or fragment, which are not supported. Did you call Uri.parse()"
                                + " on a string containing '?' or '#'? Use Uri.fromFile(new File(path)) to"
                                + " avoid this. path=%s,query=%s,fragment=%s",
                        uri.path, uri.query, uri.fragment
                    ),
                    e,
                    PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK
                )
            }

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