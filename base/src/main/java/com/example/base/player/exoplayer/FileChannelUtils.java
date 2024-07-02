package com.example.base.player.exoplayer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelUtils {
    public static byte[] readFileChannel(FileChannel channel, int length) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(length); // 创建缓冲区
            int bytesRead = channel.read(buffer); // 从文件通道读取数据到缓冲区
            if (bytesRead > 0) {
                buffer.flip(); // 切换缓冲区为读模式
                byte[] data = new byte[bytesRead];
                buffer.get(data); // 从缓冲区读取数据
                return data;
            }
            return new byte[0]; // 如果没有读取到数据，则返回空数组
        } catch (IOException e) {
            // 处理异常，可以进行日志记录或其他操作
            e.printStackTrace();
        }

        return new byte[0]; // 发生异常时返回空数组
    }
}