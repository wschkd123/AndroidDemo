package com.example.beyond.demo.ui.alarm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri

/**
 * 电商应用跳转工具类
 * 优先使用 scheme 跳转 APP，未安装则跳转 H5
 *
 * @author wangshichao
 * @date 2026/4/18
 */
object EcommerceHelper {

    private const val TAG = "EcommerceHelper"

    /**
     * 电商应用类型
     */
    enum class AppType(val packageName: String, val displayName: String) {
        TAOBAO("com.taobao.taobao", "淘宝"),
        JD("com.jingdong.app.mall", "京东"),
        PDD("com.xunmeng.pinduoduo", "拼多多")
    }

    /**
     * 跳转到商品详情页
     * @param context 上下文
     * @param appType 应用类型
     * @param url 商品URL（支持 https:// 和自定义 scheme）
     * @return 是否成功跳转
     */
    fun jumpToProductPage(context: Context, appType: AppType, url: String): Boolean {
        Log.d(TAG, "跳转${appType.displayName}商品页: $url")

        // 1. 先尝试通过 scheme 打开 APP
        if (tryOpenWithScheme(context, url)) {
            Log.d(TAG, "Scheme跳转成功")
            return true
        }

        // 2. Scheme 失败，检查 APP 是否安装，尝试直接启动
//        if (isAppInstalled(context, appType.packageName)) {
//            if (tryLaunchApp(context, appType.packageName)) {
//                Log.d(TAG, "直接启动${appType.displayName}成功")
//                Toast.makeText(context, "已打开${appType.displayName}", Toast.LENGTH_SHORT).show()
//                return true
//            }
//        }

        // 3. APP 未安装或打开失败，跳转 H5
        Log.d(TAG, "${appType.displayName}未安装，跳转H5")
        return openEcommerceH5(context, url)
    }

    /**
     * 尝试通过 scheme 打开 APP
     */
    private fun tryOpenWithScheme(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                context.startActivity(intent)
                true
            } else {
                Log.d(TAG, "Scheme无响应: $url")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scheme跳转失败: ${e.message}")
            false
        }
    }

    /**
     * 检查应用是否安装
     */
    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            intent != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 直接启动应用
     */
    private fun tryLaunchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动应用失败: ${e.message}")
            false
        }
    }

    /**
     * 打开电商H5页面（使用外部浏览器）
     */
    private fun openEcommerceH5(context: Context, url: String): Boolean {
        if (url.isEmpty()) {
            Toast.makeText(context, "不支持的应用类型", Toast.LENGTH_SHORT).show()
            return false
        }

        var success = false
        try {
            val uri = Uri.parse(url)
            Log.d(TAG, "准备启动H5页面: $uri")

            // 方式1: 使用通用 ACTION_VIEW
            success = tryOpenBrowser(context, uri)

            // 方式2: 如果方式1失败，尝试指定常见浏览器包名
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "com.android.chrome") // Chrome
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "com.vivo.browser") // vivo 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "com.miui.browser") // MIUI 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "com.huawei.browser") // 华为浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "com.oppo.browser") // OPPO 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "com.coloros.browser") // ColorOS 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "com.opera.browser") // Opera
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "org.mozilla.firefox") // Firefox
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "com.UCMobile") // UC 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "com.tencent.mtt") // QQ 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(context, uri, "com.baidu.browser.app") // 百度浏览器
            }

            // 方式3: 如果以上都失败，尝试查询系统所有能处理HTTP的应用
            if (!success) {
                success = tryOpenWithAnyBrowser(context, uri)
            }

            if (!success) {
                Log.e(TAG, "所有浏览器打开方式都失败了")
                Toast.makeText(context, "无法打开浏览器，请检查是否安装了浏览器应用", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动H5页面失败: ${e.message}", e)
            Toast.makeText(context, "无法打开页面: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return success
    }

    /**
     * 尝试使用通用方式打开浏览器
     */
    private fun tryOpenBrowser(context: Context, uri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(Intent.CATEGORY_BROWSABLE)
            }

            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                context.startActivity(intent)
                Log.d(TAG, "成功通过通用方式打开浏览器: ${resolveInfo.activityInfo.packageName}")
                true
            } else {
                Log.d(TAG, "通用方式未找到浏览器")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "通用方式打开浏览器失败: ${e.message}")
            false
        }
    }

    /**
     * 尝试使用指定包名的浏览器打开
     */
    private fun tryOpenWithSpecificBrowser(context: Context, uri: Uri, browserPackage: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage(browserPackage)
            }

            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                context.startActivity(intent)
                Log.d(TAG, "成功通过指定浏览器打开: $browserPackage")
                true
            } else {
                Log.d(TAG, "浏览器未安装或不可用: $browserPackage")
                false
            }
        } catch (e: Exception) {
            Log.d(TAG, "使用浏览器 $browserPackage 打开失败: ${e.message}")
            false
        }
    }

    /**
     * 尝试使用系统任意可用浏览器打开（终极方案）
     */
    private fun tryOpenWithAnyBrowser(context: Context, uri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = uri
            }

            // 查询所有能处理该 Intent 的应用
            val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
            if (resolveInfos.isNotEmpty()) {
                Log.d(TAG, "找到 ${resolveInfos.size} 个可处理的应用，尝试第一个")

                // 打印所有可用的浏览器
                resolveInfos.forEachIndexed { index, info ->
                    Log.d(TAG, "  可用浏览器[$index]: ${info.activityInfo.packageName}")
                }

                // 尝试启动第一个
                val firstActivity = resolveInfos[0].activityInfo
                intent.setPackage(firstActivity.packageName)
                context.startActivity(intent)
                Log.d(TAG, "成功通过任意浏览器打开: ${firstActivity.packageName}")
                true
            } else {
                Log.d(TAG, "系统中没有任何可处理HTTP的应用")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "使用任意浏览器打开失败: ${e.message}")
            false
        }
    }
}
