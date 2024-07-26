package com.example.beyond.demo.ui.transformer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import com.example.base.util.YWBitmapUtil
import com.example.beyond.demo.R

/**
 * 音频轨道资源管理
 *
 * @author wangshichao
 * @date 2024/7/26
 */
class AudioTrackHelper(context: Context) {
    private val iconSize = 36
    private val resIdList = mutableListOf(
        R.drawable.audio01,
        R.drawable.audio02,
        R.drawable.audio03,
        R.drawable.audio04,
        R.drawable.audio05,
        R.drawable.audio06,
        R.drawable.audio07,
        R.drawable.audio08,
        R.drawable.audio09,
        R.drawable.audio10,
        R.drawable.audio11,
        R.drawable.audio12,
        R.drawable.audio13,
        R.drawable.audio14,
        R.drawable.audio15
    )
    private val bitmapList = mutableListOf<Bitmap>()
    private var index = 0

    init {
        resIdList.forEach {
            val bitmap = BitmapFactory.decodeResource(context.resources, it)
            val scaleBitmap = YWBitmapUtil.scaleBitmap(bitmap, iconSize)
            bitmapList.add(scaleBitmap!!)
        }
    }

    fun getFirstBitmap(): Bitmap {
        index = 0
        val targetBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        return bitmapList[index]
    }

    fun getNextBitmap(): Bitmap {
        index++
        if (index >= resIdList.size) {
            index = 0
        }
        return bitmapList[index]
    }

    fun getSize(): Size {
        val bitmap = bitmapList.get(0)
        return Size(bitmap.width, bitmap.height)
    }
}