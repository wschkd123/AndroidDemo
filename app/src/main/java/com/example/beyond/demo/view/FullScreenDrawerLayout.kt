package com.example.beyond.demo.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

class FullScreenDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : DrawerLayout(context, attrs) {


    private var mInitialMotionX = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> mInitialMotionX = ev.x
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - mInitialMotionX
                // 从左向右滑动超过阈值，且抽屉未打开，则打开抽屉
                if (dx > touchSlop && !isDrawerOpen(GravityCompat.START)) {
                    openDrawer(GravityCompat.START)
                    return true
                }
            }
        }
        return super.onTouchEvent(ev)
    }

}