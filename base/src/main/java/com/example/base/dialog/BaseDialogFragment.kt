package com.example.base.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.FloatRange
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 * @author zhanglulu on 2020/1/14.
 * for DialogFragment 基类 的，封装一些通用的接口和方法 <br/>
 *
 * DialogFragment 生命周期：
 * show -> onCreate -> onCreateDialog -> onActivityCreated -> onStart -> 显示 Dialog
 * dismiss -> onStop -> onDestroy
 */

open class BaseDialogFragment : DialogFragment() {

    protected var TAG = javaClass.simpleName

    /**
     * 取消监听
     */
    private var onDialogDismissListener: OnDismissListener? = null

    /**
     *是否有焦点
     */
    private var isHasFocusable = true

    /**
     * 显示位置
     */
    var gravity: Int = Gravity.CENTER

    /**
     * 弹窗动画
     */
    @StyleRes
    var windowAnimStyleRes: Int? = null

    /**
     * 是否需要背景变暗
     */
    private var enableDimBehind: Boolean = true

    /**
     * 背景变暗之后的透明度，当且仅当 isDimBehind=true 时生效 <br/>
     */
    @FloatRange(from = 0.0, to = 1.0)
    var dimBehindAlpha: Float = 0.5F

    /**
     * 是否全屏
     */
    var isFullScreen = false

    /**
     * 是否正在展示
     */
    var isShowing = false

    /**
     * 当前 Dialog 的 Window
     */
    var window: Window? = null

    /**
     * 设置 Dialog 的 padding，解决在某些机型上 Dialog 会有默认 padding 的问题
     */
    var padding: Int? = null

    var onCancelClickListener: View.OnClickListener? = null
    var onSureClickListener: View.OnClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        window = dialog.window
        //解决顶部有默认标题的问题 ！！！
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        window = dialog?.window
        window?.let {
            setupWindowAttribute(it)
        }
    }


    private fun setupWindowAttribute(window: Window) {
        window.setGravity(gravity)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        if (!enableDimBehind) {
            window.attributes.flags =
                    //inv 取非 (~)
                window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        }
        if (!isHasFocusable) {
            window.attributes.flags =
                window.attributes.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        if (isFullScreen) {
            window.attributes.flags =
                window.attributes.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        } else {
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        window.attributes.dimAmount = dimBehindAlpha
        windowAnimStyleRes?.let { window.setWindowAnimations(it) }

        padding?.let {
            window.decorView.setPadding(it, it, it, it)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        isShowing = false
        super.onDismiss(dialog)
        onDialogDismissListener?.onDismiss()
    }

    public fun setOnDialogDismissListener(listener: OnDismissListener) {
        onDialogDismissListener = listener
    }


    /**
     * 展示 Dialog
     */
    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, TAG)
    }

    /**
     * 修复可能会引起 Fragment Already add 异常
     */
    override fun show(manager: FragmentManager, tag: String?) {
        if (isAdded || isShowing) {
            return
        }
        try {
            //fix Can not perform this action after onSaveInstanceState
            //由于底层使用 commit or commitNow，没有 commitAllowingStateLoss 提交方式
            super.show(manager, tag)
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface OnDismissListener {
        fun onDismiss()
    }

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}