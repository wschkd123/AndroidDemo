package com.example.base.util

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextUtils
import android.util.Log
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object YWBitmapUtil {
    private const val TAG = "YWBitmapUtil"

    /**
     * 裁剪图片
     *
     * @param source 原图
     * @param cropRect 裁剪区域
     * @return 裁剪后图片
     */
    fun cropBitmap(source: Bitmap, cropRect: Rect): Bitmap? {
        if (cropRect.isEmpty) {
            return source
        }

        // 如果裁剪区域超出原图范围，则返回原图
        val sourceRect = Rect(0, 0, source.width, source.height)
        if (!sourceRect.contains(cropRect)) {
            return source
        }
        return if (source.isRecycled) {
            null
        } else Bitmap.createBitmap(
            source, cropRect.left, cropRect.top, cropRect.width(),
            cropRect.height()
        )
    }

    /**
     * 高斯模糊转换
     *
     * @param bm 待转换 bitmap
     * @param isBookCover 是否书封
     * @param postProcess 图片后期处理
     * @return 转换结果
     */
    fun convertGaussianBlur(
        bm: Bitmap?, isBookCover: Boolean,
        postProcess: BitmapPostProcess?
    ): Bitmap? {
        return convertGaussianBlur(bm, isBookCover, 25, postProcess)
    }

    /**
     * 高斯模糊转换
     *
     * @param bm 待转换 bitmap
     * @param postProcess 图片后期处理
     * @return 转换结果
     */
    fun convertGaussianBlur(bm: Bitmap?, postProcess: BitmapPostProcess?): Bitmap? {
        return convertGaussianBlur(bm, true, postProcess)
    }

    /**
     * 高斯模糊转换
     *
     * @param bm 待转换 bitmap
     * @param isBookCover 是否书封
     * @param gaussRadius 半径
     * @param postProcess 图片后期处理
     * @return 转换结果
     */
    fun convertGaussianBlur(
        bm: Bitmap?, isBookCover: Boolean, gaussRadius: Int,
        postProcess: BitmapPostProcess?
    ): Bitmap? {
        //防止OOM
        try {
            if (bm == null) {
                return null
            }
            var left = 0
            var top = bm.height / 5
            var right = bm.width
            var bottom = bm.height * 3 / 5
            if (!isBookCover) {
                //非书封设置最大高度，宽度
                val maxWidth = 200
                val maxHeight = 200
                left = 0
                top = if (bm.height > maxHeight) maxHeight / 5 else bm.height / 5
                right = Math.min(bm.width, maxWidth)
                bottom = if (bm.height > maxHeight) maxHeight * 3 / 5 else bm.height * 3 / 5
            }
            val scaleDown = Bitmap.createBitmap(bm, left, top, right, bottom)
            val outputBitmap = doBlur(scaleDown, gaussRadius, true)
            if (postProcess == null) {
                return outputBitmap
            }
            postProcess.processBitmap(Canvas(outputBitmap!!))
            return outputBitmap
        } catch (e: Exception) {
            val message = e.message
            if (message != null) {
                Log.e("Err", message)
            }
        } catch (e: OutOfMemoryError) {
            val message = e.message
            if (message != null) {
                Log.e("Err", message)
            }
        }
        return bm
    }

    /**
     * BitMap高斯模糊处理
     *
     *
     * 原图缩放，增加虚化程度
     *
     * @param bm 原图
     * @param gaussRadius 高斯程度（越大越模糊）
     * @param postProcess 后期处理
     * @return 处理后图片
     */
    fun convertGaussianBlur(
        bm: Bitmap, gaussRadius: Int,
        postProcess: BitmapPostProcess?
    ): Bitmap? {
        //防止OOM
        try {
            val left = 0
            val top = 0
            val right = bm.width
            val bottom = bm.height
            val scaleDown = Bitmap
                .createScaledBitmap(bm, bm.width / 5, bm.height / 5, false)
            val outputBitmap = doBlur(scaleDown, gaussRadius, true)
            if (postProcess == null) {
                return outputBitmap
            }
            postProcess.processBitmap(Canvas(outputBitmap!!))
            return outputBitmap
        } catch (e: Exception) {
            Log.e("Err", e.message!!)
        } catch (e: OutOfMemoryError) {
            Log.e("Err", e.message!!)
        }
        return bm
    }

    fun doBlur(
        sentBitmap: Bitmap, radius: Int,
        canReuseInBitmap: Boolean
    ): Bitmap? {
        val bitmap: Bitmap
        bitmap = if (canReuseInBitmap) {
            sentBitmap
        } else {
            sentBitmap.copy(sentBitmap.config, true)
        }
        if (radius < 1) {
            return null
        }
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yi = 0
        yw = yi
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {

                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (-0x1000000 and pix[yi] or (dv[rsum] shl 16)
                        or (dv[gsum] shl 8) or dv[bsum])
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    /**
     * 给bitmap 换色
     * Utility.UIUtils.recolorNormalBitmap
     *
     * @param bitmap mutable的bitmap
     * @param rgbColor rgb颜色
     */
    fun recolorNormalBitmap(bitmap: Bitmap, rgbColor: Int): Bitmap {
        val b = (rgbColor and 0xFF).toFloat()
        val g = (rgbColor shr 8 and 0xFF).toFloat()
        val r = (rgbColor shr 16 and 0xFF).toFloat()
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val matrix = ColorMatrix()
        val arrays = floatArrayOf(
            0f, 0f, 0f, 0f, r,
            0f, 0f, 0f, 0f, g,
            0f, 0f, 0f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        )
        matrix.set(arrays)
        paint.setColorFilter(ColorMatrixColorFilter(matrix))
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return bitmap
    }

    /**
     * Bitmap 缩放
     *
     * @param src bitmap 来源
     * @param height 高度
     * @return
     */
    fun scaleBitmap(src: Bitmap?, height: Int): Bitmap? {
        if (src != null) {
            if (src.height == height) {
                return src
            } else if (height > 0) {
                val sH = height.toFloat() / src.height.toFloat()
                val m = Matrix()
                m.postScale(sH, sH)
                return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
            }
        }
        return src
    }

    /**
     * Bitmap 缩放
     *
     * @param src bitmap 来源
     * @param width 宽度
     * @return
     */
    fun scaleBitmapByWidth(src: Bitmap?, width: Int): Bitmap? {
        if (src != null) {
            if (src.width == width) {
                return src
            } else if (width > 0) {
                val sH = width.toFloat() / src.width.toFloat()
                val m = Matrix()
                m.postScale(sH, sH)
                return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
            }
        }
        return src
    }
    /**
     * 压缩 Bitmap 到指定大小
     *
     * @param bmp
     * @param size kb
     * @param format
     * @return
     */
    /**
     * 压缩 Bitmap 到指定大小
     *
     * @param bmp
     * @param size
     * @return
     */
    @JvmOverloads
    fun compress(
        bmp: Bitmap?,
        size: Float,
        format: CompressFormat? = CompressFormat.JPEG
    ): ByteArray? {
        if (bmp == null) {
            return null
        }
        val output = ByteArrayOutputStream()
        val result: ByteArray
        return try {
            bmp.compress(format, 100, output) //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
            var options = 100
            while (output.toByteArray().size / 1024 >= size) {  //循环判断如果压缩后图片是否大于size kb,大于继续压缩
                output.reset() //重置baos即清空baos
                bmp.compress(format, options, output) //这里压缩options%，把压缩后的数据存放到baos中
                if (options == 1) {
                    break
                }
                options -= 10 //每次都减少10
                if (options <= 0) {
                    options = 1
                }
            }
            result = output.toByteArray()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                output.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun compressBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val rowByteCount = bitmap.byteCount
        val targetSize = Math.min(rowByteCount, maxSize)
        val sampleSize =
            Math.sqrt((targetSize / rowByteCount.toFloat()).toDouble())
        return Bitmap.createScaledBitmap(
            bitmap,
            (width * sampleSize).toInt(),
            (height * sampleSize).toInt(),
            true
        )
    }

    private const val MAX_DECODE_PICTURE_SIZE = 1920 * 1440
    fun extractThumbNail(
        path: String?, height: Int,
        width: Int, crop: Boolean
    ): Bitmap? {
        var options: BitmapFactory.Options? = BitmapFactory.Options()
        try {
            options!!.inJustDecodeBounds = true
            var tmp = BitmapFactory.decodeFile(path, options)
            if (tmp != null) {
                tmp.recycle()
                tmp = null
            }
            val beY = options.outHeight * 1.0 / height
            val beX = options.outWidth * 1.0 / width
            // Logger.d("Utility", "extractThumbNail: extract beX = " + beX
            // + ", beY = " + beY);
            options.inSampleSize =
                (if (crop) (if (beY > beX) beX else beY) else if (beY < beX) beX else beY).toInt()
            if (options.inSampleSize <= 1) {
                options.inSampleSize = 1
            }

            // NOTE: out of memory error
            while (options.outHeight * options.outWidth / options.inSampleSize
                > MAX_DECODE_PICTURE_SIZE
            ) {
                options.inSampleSize++
            }
            var newHeight = height
            var newWidth = width
            if (crop) {
                if (beY > beX) {
                    newHeight = (newWidth * 1.0 * options.outHeight / options.outWidth).toInt()
                } else {
                    newWidth = (newHeight * 1.0 * options.outWidth / options.outHeight).toInt()
                }
            } else {
                if (beY < beX) {
                    newHeight = (newWidth * 1.0 * options.outHeight / options.outWidth).toInt()
                } else {
                    newWidth = (newHeight * 1.0 * options.outWidth / options.outHeight).toInt()
                }
            }
            options.inJustDecodeBounds = false
            Log.i(
                "Utility", "bitmap required size=" + newWidth + "x"
                        + newHeight + ", orig=" + options.outWidth + "x"
                        + options.outHeight + ", sample=" + options.inSampleSize
            )
            var bm = BitmapFactory.decodeFile(path, options)
            if (bm == null) {
                Log.e("Utility", "bitmap decode failed")
                return null
            }
            Log.i(
                "Utility",
                "bitmap decoded size=" + bm.width + "x"
                        + bm.height
            )
            val scale = Bitmap.createScaledBitmap(
                bm, newWidth,
                newHeight, true
            )
            /**
             * Creates a new bitmap, scaled from an existing bitmap, when possible.
             * If the specified width and height are the same as the current width and height of
             * the source btimap,
             * the source bitmap is returned and now new bitmap is created.
             * 修复bug,如果原图尺寸与新图尺寸一致，则新图直接使用原图bitmap返回，如果原图被recycle掉，后面使用新图会出bug
             */
            if (scale != null && bm != null && (bm.width != scale.width
                        || bm.height != scale.height)
            ) {
                bm.recycle()
                bm = scale
            }
            if (crop) {
                val cropped = Bitmap.createBitmap(
                    bm,
                    bm.width - width shr 1,
                    bm.height - height shr 1, width, height
                ) ?: return bm
                bm.recycle()
                bm = cropped
                Log.i(
                    "Utility", "bitmap croped size=" + bm.width + "x"
                            + bm.height
                )
            }
            return bm
        } catch (e: OutOfMemoryError) {
            Log.e("Utility", "decode bitmap failed: " + e.message)
            options = null
        }
        return null
    }

    fun getBitmapWithPath(path: String?): Bitmap? {
        val coverFile = File(path)
        if (!coverFile.exists()) {
            return null
        }
        var bmp: Bitmap? = null
        var fis: FileInputStream? = null
        val opts = BitmapFactory.Options()
        opts.inPurgeable = true
        try {
            fis = FileInputStream(coverFile)
            bmp = BitmapFactory.decodeStream(fis, null, opts)
        } catch (e: IOException) {
        } finally {
            try {
                if (null != fis) {
                    fis.close()
                    fis = null
                }
            } catch (e: Exception) {
                Log.e("Utility", "decodeWithOptions error : $e")
            }
        }
        return bmp
    }

    fun saveBitmap(bitmap: Bitmap?, path: String, format: CompressFormat): Boolean {
        return saveBitmap(bitmap, path, 100, format)
    }

    /**
     * 保存Bitmap到本地
     *
     * @param bitmap
     * @param path
     * @param quality 0~100 压缩质量
     * @return
     */
    private fun saveBitmap(
        bitmap: Bitmap?,
        path: String,
        quality: Int,
        format: CompressFormat
    ): Boolean {
        var quality = quality
        var result = false
        if (bitmap != null && !TextUtils.isEmpty(path)) {
            val saveFile = File(path)
            //检查父目录
            if (!saveFile.parentFile.exists()) {
                saveFile.parentFile.mkdirs()
            }
            //删除指定文件
            if (saveFile.exists()) {
                saveFile.delete()
            }
            if (quality > 100) {
                quality = 100
            }
            var bos: BufferedOutputStream? = null
            try {
                bos = BufferedOutputStream(FileOutputStream(saveFile))
                bitmap.compress(format, quality, bos)
                result = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (bos != null) {
                    try {
                        bos.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return result
    }

    /**
     * 从本地文件中加载bitmap
     *
     * @param path
     * @return
     */
    fun createBitmapFromFile(path: String?): Bitmap? {
        if (!TextUtils.isEmpty(path)) {
            try {
                return BitmapFactory.decodeFile(path)
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * bitmap 转 byte[]
     *
     * @param bitmap bitmap
     * @return byte[]
     */
    fun bitmapToByteArray(bitmap: Bitmap?): ByteArray? {
        if (bitmap == null) {
            return null
        }
        var baos: ByteArrayOutputStream? = null
        try {
            baos = ByteArrayOutputStream()
            bitmap.compress(CompressFormat.PNG, 100, baos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                baos!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return baos?.toByteArray()
    }

    /**
     * 图片后期处理
     */
    interface BitmapPostProcess {
        fun processBitmap(canvas: Canvas?)
    }
}
