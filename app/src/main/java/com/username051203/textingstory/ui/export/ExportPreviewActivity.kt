package com.username051203.textingstory.ui.export

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.*
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.username051203.textingstory.R
import com.username051203.textingstory.TextingStoryApp
import com.username051203.textingstory.data.model.Character
import com.username051203.textingstory.data.model.Message
import com.username051203.textingstory.databinding.ActivityExportPreviewBinding
import com.username051203.textingstory.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExportPreviewActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_STORY_ID = "extra_story_id"
    }

    private lateinit var binding: ActivityExportPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefsManager.applyTheme(this)
        binding = ActivityExportPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.create_video)

        val storyId = intent.getLongExtra(EXTRA_STORY_ID, -1L)
        val repo = (application as TextingStoryApp).repository
        val prefs = PrefsManager.get(this)
        val videoSpeed = prefs.getInt(PrefsManager.KEY_VIDEO_SPEED, 2)

        binding.btnExport.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnExport.isEnabled = false
            lifecycleScope.launch {
                try {
                    val story = repo.getStoryById(storyId)!!
                    val chars = repo.getCharactersForStoryOnce(storyId)
                    val messages = repo.getMessagesForStoryOnce(storyId)
                    val outputFile = withContext(Dispatchers.IO) {
                        ChatVideoRenderer.render(
                            context = this@ExportPreviewActivity,
                            story = story,
                            characters = chars,
                            messages = messages,
                            speedMultiplier = videoSpeed
                        )
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.btnExport.isEnabled = true
                    // Share the file
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this@ExportPreviewActivity,
                        "${packageName}.provider",
                        outputFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_video)))
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnExport.isEnabled = true
                    Toast.makeText(this@ExportPreviewActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

/**
 * Renders the chat as an MP4 using Canvas + MediaMuxer.
 * No screen recording — pure programmatic draw.
 * Each "frame" draws the chat up to message N with a typing animation.
 */
object ChatVideoRenderer {
    private const val WIDTH = 720
    private const val HEIGHT = 1280
    private const val FRAME_RATE = 30
    private const val BIT_RATE = 2_000_000
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC

    fun render(
        context: Context,
        story: com.username051203.textingstory.data.model.Story,
        characters: List<Character>,
        messages: List<Message>,
        speedMultiplier: Int
    ): File {
        val outDir = File(context.getExternalFilesDir(null), "videos")
        outDir.mkdirs()
        val outFile = File(outDir, "story_${story.id}_${System.currentTimeMillis()}.mp4")

        val charMap = characters.associateBy { it.id }
        val isDark = PrefsManager.get(context).getBoolean(PrefsManager.KEY_DARK_MODE, true)
        val bgColor = if (isDark) Color.BLACK else Color.WHITE
        val textColorDefault = if (isDark) Color.WHITE else Color.BLACK

        // MediaCodec setup
        val format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(MIME_TYPE)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = codec.createInputSurface()
        codec.start()

        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()

        // Frame timing: each message appears over ~60 frames (2s at 30fps), adjusted by speed
        val framesPerMessage = (FRAME_RATE * 2) / speedMultiplier
        val typingFrames = (FRAME_RATE * 1) / speedMultiplier // 1s typing dots
        val totalFrames = messages.size * (framesPerMessage + typingFrames)

        var presentationTimeUs = 0L
        val frameIntervalUs = 1_000_000L / FRAME_RATE

        val visibleMessages = mutableListOf<Message>()

        for (frameIndex in 0 until totalFrames) {
            val msgIndex = frameIndex / (framesPerMessage + typingFrames)
            val frameInMsg = frameIndex % (framesPerMessage + typingFrames)
            val isTypingFrame = frameInMsg < typingFrames

            if (frameInMsg == typingFrames && msgIndex < messages.size) {
                visibleMessages.add(messages[msgIndex])
            }

            // Draw frame
            val canvas = surface.lockHardwareCanvas()
            try {
                // Background
                canvas.drawColor(bgColor)

                // Draw all visible messages
                drawMessages(canvas, visibleMessages, charMap, isDark, textColorDefault)

                // Draw typing indicator if in typing phase
                if (isTypingFrame && msgIndex < messages.size) {
                    val typingChar = messages[msgIndex].characterId?.let { charMap[it] }
                    drawTypingIndicator(canvas, typingChar, isDark)
                }
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }

            // Drain encoder
            drainEncoder(codec, muxer, bufferInfo, false, presentationTimeUs) { idx ->
                trackIndex = idx; muxerStarted = true
            }
            presentationTimeUs += frameIntervalUs
        }

        // End of stream
        drainEncoder(codec, muxer, bufferInfo, true, presentationTimeUs) {}

        codec.stop()
        codec.release()
        surface.release()
        if (muxerStarted) muxer.stop()
        muxer.release()

        return outFile
    }

    private fun drawMessages(
        canvas: Canvas,
        messages: List<Message>,
        charMap: Map<Long, Character>,
        isDark: Boolean,
        textColorDefault: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 38f; color = Color.WHITE }
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        var y = 80f
        val maxY = HEIGHT - 120f
        val startIdx = if (messages.size > 12) messages.size - 12 else 0

        for (i in startIdx until messages.size) {
            val msg = messages[i]
            if (y > maxY) break

            if (msg.type == "COMMENT") {
                paint.color = Color.GRAY
                paint.textSize = 32f
                canvas.drawText(msg.text, WIDTH / 2f - paint.measureText(msg.text) / 2, y + 30, paint)
                y += 60f
                continue
            }

            val char = msg.characterId?.let { charMap[it] }
            val isRight = char?.side == "RIGHT"
            val bubbleColor = try { Color.parseColor(char?.bubbleColor ?: "#FF6FD8") } catch (e: Exception) { Color.MAGENTA }
            val txtColor = try { Color.parseColor(char?.textColor ?: "#FFFFFF") } catch (e: Exception) { Color.WHITE }

            paint.textSize = 38f
            paint.color = txtColor
            bubblePaint.color = bubbleColor

            val textW = paint.measureText(msg.text).coerceAtMost(520f)
            val bubbleW = textW + 60f
            val bubbleH = 80f

            val left = if (isRight) WIDTH - bubbleW - 80f else 100f
            val rect = RectF(left, y, left + bubbleW, y + bubbleH)
            canvas.drawRoundRect(rect, 40f, 40f, bubblePaint)
            canvas.drawText(msg.text, left + 30f, y + 52f, paint)
            y += bubbleH + 20f
        }
    }

    private fun drawTypingIndicator(canvas: Canvas, char: Character?, isDark: Boolean) {
        val color = try { Color.parseColor(char?.bubbleColor ?: "#888888") } catch (e: Exception) { Color.GRAY }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val isRight = char?.side == "RIGHT"
        val x = if (isRight) (WIDTH - 160f) else 110f
        val y = HEIGHT - 200f
        repeat(3) { i ->
            canvas.drawCircle(x + i * 30f, y, 14f, paint)
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        ptsUs: Long,
        onTrackAdded: (Int) -> Unit
    ) {
        if (endOfStream) codec.signalEndOfInputStream()
        while (true) {
            val encoderStatus = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val idx = muxer.addTrack(codec.outputFormat)
                onTrackAdded(idx)
                muxer.start()
            } else if (encoderStatus >= 0) {
                val encodedData = codec.getOutputBuffer(encoderStatus) ?: continue
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }
                if (bufferInfo.size > 0) {
                    bufferInfo.presentationTimeUs = ptsUs
                    muxer.writeSampleData(encoderStatus, encodedData, bufferInfo)
                }
                codec.releaseOutputBuffer(encoderStatus, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
    }
}

// Minimal foreground service placeholder (required for API compat)
class RecordingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = NotificationChannel("recording", "Recording", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notif = NotificationCompat.Builder(this, "recording")
            .setContentTitle("Exporting video...")
            .setSmallIcon(R.drawable.ic_person)
            .build()
        startForeground(1, notif)
        return START_NOT_STICKY
    }
}
