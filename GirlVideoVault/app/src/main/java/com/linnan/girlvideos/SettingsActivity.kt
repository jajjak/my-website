package com.linnan.girlvideos

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var repository: VideoRepository
    private lateinit var gridRow: LinearLayout
    private lateinit var lockRow: TextView
    private lateinit var statsView: TextView
    private val gridButtons = mutableMapOf<GridSize, TextView>()

    private val setPinLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "PINを設定しました", Toast.LENGTH_SHORT).show()
        }
        refreshLockRow()
    }

    private val verifyToDisableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            repository.clearPin()
            Toast.makeText(this, "アプリロックを無効にしました", Toast.LENGTH_SHORT).show()
        }
        refreshLockRow()
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) exportTo(uri)
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importFrom(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Palette.Bg
        window.navigationBarColor = Palette.Bg
        repository = VideoRepository(this)
        setContentView(buildUi())
        refreshLockRow()
        refreshStats()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(Palette.Bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingDp(18, 18, 18, 32)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPaddingDp(0, 0, 0, 18)
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 30f
            setTextColor(Palette.Text)
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        }
        val title = TextView(this).apply {
            text = "設定"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Palette.Text)
            setPaddingDp(8, 0, 0, 0)
        }
        top.addView(back)
        top.addView(title)
        root.addView(top)

        statsView = sectionCard {
            textSize = 13.5f
            setTextColor(Palette.Muted)
        }
        root.addView(sectionLabel("統計"))
        root.addView(statsView)

        root.addView(sectionLabel("グリッド列数"))
        gridRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPaddingDp(0, 0, 0, 8)
        }
        listOf(
            GridSize.AUTO to "自動",
            GridSize.TWO to "2",
            GridSize.THREE to "3",
            GridSize.FOUR to "4",
            GridSize.FIVE to "5"
        ).forEach { (size, label) ->
            val btn = TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setPaddingDp(12, 0, 12, 0)
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { marginEnd = dp(6) }
                setOnClickListener {
                    repository.setGridSize(size)
                    updateGridButtons()
                }
            }
            gridButtons[size] = btn
            gridRow.addView(btn)
        }
        root.addView(gridRow)
        updateGridButtons()

        root.addView(sectionLabel("プライバシー"))
        lockRow = actionRow("アプリロック（PIN）", "") { toggleLock() }
        root.addView(lockRow)
        root.addView(actionRow("PINを変更", "") {
            if (repository.hasPin()) {
                setPinLauncher.launch(Intent(this, LockActivity::class.java).putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_VERIFY))
            } else {
                Toast.makeText(this, "先にアプリロックを有効にしてください", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(sectionLabel("バックアップ"))
        root.addView(actionRow("動画リストをエクスポート", "JSONファイルとして保存") {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())
            exportLauncher.launch("girl_video_vault_backup_$stamp.json")
        })
        root.addView(actionRow("動画リストをインポート", "エクスポートしたJSONを読み込み") {
            importLauncher.launch(arrayOf("application/json"))
        })

        root.addView(sectionLabel("データ"))
        root.addView(actionRow("すべてのデータを削除", "動画リストと設定を初期化します", danger = true) { confirmClearAll() })

        scroll.addView(root)
        return scroll
    }

    private fun sectionLabel(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 12.5f
        setTextColor(Palette.Muted)
        typeface = Typeface.DEFAULT_BOLD
        setPaddingDp(2, 18, 0, 8)
    }

    private fun sectionCard(block: TextView.() -> Unit): TextView = TextView(this).apply {
        block()
        background = roundedBg(Palette.Surface, 16f, this@SettingsActivity, Palette.Stroke)
        setPaddingDp(16, 14, 16, 14)
    }

    private fun actionRow(label: String, sub: String, danger: Boolean = false, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = if (sub.isNotEmpty()) "$label\n$sub" else label
            textSize = 14.5f
            setTextColor(if (danger) Palette.Danger else Palette.Text)
            background = roundedBg(Palette.Surface, 16f, this@SettingsActivity, Palette.Stroke)
            setPaddingDp(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun updateGridButtons() {
        val current = repository.getGridSize()
        gridButtons.forEach { (size, btn) ->
            val selected = size == current
            btn.background = if (selected) roundedBg(Palette.Accent, 19f, this) else roundedBg(Palette.Surface, 19f, this, Palette.Stroke)
            btn.setTextColor(if (selected) Palette.AccentText else Palette.Text)
        }
    }

    private fun refreshLockRow() {
        val enabled = repository.hasPin()
        lockRow.text = if (enabled) "アプリロック（PIN）\n有効 - タップで無効化" else "アプリロック（PIN）\n無効 - タップで有効化"
    }

    private fun toggleLock() {
        if (repository.hasPin()) {
            verifyToDisableLauncher.launch(Intent(this, LockActivity::class.java).putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_VERIFY))
        } else {
            setPinLauncher.launch(Intent(this, LockActivity::class.java).putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_SET))
        }
    }

    private fun refreshStats() {
        val items = repository.load()
        val favCount = items.count { it.favorite }
        val totalMs = items.sumOf { it.durationMs }
        statsView.text = "動画: ${items.size}本  •  お気に入り: ${favCount}本\n合計再生時間: ${formatDuration(totalMs)}"
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        return if (h > 0) "${h}時間${m}分" else "${m}分"
    }

    private fun exportTo(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(repository.toJson(repository.load()).toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(this, "エクスポートしました", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "エクスポートに失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFrom(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            val imported = text?.let { repository.fromJson(it) }
            if (imported == null) {
                Toast.makeText(this, "ファイルを読み込めませんでした", Toast.LENGTH_SHORT).show()
                return
            }
            val current = repository.load()
            val existing = current.map { it.uri }.toHashSet()
            var added = 0
            imported.forEach { item ->
                if (item.uri !in existing) {
                    current.add(item)
                    existing.add(item.uri)
                    added++
                }
            }
            repository.save(current)
            refreshStats()
            Toast.makeText(this, "${added}件の動画を追加しました", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "インポートに失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle("すべてのデータを削除")
            .setMessage("動画リスト、お気に入り、設定をすべて削除します。この操作は取り消せません。（端末内の動画ファイル自体は削除されません）")
            .setPositiveButton("削除する") { _, _ ->
                repository.save(emptyList())
                repository.clearPin()
                AppLock.unlocked = true
                refreshStats()
                refreshLockRow()
                Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}
