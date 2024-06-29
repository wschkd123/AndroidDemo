package com.example.beyond.demo.ui.tts;

import android.util.Base64;

/**
 * @author wangshichao
 * @date 2024/6/29
 */
public class TTSUtil {
    // 将十六进制字符串转换为字节数组
    public static final byte[] decodeHexString(String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }

        return byteArray;
    }

    public static byte[] hexToBytes(String hex) {
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < data.length; i++) {
            int index = i * 2;
            String byteString = hex.substring(index, index + 2);
            data[i] = (byte) Integer.parseInt(byteString, 16);
        }
        return data;
    }

    public static byte[] hexToBytesUsingBase64(String hex) {
        byte[] data = hexToBytes(hex);
        String base64 = Base64.encodeToString(data, Base64.DEFAULT);
        return Base64.decode(base64, Base64.DEFAULT);
    }

}
