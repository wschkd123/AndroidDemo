package com.example.beyond.demo.ui.transformer

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.Util
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.example.base.AppContext
import com.example.base.Init.getApplicationContext
import com.example.base.player.exoplayer.ExoPlayerWrapper
import com.example.base.util.YWDeviceUtil
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.R
import com.example.beyond.demo.databinding.FragmentTransformerBinding
import com.example.beyond.demo.ui.transformer.overlay.ChatFrameOverlay
import com.example.beyond.demo.ui.transformer.overlay.CoverOverlay
import com.example.beyond.demo.ui.transformer.overlay.FullscreenAlphaInOverlay
import com.example.beyond.demo.ui.transformer.overlay.FullscreenAlphaOutOverlay
import com.example.beyond.demo.ui.transformer.util.JsonUtil
import com.google.common.base.Stopwatch
import com.google.common.base.Ticker
import com.google.common.collect.ImmutableList
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit


/**
 *
 * @author wangshichao
 * @date 2024/7/10
 */
class TransformerFragment : Fragment() {

    companion object {
        private const val TAG = "TransformerFragment"
        private const val MP4_ASSET_URI_STRING = "asset:///media/mp4/sample.mp4"
        //        private const val MP4_ASSET_URI_STRING1 = "asset:///media/mp4/hdr10-720p.mp4"
        private const val FILE_AUDIO_ONLY = "asset:///media/mp3/bear-cbr-variable-frame-size-no-seek-table.mp3"
        private const val JPG_ASSET_URI_STRING = "asset:///media/img/london.jpg"
        private const val PNG_ASSET_URI_STRING = "asset:///media/img/img_background.png"
        private const val ONE_ONE_AVATAR = "https://zmdcharactercdn.zhumengdao.com/2365d825482a71b62b59a7db80b88fa2.jpg"
        private const val THREE_THREE_AVATAR = "https://zmdcharactercdn.zhumengdao.com/34487524784424960048.png"
        private const val NINE_SIXTEEN_AVATAR = "https://zmdcharactercdn.zhumengdao.com/34459418686279680012.png"
    }

    private var _binding: FragmentTransformerBinding? = null
    private val binding get() = _binding!!
//    private val videoItem: EditedMediaItem.Builder
//    private val imageItem: EditedMediaItem.Builder
//    private val audioItem: EditedMediaItem.Builder
    private val playerWrapper = ExoPlayerWrapper()
    private lateinit var exportStopwatch: Stopwatch
    private var outputFile: File? = null

    init {

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTransformerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exportStopwatch = Stopwatch.createUnstarted(
            object : Ticker() {
                override fun read(): Long {
                    return SystemClock.elapsedRealtimeNanos()
                }
            })
        // 播放器9:16
        binding.playerView.layoutParams = binding.playerView.layoutParams.apply {
            width = YWDeviceUtil.getScreenWidth()
            height = width * 16 / 9
        }
        binding.playerView.player = playerWrapper.player
        binding.tvOutput.setOnClickListener {
            start()
        }
    }

    private fun start() {
        outputFile =
            YWFileUtil.createNewFile(YWFileUtil.getStorageFileDir(context)?.path + "/" + System.currentTimeMillis() + ".mp4")
                ?: return
        val outputFilePath = outputFile!!.absolutePath
        val transformer: Transformer = createTransformer(outputFilePath)
        val composition: Composition = createComposition()

//        exportStopwatch.reset()
//        exportStopwatch.start()
        transformer.start(composition, outputFilePath)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerWrapper.release()
    }

    private fun createComposition(): Composition {
        val durationUs = 10_000_000L
        val videoEffects = createVideoEffects(durationUs)
        val imageItem = EditedMediaItem.Builder(MediaItem.fromUri(PNG_ASSET_URI_STRING))
            .setDurationUs(durationUs)
            .setFrameRate(TransformerConstant.FRAME_RATE)
            .setEffects(Effects(ImmutableList.of(), videoEffects))

        val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(MP4_ASSET_URI_STRING))

        val audioItem = EditedMediaItem.Builder(MediaItem.fromUri(FILE_AUDIO_ONLY))
            .setEffects(Effects(ImmutableList.of(), videoEffects))

