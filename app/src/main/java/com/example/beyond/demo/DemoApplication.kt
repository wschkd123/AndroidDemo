package com.example.beyond.demo

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.beyond.demo.appwidget.CharacterWorker
import com.example.beyond.demo.common.Init
import java.util.concurrent.TimeUnit

/**
 *
 * @author wangshichao
 * @date 2024/3/11
 */
class DemoApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Init.setApplication(this)
    }
}