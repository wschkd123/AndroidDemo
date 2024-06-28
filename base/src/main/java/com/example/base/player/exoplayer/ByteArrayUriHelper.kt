package com.example.base.player.exoplayer

import android.net.Uri
import android.util.Log
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
    fun getUri(byteArray: ByteArray?): Uri {
        return try {
            val url = URL(null, "bytes:///audio", BytesHandler(byteArray))
            Uri.parse(url.toURI().toString())
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    internal inner class BytesHandler(var byteArray: ByteArray?) : URLStreamHandler() {
        override fun openConnection(u: URL): URLConnection {
            Log.e("ExoPlayer", "openConnection:$u")
            return ByteUrlConnection(u, byteArray)
        }
    }

    internal class ByteUrlConnection(url: URL?, var byteArray: ByteArray?) : URLConnection(url) {
        override fun connect() {}
        override fun getInputStream(): InputStream {
            return ByteArrayInputStream(byteArray)
        }
    }
}
