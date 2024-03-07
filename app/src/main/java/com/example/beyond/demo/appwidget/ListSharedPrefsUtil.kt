package com.example.beyond.demo.appwidget

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.LayoutRes
import androidx.core.content.edit
import com.example.beyond.demo.R

object ListSharedPrefsUtil {
    private const val PREFS_NAME = "com.example.beyond.demo.appwidget.CharacterAppWidget"
    private const val PREF_PREFIX_KEY = "appwidget_"

    internal fun saveWidgetLayoutIdPref(
        context: Context,
        appWidgetId: Int,
        @LayoutRes layoutId: Int
    ) {
        context.getSharedPreferences(name = PREFS_NAME, mode = 0).edit {
            putInt(PREF_PREFIX_KEY + appWidgetId, layoutId)
        }
    }

    internal fun loadWidgetLayoutIdPref(context: Context, appWidgetId: Int): Int =
        context.getSharedPreferences(name = PREFS_NAME, mode = 0)
            .getInt(PREF_PREFIX_KEY + appWidgetId, R.layout.view_character)

    internal fun deleteWidgetLayoutIdPref(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(name = PREFS_NAME, mode = 0).edit {
            remove(PREF_PREFIX_KEY + appWidgetId)
        }
    }

    // Wrapper for Context.getSharedPreferences to support named arguments
    private fun Context.getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return getSharedPreferences(name, mode)
    }
}