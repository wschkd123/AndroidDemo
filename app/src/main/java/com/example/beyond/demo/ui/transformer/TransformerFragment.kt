package com.example.beyond.demo.ui.transformer

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.Util
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
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
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.R
import com.example.beyond.demo.databinding.FragmentTransformerBinding
import com.example.beyond.demo.ui.transformer.overlay.AlphaInOverlay
import com.example.beyond.demo.ui.transformer.overlay.AlphaOutOverlay
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
        private const val FILE_AUDIO_ONLY = "asset:///media/mp3/test-cbr-info-header.mp3"
        const val JPG_ASSET_URI_STRING = "asset:///media/jpeg/london.jpg"
    }

    private var _binding: FragmentTransformerBinding? = null
    private val binding get() = _binding!!
    private val audioItem = EditedMediaItem.Builder(MediaItem.fromUri(FILE_AUDIO_ONLY))
        .build()
    private val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(MP4_ASSET_URI_STRING))
    private val imageItem = EditedMediaItem.Builder(MediaItem.fromUri(JPG_ASSET_URI_STRING))
        .setDurationUs(5_000_000)
        .setFrameRate(TransformerConstant.FRAME_RATE)
    private val playerWrapper = ExoPlayerWrapper()
    private lateinit var exportStopwatch: Stopwatch
    private var outputFile: File? = null

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

        exportStopwatch.reset()
        exportStopwatch.start()
        transformer.start(composition, outputFilePath)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerWrapper.release()
    }

    private fun createComposition(): Composition {
        // video
        val videoEffects = createVideoEffects()
//        videoItem.setEffects(Effects(ImmutableList.of(), videoEffects))
        val videoSequence = EditedMediaItemSequence(
            mutableListOf(videoItem.build(), videoItem.build(), videoItem.build())
        )
        val audioSequence = EditedMediaItemSequence(
            mutableListOf(audioItem),
            false
        )

        // image
        imageItem.setEffects(Effects(ImmutableList.of(), videoEffects))
        val imageSequence = EditedMediaItemSequence(
            mutableListOf(imageItem.build())
        )
        val compositionBuilder =
            Composition.Builder(mutableListOf(/*videoSequence, audioSequence, */imageSequence))
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

    private fun createVideoEffects(): ImmutableList<Effect> {
        val effects = ImmutableList.Builder<Effect>()
//        effects.add(MatrixTransformationFactory.createTransition())
        val overlayEffect: OverlayEffect? = createOverlayEffect()
        if (overlayEffect != null) {
            effects.add(overlayEffect)
        }
        // 视频分辨率
        effects.add(Presentation.createForWidthAndHeight(
            TransformerConstant.OUT_VIDEO_WIDTH, TransformerConstant.OUT_VIDEO_HEIGHT, Presentation.LAYOUT_SCALE_TO_FIT
        ))
        return effects.build()
    }

    private fun createOverlayEffect(): OverlayEffect? {
        if (context == null) return null
        val overlaysBuilder = ImmutableList.Builder<TextureOverlay>()
        overlaysBuilder.add(
            AlphaInOverlay(requireContext(), 0f, 2f),
            AlphaOutOverlay(requireContext(), 2f * C.MICROS_PER_SECOND, 3f),
//            ChatContentOverlay(),
//            BgOverlay(context)
        )
        val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(
            getApplicationContext(),
            Uri.parse("https://zmdcharactercdn.zhumengdao.com/92f03872cb959f4a763727e19d70ae38.png"),
            OverlaySettings.Builder().build()
        )
        overlaysBuilder.add(bitmapOverlay)
        val overlays: ImmutableList<TextureOverlay> = overlaysBuilder.build()
        return (if (overlays.isEmpty()) null else OverlayEffect(overlays))
    }


    private fun onCompleted(filePath: String, exportResult: ExportResult) {
        exportStopwatch.stop()
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
        exportStopwatch.stop()
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
