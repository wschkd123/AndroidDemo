package com.example.base.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.net.URL;
import java.net.URLDecoder;

/**
 * 网络相关工具集
 */
public class YWNetUtil {

    /**
     * 检查当前网络是否可用 <b/>
     * 表明网络连接是否posible，posible可能的意思，而并不是已经连接
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connMgr.getActiveNetworkInfo();
        return info != null && info.isAvailable();
    }

    /**
     * 表明网络连接是否存在并且可以传递数据。
     *
     * @param context
     */
    public static boolean isNetworkConnected(Context context) {
        if (context == null) {
            return false;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) context
                    .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 是否是WIFi状态
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager connMgr = (ConnectivityManager) context
                .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
        return activeNetworkInfo != null
                && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;

    }

    /**
     * 是否是数据状态
     */
    public static boolean isMobileConnected(Context context) {
        if (context == null) {
            return false;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) context
                    .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.getType() == ConnectivityManager.TYPE_MOBILE) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static final String TAG_NETWORK_STATUS = "NETWORK_STATUS";
    // 网络类型 2G 3G WIFI
    private static int networkType = 0;
    public static final int NETWORK_TYPE_UNKNOWN = 0;
    public static final int NET_TYPE_WIFI = 1;
    public static final int NET_TYPE_2G = 2;
    public static final int NET_TYPE_3G = 3;
    public static final int NET_TYPE_4G = 4;

