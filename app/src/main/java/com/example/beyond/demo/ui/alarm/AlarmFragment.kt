package com.example.beyond.demo.ui.alarm

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.base.BaseFragment
import com.example.beyond.demo.databinding.FragmentAlarmBinding
import java.util.Calendar

/**
 * 闹钟设置界面
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
            selectRingtone()
        }

        // 设置闹钟按钮
        binding.btnSetAlarm.setOnClickListener {
            setSystemAlarm()
        }
    }

    /**
     * 选择铃声
     */
    private fun selectRingtone() {
        try {
            // 尝试打开系统铃声选择器
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
            // 如果系统不支持，尝试打开文件选择器
            filePickerLauncher.launch("audio/*")
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AlarmFragment()
    }
}
