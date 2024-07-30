
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import androidx.media3.common.util.Assertions
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextOverlay
import com.example.base.util.ext.resToColor
import com.example.beyond.demo.R
import com.example.beyond.demo.ui.transformer.ChatMsgItem
import com.example.beyond.demo.ui.transformer.util.ChatBoxHelper
import kotlin.math.max
import kotlin.math.min


/**
 * @Author:Bandongdong
 * @Date: 2024/7/18  19:24
 * @Description:
 */
@SuppressLint("UnsafeOptInUsageError")
class TextLinesOverlay(
    val context: Context,
    chatMsg: ChatMsgItem,
) : TextOverlay() {
    private val TAG = javaClass.simpleName
    private var subString: String =
        "这是一个测试的字幕类.你猜猜是的哈哈的拉拉队开始拉开的距离卡斯蒂略看见啊世界看不到卡上看到巴斯克绝对不开机啊白色的空间吧思考....."
    var length: Int = 0
    private var startTimeUs: Long = 0L
    private var countTimes: Int = 0
    private var overlaySettings: OverlaySettings? = null
    private val chatBoxHelper = ChatBoxHelper(context, TAG, chatMsg)
    private var containerBitmap: Bitmap? = null

    init {
        if (chatMsg.text?.isNotEmpty() == true){
            this.subString = chatMsg.text
        }
        overlaySettings = OverlaySettings.Builder()
            // 覆盖物在视频中下部
            .setBackgroundFrameAnchor(0f, -0.3f)
            // 在原覆盖物下面的位置
            .setOverlayFrameAnchor(0f, -1f)
            .build()
    }
    override fun getText(presentationTimeUs: Long): SpannableString {
        // 首帧记录开始和结束时间
        if (startTimeUs <= 0L) {
            startTimeUs = presentationTimeUs
        }

        length = (max(0,presentationTimeUs - startTimeUs) / 100000).toInt()

        val text = SpannableString(
            subString.substring(
                0,
                min(length.toDouble(), (subString.length - 1).toDouble()).toInt()
            )
        )
        text.setSpan(
            ForegroundColorSpan(R.color.video_create_chat_content_text.resToColor(context)),
            0,
            min(length.toDouble(), (subString.length - 1).toDouble()).toInt(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return text
    }


    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings!!
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {

        val curLength = getText(presentationTimeUs).length
        countTimes = max(0,curLength/17 - 2)
        val overlayText: SpannableString = if (countTimes > 0){
            SpannableString(getText(presentationTimeUs).drop(countTimes*17))
        }else{
            getText(presentationTimeUs)
        }
        val textPaint = TextPaint()
        textPaint.textSize = 42f
        val staticLayout = StaticLayout(
            overlayText,
                textPaint,
                850,
                Layout.Alignment.ALIGN_NORMAL,
                Layout.DEFAULT_LINESPACING_MULTIPLIER,
                Layout.DEFAULT_LINESPACING_ADDITION,
                true
            )

        if (containerBitmap == null) {
            containerBitmap = chatBoxHelper.drawContainerView()
        }
        val canvas = Canvas(Assertions.checkNotNull(containerBitmap))
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.translate(60f, 90f)
        staticLayout.draw(canvas)
        return Assertions.checkNotNull(containerBitmap)
    }

}