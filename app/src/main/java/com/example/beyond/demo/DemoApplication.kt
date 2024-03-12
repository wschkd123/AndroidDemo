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
        // 修复WorkManager更新小部件时一直刷新 https://issuetracker.google.com/issues/241076154
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "not_executed_work",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequest.Builder(CharacterWorker::class.java)
                    .setInitialDelay(365 * 10, TimeUnit.DAYS)
                    .build()
            )

    }
}