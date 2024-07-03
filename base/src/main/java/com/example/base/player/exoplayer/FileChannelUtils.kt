package com.example.base.player.exoplayer

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

object FileChannelUtils {
    fun read(channel: FileChannel?, length: Int): ByteArray {
        channel ?: return ByteArray(0)
        try {
            val buffer = ByteBuffer.allocate(length) // 创建缓冲区
            val bytesRead = channel.read(buffer) // 从文件通道读取数据到缓冲区
            if (bytesRead > 0) {
                buffer.flip() // 切换缓冲区为读模式
                val data = ByteArray(bytesRead)
                buffer[data] // 从缓冲区读取数据
                return data
            }
            return ByteArray(0)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ByteArray(0)
    }

    fun write(channel: FileChannel?, data: ByteArray?): Int {
        channel ?: return 0
        data ?: return 0
        try {
            val buffer = ByteBuffer.wrap(data) // 包装字节数组为缓冲区
            return channel.write(buffer) // 将缓冲区中的数据写入文件通道
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return 0
    }
}