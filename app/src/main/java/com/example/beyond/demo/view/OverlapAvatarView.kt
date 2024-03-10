package com.example.beyond.demo.view

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.example.beyond.demo.R

/**
 * Created by p_dmweidu on 2023/5/28
 * Desc: 显示多个头像的View，根据传入的列表数量显示。
 */
class OverlapAvatarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {

    private val TAG = "OverlapAvatarView"

    private var mRadius = 0f
    private var borderColor = 0
    private var borderWidth = 0f
    private var avatarSize = 40f
    private var overlapSize = 10f
    private var overlapType = TYPE_RIGHT_TO_LEFT
    companion object {
        /**
         * 图片从右到左排列
         */
        private const val TYPE_RIGHT_TO_LEFT = 0
        /**
         * 图片从左到右排列
         */
        private const val TYPE_LIFE_TO_RIGHT = 1
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.OverlapAvatarView)
        mRadius = a.getDimension(R.styleable.OverlapAvatarView_avatar_round_width, 0f)
        borderColor = a.getColor(R.styleable.OverlapAvatarView_avatar_border_color, borderColor)
        borderWidth = a.getDimension(R.styleable.OverlapAvatarView_avatar_border_width, borderWidth)
        avatarSize = a.getDimension(R.styleable.OverlapAvatarView_avatar_size, avatarSize)
        overlapSize = a.getDimension(R.styleable.OverlapAvatarView_avatar_overlap_size, overlapSize)
        overlapType = a.getInt(R.styleable.OverlapAvatarView_avatar_overlap_type, overlapType)
        a.recycle()
    }


    /**
     * 设置头像数据
     *
     * @param avatarList 头像列表
     */
    fun setAvatarList(avatarList: List<String?>?) {
        if (context == null) {
            return
        }
        removeAllViews()
        if (avatarList.isNullOrEmpty()) {
            return
        }
        if (overlapType == TYPE_LIFE_TO_RIGHT) {
            for (index in avatarList.indices) {
                addAvatarView(avatarList, index)
            }
        } else {
            for (index in avatarList.size - 1 downTo 0) {
                addAvatarView(avatarList, index)
            }
        }
    }

    private fun addAvatarView(avatarList: List<String?>, index: Int) {
        val imageView = RoundImageView(context).apply {
            setRadius(avatarSize.div(2f))
            setBorderColor(borderColor)
            setBorderWidth(borderWidth)
        }
        val layoutParams = LayoutParams(avatarSize.toInt(), avatarSize.toInt())
        var marginStart = (avatarSize - overlapSize) * index
        if (marginStart < 0) {
            marginStart = 0f
        }
        layoutParams.marginStart = marginStart.toInt()
        addView(imageView, layoutParams)
        Glide.with(context).load(avatarList[index]).into(imageView)

    }

}