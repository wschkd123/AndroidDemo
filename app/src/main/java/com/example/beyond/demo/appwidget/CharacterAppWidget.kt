package com.example.beyond.demo.appwidget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import com.example.beyond.demo.R
import com.example.beyond.demo.ui.MainActivity

/**
 * 单个人物
 */
class CharacterAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
//            ListSharedPrefsUtil.deleteWidgetLayoutIdPref(context, appWidgetId)
        }
    }

    companion object {

        private const val REQUEST_CODE_OPEN_ACTIVITY = 1

        @SuppressLint("RemoteViewLayout")
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val appOpenIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_ACTIVITY,
                activityIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            fun constructRemoteViews(
                @LayoutRes widgetLayoutId: Int
            ) = RemoteViews(context.packageName, widgetLayoutId).apply {

            }

            val remoteViews = constructRemoteViews(R.layout.view_character)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }
}
