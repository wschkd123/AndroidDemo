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
            EcommerceHelper.jumpToProductPage(
                requireContext(),
                EcommerceHelper.AppType.TAOBAO,
                "https://e.tb.cn/h.iJcbg27uhx7Xujo"
            )
        }

        // 京东入口按钮
        binding.btnJd.setOnClickListener {
            EcommerceHelper.jumpToProductPage(
                requireContext(),
                EcommerceHelper.AppType.JD,
                "https://3.cn/2L2-YayJ"
            )
        }

        // 拼多多入口按钮
        binding.btnPdd.setOnClickListener {
            EcommerceHelper.jumpToProductPage(
                requireContext(),
                EcommerceHelper.AppType.PDD,
                "https://mobile.yangkeduo.com/goods.html?ps=jGcMGBGrNv"
            )
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AlarmFragment()
    }
}
