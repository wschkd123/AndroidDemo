package com.example.beyond.demo.ui.transformer.util

import android.util.Log
import androidx.media3.effect.OverlaySettings
import java.lang.reflect.Field

/**
 *
 * @author wangshichao
 * @date 2024/7/23
 */
object ReflectUtil {

    @JvmStatic
    fun updateOverlaySettingsFiled(settings: OverlaySettings, fieldName: String, value: Any) =
        try {
            val field: Field = settings.javaClass.getField(fieldName)
            field.isAccessible = true
            field.set(settings, value)
        } catch (e: Exception) {
            Log.e("ReflectUtil", "value=$value e=" + e.message)
            e.printStackTrace()
        }
}