        // video
        val videoSequence = EditedMediaItemSequence(
            mutableListOf(videoItem.build(), videoItem.build(), videoItem.build())
        )

        // audio
        val audioSequence = EditedMediaItemSequence(
            mutableListOf(audioItem.build()),
            false
        )

        // image
        val imageSequence = EditedMediaItemSequence(
            mutableListOf(imageItem.build())
        )
        val compositionBuilder =
            Composition.Builder(mutableListOf(imageSequence))
//            Composition.Builder(mutableListOf(videoSequence, audioSequence, imageSequence))
        return compositionBuilder.build()
    }

    private fun createTransformer(filePath: String): Transformer {
        val transformerBuilder: Transformer.Builder = Transformer.Builder(AppContext.application)
        // mp4格式
//        transformerBuilder.setTransformationRequest(
//            TransformationRequest.Builder().setVideoMimeType(MimeTypes.VIDEO_MP4).build()
//        )
        return transformerBuilder
            .addListener(
                object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        this@TransformerFragment.onCompleted(filePath, exportResult)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        this@TransformerFragment.onError(exportException)
                    }
                })
            .build()
    }

    private fun createVideoEffects(durationUs: Long): ImmutableList<Effect> {
        val effects = ImmutableList.Builder<Effect>()
//        effects.add(MatrixTransformationFactory.createTransition())
        // 配置输出视频分辨率。需要放前面，后续Overlay中configure尺寸才生效
        effects.add(
            Presentation.createForWidthAndHeight(
            TransformerConstant.OUT_VIDEO_WIDTH, TransformerConstant.OUT_VIDEO_HEIGHT, Presentation.LAYOUT_SCALE_TO_FIT
        ))
        val overlayEffect: OverlayEffect? = createOverlayEffect(durationUs)
        if (overlayEffect != null) {
            effects.add(overlayEffect)
        }
        return effects.build()
    }

    private fun createOverlayEffect(durationUs: Long): OverlayEffect? {
        if (context == null) return null
        val overlaysBuilder = ImmutableList.Builder<TextureOverlay>()
        var startTime: Long = 0
        val coverDuration = 400_000L
        val characterBgDuration = 800_000L
        // 视频封面
        overlaysBuilder.add(CoverOverlay(requireContext(), THREE_THREE_AVATAR, startTime, coverDuration))
        startTime += coverDuration
        // A背景图渐隐
        overlaysBuilder.add(FullscreenAlphaOutOverlay(requireContext(), ONE_ONE_AVATAR, startTime, characterBgDuration))
        startTime += characterBgDuration
        // B背景图渐显
        overlaysBuilder.add(FullscreenAlphaInOverlay(requireContext(), NINE_SIXTEEN_AVATAR, startTime, characterBgDuration))
        startTime += characterBgDuration

//        overlaysBuilder.add(ImageOverlay(requireContext()))
        overlaysBuilder.add(ChatFrameOverlay(startTime, characterBgDuration))
        return OverlayEffect(overlaysBuilder.build())
    }


    private fun onCompleted(filePath: String, exportResult: ExportResult) {
//        exportStopwatch.stop()
        val elapsedTimeMs: Long = exportStopwatch.elapsed(TimeUnit.MILLISECONDS)
        binding.informationTextView.text =
            getString(R.string.export_completed, elapsedTimeMs / 1000f, filePath)
        playerWrapper.addMediaItem("file://$filePath")
        Log.d(
            TAG,
            "Output file path: file://$filePath"
        )
        try {
            val resultJson: JSONObject = JsonUtil.exportResultAsJsonObject(exportResult)
                .put("elapsedTimeMs", elapsedTimeMs)
                .put("device", JsonUtil.getDeviceDetailsAsJsonObject())
            for (line in Util.split(resultJson.toString(2), "\n")) {
                Log.d(TAG, line)
            }
        } catch (e: JSONException) {
            Log.d(
                TAG,
                "Unable to convert exportResult to JSON",
                e
            )
        }
    }

    private fun onError(exportException: ExportException) {
//        exportStopwatch.stop()
        binding.informationTextView.text = "Export error"
        Toast.makeText(getApplicationContext(), "Export error: $exportException", Toast.LENGTH_LONG)
            .show()
        Log.e(
            TAG,
            "Export error",
            exportException
        )
    }

}
