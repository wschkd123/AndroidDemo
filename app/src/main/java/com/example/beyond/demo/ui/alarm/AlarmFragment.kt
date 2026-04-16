package com.example.beyond.demo.ui.alarm

import android.app.AlertDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
            jumpToTaobaoSearch("女装")
        }
    }

    /**
     * 显示铃声选择对话框
     */
    private fun showRingtoneSelectionDialog() {
        val options = arrayOf(
            "使用系统铃声",
            "使用内置MP3",
            "从URL下载",
            "选择本地文件"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("选择铃声来源")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectSystemRingtone()
                    1 -> selectBuiltInRingtone()
                    2 -> inputUrlForRingtone()
                    3 -> selectLocalFile()
                }
            }
            .show()
    }

    /**
     * 选择系统铃声
     */
    private fun selectSystemRingtone() {
        try {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择闹钟铃声")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtonePath?.let { Uri.parse(it) })
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            }
            startActivity(intent)
            Toast.makeText(context, "请在系统界面选择铃声", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "打开系统铃声选择器失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 选择内置MP3铃声
     */
    private fun selectBuiltInRingtone() {
        val ringtones = RingtoneHelper.getAvailableBuiltInRingtones()
        val names = ringtones.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("选择内置铃声")
            .setItems(names) { _, which ->
                val selectedRingtone = ringtones[which]
                val uri = RingtoneHelper.copyAssetToStorage(requireContext(), selectedRingtone.fileName)
                if (uri != null) {
                    selectedRingtonePath = uri.toString()
                    selectedRingtoneName = selectedRingtone.displayName
                    binding.tvRingtone.text = selectedRingtoneName
                    Toast.makeText(context, "已选择: ${selectedRingtone.displayName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "加载铃声失败", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    /**
     * 输入URL下载铃声
     */
    private fun inputUrlForRingtone() {
        val editText = EditText(requireContext()).apply {
            hint = "https://example.com/ringtone.mp3"
            setText("https://")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("输入音频URL")
            .setMessage("请输入MP3音频文件的下载链接:")
            .setView(editText)
            .setPositiveButton("下载") { _, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty() && url.startsWith("http")) {
                    downloadRingtoneFromUrl(url)
                } else {
                    Toast.makeText(context, "请输入有效的URL", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 从URL下载铃声
     */
    private fun downloadRingtoneFromUrl(url: String) {
        Toast.makeText(context, "正在下载...", Toast.LENGTH_SHORT).show()

        // 在后台线程下载
        Thread {
            val fileName = "ringtone_${System.currentTimeMillis()}.mp3"
            val uri = RingtoneHelper.downloadFromUrl(requireContext(), url, fileName)

            requireActivity().runOnUiThread {
                if (uri != null) {
                    selectedRingtonePath = uri.toString()
                    selectedRingtoneName = "网络铃声"
                    binding.tvRingtone.text = selectedRingtoneName
                    Toast.makeText(context, "下载成功: $selectedRingtoneName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "下载失败,请检查URL是否正确", Toast.LENGTH_LONG).show()
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
            // 使用AlarmClock Intent直接调用系统闹钟APP
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "自定义闹钟")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false) // 显示系统闹钟UI让用户确认
                putExtra(AlarmClock.EXTRA_VIBRATE, true) // 启用震动

                // 设置自定义铃声（如果选择了）
                selectedRingtonePath?.let { ringtoneUri ->
                    putExtra(AlarmClock.EXTRA_RINGTONE, Uri.parse(ringtoneUri))
                }
            }

            // 检查是否有权限 (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = requireContext().getSystemService(android.app.AlarmManager::class.java)
                if (alarmManager.canScheduleExactAlarms()) {
                    startActivity(intent)
                } else {
                    // 请求精确闹钟权限
                    requestExactAlarmPermission()
                    return
                }
            } else {
                startActivity(intent)
            }

            Toast.makeText(
                context,
                "闹钟已设置: ${String.format("%02d:%02d", hour, minute)}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "设置闹钟失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 请求精确闹钟权限（Android 12+）
     */
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
            Toast.makeText(
                context,
                "请授予精确闹钟权限以设置闹钟",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 跳转到淘宝搜索
     */
    private fun jumpToTaobaoSearch(keyword: String) {
        Log.d("TaobaoJump", "========== 开始跳转淘宝 ==========")
        Log.d("TaobaoJump", "搜索关键词: $keyword")
        
        val query = URLEncoder.encode(keyword, "UTF-8")
        Log.d("TaobaoJump", "编码后的查询: $query")
        
        // 检查淘宝APP是否安装
        val isInstalled = isAppInstalled("com.taobao.taobao")
        Log.d("TaobaoJump", "淘宝APP是否安装: $isInstalled")
        
        if (isInstalled) {
            // 尝试多种方式打开淘宝APP
            val success = tryOpenTaobaoApp(query, keyword)
            if (success) {
                return
            }
        } else {
            Log.d("TaobaoJump", "淘宝APP未安装,准备跳转到H5页面")
        }
        
        // 降级方案：跳转 H5 页
        openTaobaoH5(query)
        
        Log.d("TaobaoJump", "========== 跳转淘宝结束 ==========")
    }
    
    /**
     * 尝试多种方式打开淘宝APP (优先级: 原生APP > tbopen协议 > H5)
     * @return 是否成功打开
     */
    private fun tryOpenTaobaoApp(query: String, keyword: String): Boolean {
        // 方式1: 使用getLaunchIntentForPackage打开淘宝原生APP (无法传递搜索参数)
//        try {
//            val launchIntent = requireContext().packageManager.getLaunchIntentForPackage("com.taobao.taobao")
//            if (launchIntent != null) {
//                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                Log.d("TaobaoJump", "方式1: 使用LaunchIntent打开淘宝原生APP")
//                startActivity(launchIntent)
//                Toast.makeText(context, "已打开淘宝,请手动搜索: $keyword", Toast.LENGTH_LONG).show()
//                Log.d("TaobaoJump", "方式1: 成功(淘宝原生APP)")
//                return true
//            } else {
//                Log.w("TaobaoJump", "方式1: getLaunchIntentForPackage返回null")
//            }
//        } catch (e: Exception) {
//            Log.e("TaobaoJump", "方式1失败: ${e.message}", e)
//        }
        
        // 方式2: 使用 tbopen:// 协议 (淘宝开放平台scheme,可带搜索参数)
        try {
            val encodedUrl = URLEncoder.encode("https://s.m.taobao.com/h5?q=$query", "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("tbopen://m.taobao.com/tbopen/index.html?action=ali.open.nav&module=h5&h5Url=$encodedUrl")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Log.d("TaobaoJump", "方式2: 尝试 tbopen:// 协议")
            val resolveInfo = requireContext().packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null && resolveInfo.activityInfo.packageName == "com.taobao.taobao") {
                Log.d("TaobaoJump", "方式2: 找到淘宝Activity: ${resolveInfo.activityInfo.name}")
                startActivity(intent)
                Log.d("TaobaoJump", "方式2: 成功(tbopen协议)")
                return true
            } else {
                Log.w("TaobaoJump", "方式2: 未找到淘宝APP处理tbopen协议")
            }
        } catch (e: Exception) {
            Log.e("TaobaoJump", "方式2失败: ${e.message}", e)
        }
        
        // 方式3: 使用 https URL (降级方案,可能在浏览器或淘宝中打开)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://s.m.taobao.com/h5?q=$query")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Log.d("TaobaoJump", "方式3: 尝试 https URL")
            val resolveInfo = requireContext().packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                Log.d("TaobaoJump", "方式3: 找到Activity: ${resolveInfo.activityInfo.packageName}")
                startActivity(intent)
                Log.d("TaobaoJump", "方式3: 成功")
                return true
            }
        } catch (e: Exception) {
            Log.e("TaobaoJump", "方式3失败: ${e.message}", e)
        }
        
        Log.w("TaobaoJump", "所有打开淘宝的方式都失败了")
        return false
    }
    
    /**
     * 打开淘宝H5页面
     */
    private fun openTaobaoH5(query: String) {
        try {
            val h5Uri = Uri.parse("https://s.m.taobao.com/h5?search-btn=&q=$query")
            Log.d("TaobaoJump", "准备启动H5页面: $h5Uri")
            
            val h5Intent = Intent(Intent.ACTION_VIEW, h5Uri)
            h5Intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            val resolveInfo = requireContext().packageManager.resolveActivity(h5Intent, 0)
            if (resolveInfo != null) {
                startActivity(h5Intent)
                Log.d("TaobaoJump", "成功启动H5页面")
            } else {
                Toast.makeText(context, "无法打开淘宝页面", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("TaobaoJump", "启动H5页面失败: ${e.message}", e)
            Toast.makeText(context, "无法打开淘宝", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 检查应用是否已安装
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            // 方法1: 使用getPackageInfo (需要处理Android 13的权限)
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                requireContext().packageManager.getPackageInfo(packageName, 0)
            }
            Log.d("TaobaoJump", "找到应用: $packageName, 版本: ${packageInfo.versionName}")
            true
        } catch (e: Exception) {
            Log.d("TaobaoJump", "getPackageInfo失败: ${e.javaClass.simpleName} - ${e.message}")
            
            // 方法2: 使用getLaunchIntentForPackage作为备选方案
            val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            val result = launchIntent != null
            Log.d("TaobaoJump", "getLaunchIntentForPackage结果: $result")
            result
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AlarmFragment()
    }
}
