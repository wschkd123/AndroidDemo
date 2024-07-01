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
import androidx.media3.datasource.TransferListener
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
    val data: MutableList<Byte>
) : BaseDataSource(false) {
    class Factory(data: ByteArray, var listener: TransferListener? = null) : DataSource.Factory {
        val dataSource: StreamDataSource

        init {
            val list: MutableList<Byte> = mutableListOf()
            for (b in data) {
                list.add(b)
            }
            dataSource = StreamDataSource(list)
        }

        override fun createDataSource(): DataSource {
            listener?.let {
                dataSource.addTransferListener(it)
            }
            return dataSource
        }
    }

    private val TAG = "ExoPlayer-DataSource"
    private var file: RandomAccessFile? = null
    private var uri: Uri? = null
//    private var readPosition = 0
    private var bytesRemaining = 0L
    private var opened = false
    private var noMoreData = false

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        this.uri = uri
        transferInitializing(dataSpec)
        file = openLocalFile(uri)
        bytesRemaining = try {
            file!!.seek(dataSpec.position)
            if (dataSpec.length == C.LENGTH_UNSET.toLong()) file!!.length() - dataSpec.position else dataSpec.length
        } catch (e: IOException) {
            throw FileDataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }
        if (bytesRemaining < 0) {
            throw FileDataSourceException( /* message= */
                null,  /* cause= */
                null,
                PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE
            )
        }

        opened = true
        transferStarted(dataSpec)

        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return if (length == 0) {
            0
        } else if (bytesRemaining == 0L) {
            C.RESULT_END_OF_INPUT
        } else {
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
            bytesRead
        }
    }

    override fun getUri(): Uri? {
        return uri
    }

    @Throws(FileDataSourceException::class)
    override fun close() {
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
        Log.w(TAG,
            "appendBytes: newData=${newData.size/1000}kb bytesRemaining=${bytesRemaining/1000}kb dataLength=${data.size / 1000}kb"
        )
    }

    fun noMoreData() {
        noMoreData = true
        Log.w(TAG, "noMoreData")
    }

    @Throws(FileDataSourceException::class)
    private fun openLocalFile(uri: Uri): RandomAccessFile? {
        try {
            return RandomAccessFile(Assertions.checkNotNull(uri.path), "r")
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