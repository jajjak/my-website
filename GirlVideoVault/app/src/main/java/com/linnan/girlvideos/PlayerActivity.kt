package com.linnan.girlvideos

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.min

class PlayerActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_URI = "uri"
        const val EXTRA_TITLE = "title"
        private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
        private const val SEEK_STEP_MS = 10_000
    }

    private lateinit var video: VideoView
    private lateinit var controls: LinearLayout
    private lateinit var seek: SeekBar
    private lateinit var play: TextView
    private lateinit var time: TextView
    private lateinit var loopButton: TextView
    private lateinit var speedButton: TextView
    private lateinit var flash: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var userSeeking = false
    private var mediaPlayer: MediaPlayer? = null
    private var speedIndex = 1
    private var looping = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!userSeeking && video.isPlaying || (!userSeeking && video.duration > 0)) {
                val d = max(video.duration, 0)
                val p = max(video.currentPosition, 0)
                if (d > 0) {
                    seek.max = d
                    seek.progress = p.coerceAtMost(d)
                    time.text = "${formatTime(p)} / ${formatTime(d)}"
                }
                play.text = if (video.isPlaying) "Ⅱ" else "▶"
            }
            handler.postDelayed(this, 400)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val uri = intent.getStringExtra(EXTRA_URI) ?: run { finish(); return }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        video = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        root.addView(video)

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleControls()
                return true
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val forward = e.x > video.width / 2f
                seekRelative(if (forward) SEEK_STEP_MS else -SEEK_STEP_MS)
                showFlash(if (forward) "+10秒 ⏩" else "⏪ -10秒")
                return true
            }
        })
        video.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event); true }

        flash = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = roundedBg(Color.argb(160, 0, 0, 0), 20f, this@PlayerActivity)
            setPaddingDp(18, 10, 18, 10)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }
        root.addView(flash)

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPaddingDp(12, 8, 12, 8)
            setBackgroundColor(Color.argb(150, 0, 0, 0))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(62), Gravity.TOP)
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { finish() }
            layoutParams = LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val titleView = TextView(this).apply {
            text = title
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        }
        top.addView(back)
        top.addView(titleView)
        root.addView(top)

        controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPaddingDp(10, 8, 10, 8)
            setBackgroundColor(Color.argb(175, 0, 0, 0))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(76), Gravity.BOTTOM)
        }
        play = TextView(this).apply {
            text = "▶"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Palette.AccentText)
            background = roundedBg(Palette.Accent, 24f, this@PlayerActivity)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            setOnClickListener {
                if (video.isPlaying) video.pause() else video.start()
                this@PlayerActivity.play.text = if (video.isPlaying) "Ⅱ" else "▶"
            }
        }
        seek = SeekBar(this).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(Palette.Accent)
            thumbTintList = android.content.res.ColorStateList.valueOf(Palette.Accent)
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                leftMargin = dp(6)
                rightMargin = dp(6)
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = Unit
                override fun onStartTrackingTouch(seekBar: SeekBar?) { userSeeking = true }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let { video.seekTo(it.progress) }
                    userSeeking = false
                }
            })
        }
        time = TextView(this).apply {
            text = "0:00 / 0:00"
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(84), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        speedButton = TextView(this).apply {
            text = "1.0x"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(42), ViewGroup.LayoutParams.MATCH_PARENT)
            setOnClickListener { cycleSpeed() }
        }
        loopButton = TextView(this).apply {
            text = "🔁"
            textSize = 18f
            setTextColor(Color.WHITE)
            alpha = 0.55f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.MATCH_PARENT)
            setOnClickListener { toggleLoop() }
        }
        val rotate = TextView(this).apply {
            text = "↻"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.MATCH_PARENT)
            setOnClickListener {
                requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }
        }
        controls.addView(play)
        controls.addView(seek)
        controls.addView(time)
        controls.addView(speedButton)
        controls.addView(loopButton)
        controls.addView(rotate)
        root.addView(controls)

        setContentView(root)
        video.setVideoURI(Uri.parse(uri))
        video.setOnPreparedListener { mp ->
            mediaPlayer = mp
            mp.isLooping = looping
            mp.setOnVideoSizeChangedListener { _, _, _ -> }
            seek.max = max(video.duration, 0)
            video.start()
            play.text = "Ⅱ"
            applySpeed()
        }
        video.setOnCompletionListener { play.text = "▶" }
        handler.post(ticker)
    }

    private fun seekRelative(deltaMs: Int) {
        val duration = max(video.duration, 0)
        if (duration <= 0) return
        val newPos = max(0, min(duration, video.currentPosition + deltaMs))
        video.seekTo(newPos)
    }

    private fun cycleSpeed() {
        speedIndex = (speedIndex + 1) % SPEEDS.size
        applySpeed()
        showFlash("${SPEEDS[speedIndex]}x")
    }

    private fun applySpeed() {
        val speed = SPEEDS[speedIndex]
        speedButton.text = "${speed}x"
        val mp = mediaPlayer ?: return
        try {
            val wasPlaying = video.isPlaying
            mp.playbackParams = PlaybackParams().setSpeed(speed)
            if (wasPlaying && !video.isPlaying) video.start()
        } catch (_: Exception) { }
    }

    private fun toggleLoop() {
        looping = !looping
        mediaPlayer?.isLooping = looping
        loopButton.alpha = if (looping) 1f else 0.55f
        showFlash(if (looping) "ループ再生 ON" else "ループ再生 OFF")
    }

    private fun showFlash(text: String) {
        flash.text = text
        flash.visibility = View.VISIBLE
        flash.animate().cancel()
        flash.alpha = 1f
        handler.removeCallbacks(hideFlash)
        handler.postDelayed(hideFlash, 650)
    }

    private val hideFlash = Runnable {
        flash.animate().alpha(0f).setDuration(200).withEndAction { flash.visibility = View.GONE }.start()
    }

    private fun toggleControls() {
        controls.visibility = if (controls.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { video.stopPlayback() } catch (_: Exception) { }
        super.onDestroy()
    }

    private fun formatTime(ms: Int): String {
        val total = max(0, ms / 1000)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
