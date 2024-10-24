package com.example.beyond.demo.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.beyond.demo.R
import java.lang.ref.WeakReference

/**
 * 帧动画。可以使用 AnimationDrawable（ic_audio_dancing.xml） 替代
 *
 * @author wangshichao
 * @date 2024/10/18
 */
class AudioTrackAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    companion object {
        private const val TAG = "AudioTrackAnimationView"
        private const val WHAT_REFRESH = 0x1234
    }

    /**
     * 音频跳动动画
     */
    private val audioAnimationDrawable by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_audio_dancing) as AnimationDrawable
    }
    private val mBounds = Rect()
    private var frameIndex = 0
    private var isRunning = false
    private val handler by lazy { MyHandler(WeakReference(this)) }

    fun play() {
        if (isRunning) {
            return
        }
        Log.i(TAG, "start: frameIndex=$frameIndex")
        isRunning = true
        invalidate()
    }

    fun stop() {
        if (isRunning.not()) {
            return
        }
        Log.i(TAG, "stop: frameIndex=$frameIndex")
        handler.removeCallbacksAndMessages(null)
        frameIndex = 0
        isRunning = false
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            mBounds.right = w
            mBounds.bottom = h
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val drawable = getCurFrameDrawable()
        drawable.draw(canvas)
        if (isRunning) {
            handler.sendEmptyMessageDelayed(WHAT_REFRESH, 100)
        }
    }

    private fun getCurFrameDrawable(): Drawable {
        Log.w(TAG, "getCurFrameDrawable frameIndex=$frameIndex}")
        if (frameIndex < 0 || frameIndex >= audioAnimationDrawable.numberOfFrames) {
            frameIndex = 0
        }
        val drawable = audioAnimationDrawable.getFrame(frameIndex)
        drawable?.setBounds(0, 0, mBounds.right, mBounds.bottom)
        frameIndex++
        if (frameIndex == audioAnimationDrawable.numberOfFrames) {
            frameIndex = 0
        }
        return drawable
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }

    class MyHandler(private val weakReference: WeakReference<AudioTrackAnimationView>) : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == WHAT_REFRESH) {
                weakReference.get()?.invalidate()
            }
        }
    }
}