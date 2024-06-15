package com.example.base.util.ext

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.base.util.YWCommonUtil
import java.math.BigDecimal

/**
 * kotlin 扩展函数
 *
 * @author p_jruixu
 */

/**
 * dp 转为 px
 */
fun Int.dpToPx() = YWCommonUtil.dp2px(this.toFloat())
fun Int.dpToPxFloat() = YWCommonUtil.dp2pxFloat(this.toFloat())

/**
 * 颜色资源 id 转为 颜色
 */
fun Int.resToColor(context: Context) = ContextCompat.getColor(context, this)

/**
 * 尺寸资源 id 转为 像素
 */
fun Int.resToDimen(context: Context) = context.resources.getDimension(this)

/**
 * 图片资源 id 转为 图片
 */
fun Int.resToDrawable(context: Context): Drawable? = context.resources.getDrawable(this)

/**
 * 文字资源 id 转为 文字
 */
fun Int.resToString(context: Context, vararg params: Any): String = context.getString(this, *params)

/**
 * 给某个颜色增加透明度
 *
 * [alphaPercent] 不透明度百分比
 */
fun Int.alpha(alphaPercent: Float): Int {
    val alpha = Color.alpha(this)
    val red = Color.red(this)
    val green = Color.green(this)
    val blue = Color.blue(this)

    val newAlpha = (alpha * alphaPercent).toInt()
    return Color.argb(newAlpha, red, green, blue)
}

/**
 * Float转Int透明度
 */
fun Float.toIntAlpha() = (this * 255 + 0.5f).toInt()

/**
 * Float四舍五入
 */
fun Float.round() = (this + 0.5f).toInt()

/**
 * 分转换为元
 * 1. 1200分->12元
 * 2. 1212分->12.12元
 */
fun Long.toYuan(): String {

    return BigDecimal(this).movePointLeft(2).toPlainString()
}

fun String.toFloatSafe(): Float {
    return try {
        BigDecimal(this).toFloat()
    } catch (e: Exception) {
        0f
    }
}

fun String.toIntSafe(): Int {
    return try {
        this.toInt()
    } catch (e: Exception) {
        0
    }
}

/**
 * 给 View 设置为可见
 */
fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.isVisible() = this.visibility == View.VISIBLE

/**
 * 给 View 设置不可见
 */
fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.isInvisible() = this.visibility == View.INVISIBLE

/**
 * 给 View 设置为消失
 */
fun View.gone() {
    this.visibility = View.GONE
}

fun View.isGone() = this.visibility == View.GONE

/**
 * 通过 layoutRes 生成 View
 */
@JvmOverloads
fun Int.inflateLayoutRes(
    context: Context,
    root: ViewGroup? = null,
    attachToRoot: Boolean = (root != null)
): View =
    LayoutInflater.from(context).inflate(this, root, attachToRoot)