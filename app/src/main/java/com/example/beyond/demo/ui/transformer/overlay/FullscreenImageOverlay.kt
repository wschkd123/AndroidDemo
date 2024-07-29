package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.util.UnstableApi
import com.example.beyond.demo.ui.transformer.util.FullscreenBgHelper

/**
 * 图片url基类。支持在指定时间范围内显示
 *
 * @author wangshichao
 * @date 2024/7/25
 */
@UnstableApi
open class FullscreenImageOverlay(
    context: Context,
    url: String,
    durationUs: Long
) : BaseBitmapOverlay(context, url, durationUs) {
    private val bitmapHelper: FullscreenBgHelper = FullscreenBgHelper()

    /**
     * 剪裁原Bitmap
     */
    override fun cropBitmap(srcBitmap: Bitmap): Bitmap {
        return bitmapHelper.createCharacterBgWithMask(srcBitmap)
    }

}