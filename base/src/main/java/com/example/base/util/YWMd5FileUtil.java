package com.example.base.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * 计算单一文件MD5值
 *
 * @author River
 */
public class YWMd5FileUtil {

    public static String getMd5ByFile(File file) {
        FileInputStream fis = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            byte[] result = digest.digest();
            StringBuffer sb = new StringBuffer();
            for (byte b : result) {
                // 与运算
                int number = b & 0xff;
                String str = Integer.toHexString(number);
                if (str.length() == 1) {
                    sb.append("0");
                }
                sb.append(str);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getMd5(InputStream inputStream) {
        MessageDigest md = null;
        BufferedInputStream in = null;
        try {
            md = MessageDigest.getInstance("MD5");
            in = new BufferedInputStream(inputStream);
            byte[] bytes = new byte[8192];
            int byteCount;
            while ((byteCount = in.read(bytes)) > 0) {
                md.update(bytes, 0, byteCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (md == null) {
            return "";
        }
        return byte2hex(md.digest()).toLowerCase();
    }

    /**
     * 计算string的MD5
     *
     * @param str
     * @return
     */
    public static String getMD5(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return byte2hex(md5.digest(str.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }
}
