package com.example.base.player.exoplayer

import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

/**
 * 字节数组uri辅助类。用于支持ExoPlayer播放字节数组格式数据
 *
 * @author wangshichao
 * @date 2024/6/28
 */
class ByteArrayUriHelper {
    fun getUri(): Uri {
        return try {
            val url = URL(null, "bytes:///audio", BytesHandler())
            Uri.parse(url.toURI().toString())
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    internal inner class BytesHandler : URLStreamHandler() {
        override fun openConnection(u: URL): URLConnection {
            return ByteUrlConnection(u)
        }
    }

    internal class ByteUrlConnection(url: URL?) : URLConnection(url) {
        override fun connect() {}
        override fun getInputStream(): InputStream {
            return ByteArrayInputStream(ByteArray(0))
        }
    }
}