    /**
     * 获取当前的网络状态，中国特色
     *
     * @param context 上下文
     * @return APN 类型
     * {@link #NETWORK_TYPE_UNKNOWN}
     * {@link #NET_TYPE_WIFI},
     * {@link #NET_TYPE_2G},
     * {@link #NET_TYPE_3G} or
     * {@link #NET_TYPE_4G}.
     */
    public static int getAPNType(Context context) {
        //结果返回值
        int netType = NETWORK_TYPE_UNKNOWN;
        try {
            //获取手机所有连接管理对象
            ConnectivityManager manager = (ConnectivityManager) context
                    .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            //获取NetworkInfo对象
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            //NetworkInfo对象为空 则代表没有网络
            if (networkInfo == null) {
                return netType;
            }
            //否则 NetworkInfo对象不为空 则获取该networkInfo的类型
            int nType = networkInfo.getType();
            if (nType == ConnectivityManager.TYPE_WIFI) {
                //WIFI
                netType = NET_TYPE_WIFI;
            } else if (nType == ConnectivityManager.TYPE_MOBILE) {
                int nSubType = networkInfo.getSubtype();
                TelephonyManager telephonyManager = (TelephonyManager) context.
                        getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager.isNetworkRoaming()) {
                    netType = NET_TYPE_2G;
                } else {
                    switch (nSubType) {
                        case TelephonyManager.NETWORK_TYPE_LTE:
                            netType = NET_TYPE_4G;
                            break;
                        case TelephonyManager.NETWORK_TYPE_UMTS:
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_EVDO_A:
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                            //3G   联通的3G为UMTS或HSDPA 电信的3G为EVDO
                            netType = NET_TYPE_3G;
                            break;
                        case TelephonyManager.NETWORK_TYPE_GPRS:
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                        case TelephonyManager.NETWORK_TYPE_CDMA:
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                        case TelephonyManager.NETWORK_TYPE_IDEN:
                            //2G 移动和联通的2G为GPRS或EGDE，电信的2G为CDMA
                            netType = NET_TYPE_2G;
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return netType;
    }

    /**
     * APN Type 文字
     */
    public static String getAPNTypeStr(Context context) {
        return netType2String(getAPNType(context));
    }

    private static String netType2String(int networkType) {
        String netWorkType = "";
        switch (networkType) {
            case NETWORK_TYPE_UNKNOWN:
                netWorkType = "unknow";
                break;
            case NET_TYPE_WIFI:
                netWorkType = "wifi";
                break;
            case NET_TYPE_2G:
                netWorkType = "2g";
                break;
            case NET_TYPE_3G:
                netWorkType = "3g";
                break;
            case NET_TYPE_4G:
                netWorkType = "4g";
                break;
        }
        return netWorkType;
    }

    /**
     * 获取IP
     */
//    public static String getIPAddress(Context context) {
//        NetworkInfo info = ((ConnectivityManager) context
//                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
//        if (info != null && info.isConnected()) {
//            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
//                try {
//                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
//                    for (Enumeration<NetworkInterface> en = NetworkInterface
//                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
//                        NetworkInterface intf = en.nextElement();
//                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
//                        enumIpAddr.hasMoreElements(); ) {
//                            InetAddress inetAddress = enumIpAddr.nextElement();
//                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof
//                            Inet4Address) {
//                                return inetAddress.getHostAddress();
//                            }
//                        }
//                    }
//                } catch (SocketException e) {
//                    e.printStackTrace();
//                }
//
//            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
//                WifiManager wifiManager = (WifiManager) context.getSystemService(Context
//                .WIFI_SERVICE);
//                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//得到IPV4地址
//                return ipAddress;
//            }
//        } else {
//            //当前无网络连接,请在设置中打开网络
//        }
//        return null;
//    }

    /**
     * 将得到的int类型的IP转换为String类型
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    /**
     * 支持的url scheme
     */
    public static final String[] URL_SUPPORT_SCHEME = {"http://", "https://"};

    public static boolean isHttps(String url) {
        if (!TextUtils.isEmpty(url)) {
            return url.startsWith("https");
        }
        return false;
    }

    /**
     * 是否支持此url
     *
     * @param url
     * @return
     */
    public static boolean isSupportedNetURL(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        url = url.toLowerCase().trim();
        for (String scheme : URL_SUPPORT_SCHEME) {
            if (url.startsWith(scheme)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 后端定义的网络类型
     */
    private static final String QR_NET_STATUS_UNKNOWN = "-1";
    private static final String QR_NET_STATUS_UNREACHABLE = "0";
    private static final String QR_NET_STATUS_WIFI = "1";
    private static final String QR_NET_STATUS_WWAN = "2";
    private static final String QR_NET_STATUS_2G = "3";
    private static final String QR_NET_STATUS_3G = "4";
    private static final String QR_NET_STATUS_4G = "5";
    private static final String QR_NET_STATUS_XG = "6";

    /**
     * 获取header中的net_type字段
     */
    public static String getHeaderNetType(Context c) {
        int apnState = YWNetUtil.getAPNType(c);
        switch (apnState) {
            case NET_TYPE_WIFI: {
                return QR_NET_STATUS_WIFI;
            }
            case NET_TYPE_2G: {
                return QR_NET_STATUS_2G;
            }
            case NET_TYPE_3G: {
                return QR_NET_STATUS_3G;
            }
            case NET_TYPE_4G: {
                return QR_NET_STATUS_4G;
            }
            default: {
                return QR_NET_STATUS_UNKNOWN;
            }
        }
    }

    public static String replaceHttpWithHttps(String url) {
        if (url != null && url.toLowerCase().startsWith("http://")) {
            return url.replace("http://", "https://");
        }
        return url;
    }


    public static @Nullable String replaceHostInUrl(String url, String newHost) {
        try {
            // 获取 URL 对象和 host 验证器
            String originalHost = new URL(url).getHost();
            // 如果新 host 和原 host 相同，直接返回原 URL
            if (originalHost.equals(newHost)) {
                return url;
            }
            return url.replaceAll(originalHost, newHost);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取某个url传递的参数值
     */
    public static String getMatchUrlParameter(String httpUrl, String key) {
        try {
            URL url = new URL(httpUrl);
            String query = url.getQuery();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (key != null && pair != null && key
                        .equalsIgnoreCase(URLDecoder.decode(pair.substring(0, idx)))) {
                    return URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                }
            }
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }

        return "";
    }

    public static boolean isBadNetwork(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {// 有网时，请求网络数据
            NetworkInfo[] networkInfos = connectivityManager
                    .getAllNetworkInfo();
            for (int i = 0; i < networkInfos.length; i++) {
                State state = networkInfos[i].getState();
                if (State.CONNECTED == state) {
                    break;
                }
            }
            NetworkInfo activeInfo = connectivityManager.getActiveNetworkInfo();
            if (activeInfo != null
                    && activeInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                if (activeInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_EDGE
                        || activeInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_CDMA
                        || activeInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS) {
                    return true;
                }
            }
        }
        return false;
    }
}
