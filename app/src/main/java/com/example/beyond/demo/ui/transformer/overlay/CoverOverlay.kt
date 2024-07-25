package com.example.beyond.demo.ui.transformer.overlay

import android.content.Context
import android.graphics.Bitmap
import com.example.beyond.demo.ui.transformer.util.TransformerUtil

/**
 * 视频封面-第一帧
 *
 * @author wangshichao
 * @date 2024/7/24
 */
class CoverOverlay(
    context: Context,
    url: String,
    startTimeUs: Long,
    durationUs: Long
) : BaseBitmapOverlay(context, url, startTimeUs, durationUs) {

    override fun cropBitmap(srcBitmap: Bitmap): Bitmap {
        return TransformerUtil.createCharacterBgWithMask(srcBitmap)
    }
}