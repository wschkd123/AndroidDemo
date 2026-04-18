package com.example.beyond.demo.ui.alarm

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.base.BaseFragment
import com.example.beyond.demo.databinding.FragmentAlarmBinding
import java.net.URLEncoder
import java.util.Calendar

/**
 * 闹钟设置界面 - 支持内置MP3和URL音频作为铃声
 *
 * @author wangshichao
 * @date 2026/4/16
 */
class AlarmFragment : BaseFragment() {

    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    private var selectedRingtonePath: String? = null
    private var selectedRingtoneName: String = "默认铃声"

    // 待下载的URL（用于权限请求后继续下载）
    private var pendingDownloadUrl: String? = null

    // 存储权限请求（Android 9及以下需要）
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingDownloadUrl?.let { executeDownload(it) }
        } else {
            Toast.makeText(context, "需要存储权限才能下载铃声", Toast.LENGTH_LONG).show()
        }
        pendingDownloadUrl = null
    }

    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedRingtonePath = it.toString()
            selectedRingtoneName = it.lastPathSegment ?: "自定义铃声"
            binding.tvRingtone.text = selectedRingtoneName
            Toast.makeText(context, "已选择: $selectedRingtoneName", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 时间选择器 - 设置默认时间为当前时间后1小时
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        binding.timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
        binding.timePicker.minute = calendar.get(Calendar.MINUTE)
        binding.timePicker.setIs24HourView(true)

        // 选择铃声按钮
        binding.btnSelectRingtone.setOnClickListener {
            showRingtoneSelectionDialog()
        }

        // 设置闹钟按钮
        binding.btnSetAlarm.setOnClickListener {
            setSystemAlarm()
        }

        // 淘宝入口按钮
        binding.btnTaobao.setOnClickListener {
            jumpToEcommerceApp("taobao", "女装", "com.taobao.taobao")
        }

        // 京东入口按钮
        binding.btnJd.setOnClickListener {
            jumpToEcommerceApp("jd", "女装", "com.jingdong.app.mall")
        }

        // 拼多多入口按钮
        binding.btnPdd.setOnClickListener {
            jumpToEcommerceApp("pdd", "女装", "com.xunmeng.pinduoduo")
        }
    }


    /**
     * 显示铃声选择对话框
     */
    private fun showRingtoneSelectionDialog() {
        val options = arrayOf("从URL下载", "选择本地文件")

        AlertDialog.Builder(requireContext())
            .setTitle("选择铃声来源")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> downloadDefaultRingtone()
                    1 -> selectLocalFile()
                }
            }
            .show()
    }

    /**
     * 下载默认铃声
     */
    private fun downloadDefaultRingtone() {
        downloadRingtone(RingtoneHelper.getDefaultRingtoneUrl())
    }

    /**
     * 下载铃声（带权限检查）
     */
    private fun downloadRingtone(url: String) {
        // Android 9及以下需要检查存储权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownloadUrl = url
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        executeDownload(url)
    }

    /**
     * 执行下载
     */
    private fun executeDownload(url: String) {
        Toast.makeText(context, "正在下载...", Toast.LENGTH_SHORT).show()

        Thread {
            val fileName = RingtoneHelper.generateFileName()
            val uri = RingtoneHelper.downloadToPublicDirectory(requireContext(), url, fileName)

            requireActivity().runOnUiThread {
                if (uri != null) {
                    selectedRingtonePath = uri.toString()
                    selectedRingtoneName = "网络铃声"
                    binding.tvRingtone.text = selectedRingtoneName
                    Toast.makeText(context, "下载成功", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "铃声URI: $uri")
                } else {
                    Toast.makeText(context, "下载失败", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /**
     * 选择本地文件
     */
    private fun selectLocalFile() {
        filePickerLauncher.launch("audio/*")
    }

    /**
     * 设置系统闹钟
     */
    private fun setSystemAlarm() {
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute

        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "自定义闹钟")
                putExtra(AlarmClock.EXTRA_VIBRATE, true) // 启用震动
                putExtra(AlarmClock.EXTRA_SKIP_UI, false) // 显示系统闹钟UI让用户确认

                // 设置自定义铃声（如果已选择）
                selectedRingtonePath?.let { path ->
                    putExtra(AlarmClock.EXTRA_RINGTONE, Uri.parse(path))
                    Log.d(TAG, "设置铃声: $path")
                }
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
                Toast.makeText(
                    context,
                    "闹钟已设置: ${String.format("%02d:%02d", hour, minute)}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "未找到闹钟应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "设置闹钟失败: ${e.message}")
            Toast.makeText(context, "设置闹钟失败", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 通用的电商APP跳转方法
     * @param appType 应用类型: taobao/jd/pdd
     * @param keyword 搜索关键词
     * @param packageName 应用包名
     */
    private fun jumpToEcommerceApp(appType: String, keyword: String, packageName: String) {
        val appName = when (appType) {
            "taobao" -> "淘宝"
            "jd" -> "京东"
            "pdd" -> "拼多多"
            else -> appType
        }
        
        Log.d(TAG, "========== 开始跳转$appName ==========")
        Log.d(TAG, "应用类型: $appType, 关键词: $keyword, 包名: $packageName")
        
        val query = URLEncoder.encode(keyword, "UTF-8")
        Log.d(TAG, "编码后的查询: $query")
        
        // 检查APP是否安装
        val isInstalled = isAppInstalled(packageName)
        Log.d(TAG, "$appName 是否安装: $isInstalled")
        
        if (isInstalled) {
            // 尝试打开APP
            val success = tryOpenEcommerceApp(appType, query, keyword, packageName)
            if (success) {
                return
            }
        } else {
            Log.d(TAG, "$appName 未安装,准备跳转到H5页面")
        }
        
        // 降级方案：跳转 H5 页
        openEcommerceH5(appType, query)
        
        Log.d(TAG, "========== 跳转$appName 结束 ==========")
    }
    
    /**
     * 尝试多种方式打开电商APP
     * @return 是否成功打开
     */
    private fun tryOpenEcommerceApp(appType: String, query: String, keyword: String, packageName: String): Boolean {
        return when (appType) {
            "taobao" -> tryOpenTaobaoApp(query, keyword, packageName)
            "jd" -> tryOpenJdApp(query, keyword, packageName)
            "pdd" -> tryOpenPddApp(query, keyword, packageName)
            else -> false
        }
    }
    
    /**
     * 打开淘宝APP (优先tbopen协议,失败后H5,都必须带搜索关键字)
     */
    private fun tryOpenTaobaoApp(query: String, keyword: String, packageName: String): Boolean {
        // 方式1: 使用 tbopen:// 协议 (带搜索参数)
        try {
            val encodedUrl = URLEncoder.encode("https://s.m.taobao.com/h5?q=$query", "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("tbopen://m.taobao.com/tbopen/index.html?action=ali.open.nav&module=h5&h5Url=$encodedUrl")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Log.d(TAG, "淘宝-方式1: 尝试 tbopen:// 协议(带搜索)")
            val resolveInfo = requireContext().packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null && resolveInfo.activityInfo.packageName == packageName) {
                startActivity(intent)
                Log.d(TAG, "淘宝-方式1: 成功(tbopen,带搜索参数)")
                return true
            } else {
                Log.w(TAG, "淘宝-方式1: 未找到淘宝处理tbopen, resolveInfo=${resolveInfo?.activityInfo?.packageName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "淘宝-方式1失败: ${e.message}", e)
        }
        
        // tbopen失败,返回false让外层调用H5
        Log.d(TAG, "淘宝-tbopen协议不可用,将跳转到H5(带搜索)")
        return false
    }
    
    /**
     * 打开京东APP (优先使用带搜索参数的协议)
     */
    private fun tryOpenJdApp(query: String, keyword: String, packageName: String): Boolean {
        // 方式1: 使用 openapp.jdmobile:// 协议 (京东deep link,带搜索)
        try {
            val jsonParam = "{\"category\":\"jump\",\"des\":\"search\",\"keyWord\":\"$keyword\"}"
            val encodedParam = URLEncoder.encode(jsonParam, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("openapp.jdmobile://virtual?params=$encodedParam")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Log.d(TAG, "京东-方式1: 尝试 openapp.jdmobile:// (带搜索)")
            val resolveInfo = requireContext().packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null && resolveInfo.activityInfo.packageName == packageName) {
                startActivity(intent)
                Log.d(TAG, "京东-方式1: 成功(openapp,带搜索参数)")
                return true
            } else {
                Log.w(TAG, "京东-方式1: 未找到京东处理openapp")
            }
        } catch (e: Exception) {
            Log.e(TAG, "京东-方式1失败: ${e.message}", e)
        }
        
        // 方式2: 使用 jd:// 协议 (带搜索)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("jd://search?keyword=$query")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Log.d(TAG, "京东-方式2: 尝试 jd:// 协议(带搜索)")
            val resolveInfo = requireContext().packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null && resolveInfo.activityInfo.packageName == packageName) {
                startActivity(intent)
                Log.d(TAG, "京东-方式2: 成功(jd://,带搜索参数)")
                return true
            } else {
                Log.w(TAG, "京东-方式2: 未找到京东处理jd://")
            }
        } catch (e: Exception) {
            Log.e(TAG, "京东-方式2失败: ${e.message}", e)
        }
        
        // 方式3: 使用getLaunchIntentForPackage (不带搜索,作为备选)
        try {
            val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                Log.d(TAG, "京东-方式3: 使用LaunchIntent(不带搜索)")
                startActivity(launchIntent)
                Toast.makeText(context, "已打开京东,请手动搜索: $keyword", Toast.LENGTH_LONG).show()
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "京东-方式3失败: ${e.message}", e)
        }
        
        return false
    }
    
    /**
     * 打开拼多多APP (优先使用带搜索参数的协议)
     */
    private fun tryOpenPddApp(query: String, keyword: String, packageName: String): Boolean {
        // 方式1: 使用 pinduoduo:// 协议 (带搜索)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("pinduoduo://com.xunmeng.pinduoduo/goods_detail.html?goods_id=193791252730")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Log.d(TAG, "拼多多-方式1: 尝试 pinduoduo:// 协议(带搜索)")
            val resolveInfo = requireContext().packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null && resolveInfo.activityInfo.packageName == packageName) {
                startActivity(intent)
                Log.d(TAG, "拼多多-方式1: 成功(pinduoduo://,带搜索参数)")
                return true
            } else {
                Log.w(TAG, "拼多多-方式1: 未找到拼多多处理pinduoduo://")
            }
        } catch (e: Exception) {
            Log.e(TAG, "拼多多-方式1失败: ${e.message}", e)
        }
        
        // 方式2: 使用getLaunchIntentForPackage (不带搜索,作为备选)
        try {
            val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                Log.d(TAG, "拼多多-方式2: 使用LaunchIntent(不带搜索)")
                startActivity(launchIntent)
                Toast.makeText(context, "已打开拼多多,请手动搜索: $keyword", Toast.LENGTH_LONG).show()
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "拼多多-方式2失败: ${e.message}", e)
        }
        
        return false
    }
    
    /**
     * 打开电商H5页面（使用外部浏览器）
     */
    private fun openEcommerceH5(appType: String, query: String) {
        val url = when (appType) {
            "taobao" -> "https://s.m.taobao.com/h5?q=$query"
            "jd" -> "https://search.jd.com/Search?keyword=$query"
            "pdd" -> "https://p.pinduoduo.com/OLr3nEjT?sc=EFAC"
            else -> ""
        }
        
        if (url.isEmpty()) {
            Toast.makeText(context, "不支持的应用类型", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val uri = Uri.parse(url)
            Log.d(TAG, "准备启动H5页面: $uri")
            
            // 尝试多种方式打开浏览器
            var success: Boolean
            
            // 方式1: 使用通用 ACTION_VIEW
            success = tryOpenBrowser(uri)
            
            // 方式2: 如果方式1失败，尝试指定常见浏览器包名
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "com.android.chrome") // Chrome
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "com.vivo.browser") // vivo 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "com.miui.browser") // MIUI 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "com.huawei.browser") // 华为浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "com.oppo.browser") // OPPO 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "com.coloros.browser") // ColorOS 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "com.opera.browser") // Opera
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "org.mozilla.firefox") // Firefox
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "com.UCMobile") // UC 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "com.tencent.mtt") // QQ 浏览器
            }
            if (!success) {
                success = tryOpenWithSpecificBrowser(uri, "com.baidu.browser.app") // 百度浏览器
            }
            
            // 方式3: 如果以上都失败，尝试查询系统所有能处理HTTP的应用
            if (!success) {
                success = tryOpenWithAnyBrowser(uri)
            }
            
            if (!success) {
                Log.e(TAG, "所有浏览器打开方式都失败了")
                Toast.makeText(context, "无法打开浏览器，请检查是否安装了浏览器应用", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动H5页面失败: ${e.message}", e)
            Toast.makeText(context, "无法打开页面: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 尝试使用通用方式打开浏览器
     */
    private fun tryOpenBrowser(uri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            
            val resolveInfo = requireContext().packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                startActivity(intent)
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
    private fun tryOpenWithSpecificBrowser(uri: Uri, browserPackage: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage(browserPackage)
            }
            
            val resolveInfo = requireContext().packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                startActivity(intent)
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
    private fun tryOpenWithAnyBrowser(uri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = uri
            }
            
            // 查询所有能处理该 Intent 的应用
            val resolveInfos = requireContext().packageManager.queryIntentActivities(intent, 0)
            if (resolveInfos.isNotEmpty()) {
                Log.d(TAG, "找到 ${resolveInfos.size} 个可处理的应用，尝试第一个")
                
                // 打印所有可用的浏览器
                resolveInfos.forEachIndexed { index, info ->
                    Log.d(TAG, "  可用浏览器[$index]: ${info.activityInfo.packageName}")
                }
                
                // 尝试启动第一个
                val firstActivity = resolveInfos[0].activityInfo
                intent.setPackage(firstActivity.packageName)
                startActivity(intent)
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

    /**
     * 跳转到淘宝搜索 (保留兼容)
     */
    private fun jumpToTaobaoSearch(keyword: String) {
        jumpToEcommerceApp("taobao", keyword, "com.taobao.taobao")
    }
    
    /**
     * 检查应用是否已安装（支持多个包名候选）
     */
    private fun isAppInstalled(packageName: String): Boolean {
        // 对于京东，尝试多个可能的包名
        val candidatePackages = when (packageName) {
            "com.jingdong.app.mall" -> listOf(
                "com.jingdong.app.mall",
                "com.jd.jrapp"
            )
            else -> listOf(packageName)
        }
        
        for (candidatePackage in candidatePackages) {
            // 方法1: 使用getLaunchIntentForPackage（优先，更可靠）
            val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(candidatePackage)
            if (launchIntent != null) {
                Log.d(TAG, "通过LaunchIntent找到应用: $candidatePackage")
                return true
            }
            
            // 方法2: 使用queryIntentActivities检测
            try {
                val intent = requireContext().packageManager.getLaunchIntentForPackage(candidatePackage)
                if (intent != null) {
                    val resolveInfos = requireContext().packageManager.queryIntentActivities(intent, 0)
                    if (resolveInfos.isNotEmpty()) {
                        Log.d(TAG, "通过queryIntentActivities找到应用: $candidatePackage")
                        return true
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "queryIntentActivities失败 ($candidatePackage): ${e.message}")
            }
            
            // 方法3: 使用getPackageInfo作为最后手段
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requireContext().packageManager.getPackageInfo(
                        candidatePackage, 
                        android.content.pm.PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    requireContext().packageManager.getPackageInfo(candidatePackage, 0)
                }
                Log.d(TAG, "通过getPackageInfo找到应用: $candidatePackage, 版本: ${packageInfo.versionName}")
                return true
            } catch (e: Exception) {
                // getPackageInfo 在某些系统上可能返回奇怪的错误，忽略即可
                Log.d(TAG, "getPackageInfo失败 ($candidatePackage)")
            }
        }
        
        Log.d(TAG, "所有候选包名都未找到应用")
        return false
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AlarmFragment()
    }
}
