package com.yuewen.baseutil.ext

import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.RecyclerView

/**
 * @author wangshichao
 * @date 2023/4/15
 *
 */

/**
 * 获取EditText的内容，其中去除字符串两端的空格
 */
fun EditText.content(): String {
    return this.text?.trim()?.toString() ?: ""
}

/**
 * 设置EditText的内容，并将光标移动到最后。
 * 在setText之后setSelection之前，重新获取EditText长度，避免中间字符串长度变化导致IndexOutOfBoundsException: setSpan (156 ... 156) ends beyond length 12
 */
fun EditText.setTextWithSelection(text: CharSequence?) {
    this.setText(text)
    this.setSelection(this.text?.length ?: 0)
}

/**
 * 判断EditText是否为空。其中过滤了空格和回车
 */
fun EditText.isEmpty(): Boolean {
    return this.text?.trim()?.toString()?.isEmpty() ?: true
}

fun EditText.isNotEmpty(): Boolean {
    return !isEmpty()
}

fun RadioGroup.getCheckedRadioButtonIntTag(): Int? {
    return if (checkedRadioButtonId != View.NO_ID) {
        val rg = findViewById<RadioButton>(checkedRadioButtonId)
        (rg.tag as String).toInt()
    } else {
        null
    }
}

fun RadioGroup.isChecked(): Boolean {
    return checkedRadioButtonId != View.NO_ID
}

fun <T : RecyclerView> T.removeItemDecorations() {
    while (itemDecorationCount > 0) {
        removeItemDecorationAt(0)
    }
}

/**
 * Group添加点击事件
 */
fun Group.setAllOnClickListener(listener: View.OnClickListener?) {
    referencedIds.forEach { id -> rootView.findViewById<View>(id).setOnClickListener(listener) }
}