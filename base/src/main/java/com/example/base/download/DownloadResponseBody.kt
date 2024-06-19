package com.example.base.download

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Okio
import okio.Source
import java.io.IOException

/**
 * 自定义ResponseBody，包含进度
 *
 * @author wangshichao
 * @date 2024/6/19
 */
internal class DownloadResponseBody(
    private val responseBody: ResponseBody,
    private val downloadListener: FileDownloadListener?,
    private val url: String
) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null
    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()))
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L
            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                val done = bytesRead == -1L
                downloadListener?.onProgress(url, totalBytesRead, responseBody.contentLength(), done)
                return bytesRead
            }
        }
    }
}