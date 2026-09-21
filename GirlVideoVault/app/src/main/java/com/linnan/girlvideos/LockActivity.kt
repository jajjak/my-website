package com.linnan.girlvideos

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity

class LockActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_SET = "set"
        const val MODE_VERIFY = "verify"
        private const val PIN_LENGTH = 4
        private const val MAX_ATTEMPTS = 5
        private const val COOLDOWN_MS = 15_000L
    }

    private lateinit var repository: VideoRepository
    private lateinit var dotsRow: LinearLayout
    private lateinit var dots: List<View>
    private lateinit var messageView: TextView
    private lateinit var keypad: GridLayout

    private var mode = MODE_VERIFY
    private var buffer = StringBuilder()
    private var firstEntry: String? = null
    private var confirming = false
    private var failedAttempts = 0
    private var cooldownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Palette.Bg
        window.navigationBarColor = Palette.Bg
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_VERIFY
        repository = VideoRepository(this)
        setContentView(buildUi())
        updateMessage()

        onBackPressedDispatcher.addCallback(this) {
            if (mode == MODE_VERIFY) {
                moveTaskToBack(true)
            } else {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun buildUi(): View {
        val root = FrameLayout(this).apply { setBackgroundColor(Palette.Bg) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPaddingDp(24, 64, 24, 24)
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val icon = TextView(this).apply {
            text = "🔒"
            textSize = 36f
            gravity = Gravity.CENTER
        }
        val title = TextView(this).apply {
            text = "女の子保存動画"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Palette.Text)
            gravity = Gravity.CENTER
            setPaddingDp(0, 10, 0, 6)
        }
        messageView = TextView(this).apply {
            textSize = 13.5f
            setTextColor(Palette.Muted)
            gravity = Gravity.CENTER
            setPaddingDp(0, 0, 0, 26)
        }

        dotsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPaddingDp(0, 0, 0, 40)
        }
        dots = (0 until PIN_LENGTH).map {
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { setMargins(dp(8), 0, dp(8), 0) }
                background = roundedBg(Palette.Stroke, 7f, this@LockActivity)
            }
        }
        dots.forEach { dotsRow.addView(it) }

        keypad = GridLayout(this).apply {
            columnCount = 3
            rowCount = 4
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val labels = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        labels.forEach { label ->
            val cell = TextView(this).apply {
                text = label
                textSize = 22f
                setTextColor(Palette.Text)
                gravity = Gravity.CENTER
                isClickable = label.isNotEmpty()
                isFocusable = label.isNotEmpty()
                if (label.isNotEmpty()) {
                    background = rippleBg(Palette.Surface, 34f, this@LockActivity)
                }
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dp(68)
                    height = dp(68)
                    setMargins(dp(10), dp(10), dp(10), dp(10))
                }
                when (label) {
                    "⌫" -> setOnClickListener { onBackspace() }
                    "" -> Unit
                    else -> setOnClickListener { onDigit(label) }
                }
            }
            keypad.addView(cell)
        }

        column.addView(icon)
        column.addView(title)
        column.addView(messageView)
        column.addView(dotsRow)
        column.addView(keypad)
        root.addView(column)
        return root
    }

    private fun updateMessage() {
        messageView.text = when {
            mode == MODE_SET && !confirming -> "新しいPINを入力"
            mode == MODE_SET && confirming -> "もう一度入力して確認"
            else -> "PINを入力してロック解除"
        }
    }

    private fun onDigit(d: String) {
        if (buffer.length >= PIN_LENGTH) return
        buffer.append(d)
        refreshDots()
        if (buffer.length == PIN_LENGTH) {
            keypad.postDelayed({ submit() }, 120)
        }
    }

    private fun onBackspace() {
        if (buffer.isEmpty()) return
        buffer.deleteCharAt(buffer.length - 1)
        refreshDots()
    }

    private fun refreshDots() {
        dots.forEachIndexed { i, dot ->
            dot.background = roundedBg(
                if (i < buffer.length) Palette.Accent else Palette.Stroke,
                7f,
                this
            )
        }
    }

    private fun submit() {
        val entered = buffer.toString()
        buffer.clear()
        if (mode == MODE_SET) {
            handleSetEntry(entered)
        } else {
            handleVerifyEntry(entered)
        }
    }

    private fun handleSetEntry(entered: String) {
        if (!confirming) {
            firstEntry = entered
            confirming = true
            updateMessage()
            refreshDots()
        } else {
            if (entered == firstEntry) {
                repository.setPin(entered)
                AppLock.unlocked = true
                setResult(RESULT_OK)
                finish()
            } else {
                confirming = false
                firstEntry = null
                messageView.text = "一致しません。もう一度入力"
                messageView.setTextColor(Palette.Danger)
                shakeDots()
                keypad.postDelayed({
                    messageView.setTextColor(Palette.Muted)
                    updateMessage()
                }, 900)
                refreshDots()
            }
        }
    }

    private fun handleVerifyEntry(entered: String) {
        if (repository.verifyPin(entered)) {
            AppLock.unlocked = true
            failedAttempts = 0
            setResult(RESULT_OK)
            finish()
        } else {
            failedAttempts++
            shakeDots()
            refreshDots()
            if (failedAttempts >= MAX_ATTEMPTS) {
                startCooldown()
            } else {
                messageView.text = "PINが違います（${MAX_ATTEMPTS - failedAttempts}回）"
                messageView.setTextColor(Palette.Danger)
            }
        }
    }

    private fun startCooldown() {
        setKeypadEnabled(false)
        cooldownTimer?.cancel()
        cooldownTimer = object : CountDownTimer(COOLDOWN_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                messageView.setTextColor(Palette.Danger)
                messageView.text = "試行回数が多すぎます。${(millisUntilFinished / 1000) + 1}秒後に再試行"
            }
            override fun onFinish() {
                failedAttempts = 0
                messageView.setTextColor(Palette.Muted)
                updateMessage()
                setKeypadEnabled(true)
            }
        }.start()
    }

    private fun setKeypadEnabled(enabled: Boolean) {
        for (i in 0 until keypad.childCount) {
            val child = keypad.getChildAt(i)
            if (child is TextView && child.text.isNotEmpty()) {
                child.isEnabled = enabled
                child.alpha = if (enabled) 1f else 0.35f
            }
        }
    }

    private fun shakeDots() {
        ObjectAnimator.ofFloat(dotsRow, "translationX", 0f, -18f, 18f, -14f, 14f, -6f, 6f, 0f).apply {
            duration = 420
            start()
        }
    }

    override fun onDestroy() {
        cooldownTimer?.cancel()
        super.onDestroy()
    }
}
