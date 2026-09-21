package com.linnan.girlvideos

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var repository: VideoRepository
    private lateinit var adapter: VideoAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var empty: LinearLayout
    private lateinit var counter: TextView
    private lateinit var search: EditText
    private lateinit var allChip: TextView
    private lateinit var favChip: TextView
    private lateinit var recentChip: TextView
    private lateinit var sortButton: TextView
    private lateinit var titleRow: LinearLayout
    private lateinit var selectionBar: LinearLayout
    private lateinit var selectionCountView: TextView
    private lateinit var searchAndChips: LinearLayout

    private var allItems = mutableListOf<VideoItem>()
    private var mode = Mode.ALL
    private var query = ""
    private var sortMode = SortMode.NEWEST
    private var selectionMode = false
    private val selectedUris = mutableSetOf<String>()
    private var hasLoadedOnce = false

    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        val existing = allItems.map { it.uri }.toHashSet()
        var added = 0
        uris.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            if (uri.toString() !in existing) {
                allItems.add(0, repository.createItem(uri))
                existing.add(uri.toString())
                added++
            }
        }
        repository.save(allItems)
        refresh()
        if (added > 0) Toast.makeText(this, "${added}本追加しました", Toast.LENGTH_SHORT).show()
    }

    private val lockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadAndRefresh()
        } else if (!AppLock.unlocked) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Palette.Bg
        window.navigationBarColor = Palette.Bg

        repository = VideoRepository(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        if (repository.hasPin() && !AppLock.unlocked) {
            lockLauncher.launch(Intent(this, LockActivity::class.java).putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_VERIFY))
        } else {
            loadAndRefresh()
        }
    }

    private fun loadAndRefresh() {
        allItems = repository.load()
        sortMode = repository.getSortMode()
        recycler.layoutManager = GridLayoutManager(this, calculateColumns())
        hasLoadedOnce = true
        refresh()
    }

    private fun buildUi(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Palette.Bg)
        }
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingDp(18, 18, 18, 8)
        }

        titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "女の子保存動画"
            setTextColor(Palette.Text)
            textSize = 22f
            typeface = Typeface.create("sans", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        sortButton = iconButton("⇅") { showSortMenu(it) }
        val settingsButton = iconButton("⚙") {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        titleRow.addView(title)
        titleRow.addView(sortButton)
        titleRow.addView(settingsButton)

        selectionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        val cancelSel = iconButton("×") { exitSelection() }
        selectionCountView = TextView(this).apply {
            textSize = 16f
            setTextColor(Palette.Text)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPaddingDp(8, 0, 0, 0)
        }
        val favSel = iconButton("♥") { bulkFavorite() }
        val delSel = iconButton("🗑") { bulkRemove() }
        selectionBar.addView(cancelSel)
        selectionBar.addView(selectionCountView)
        selectionBar.addView(favSel)
        selectionBar.addView(delSel)

        counter = TextView(this).apply {
            setTextColor(Palette.Muted)
            textSize = 12.5f
            setPaddingDp(1, 2, 0, 10)
        }

        searchAndChips = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        search = EditText(this).apply {
            hint = "動画を検索"
            setHintTextColor(Color.rgb(115, 115, 126))
            setTextColor(Palette.Text)
            textSize = 15f
            setSingleLine(true)
            background = roundedBg(Palette.Surface, 17f, this@MainActivity, Palette.Stroke)
            setPaddingDp(16, 0, 16, 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    query = s?.toString()?.trim().orEmpty()
                    refresh()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPaddingDp(0, 10, 0, 3)
        }
        allChip = createChip("すべて") { mode = Mode.ALL; refresh() }
        favChip = createChip("お気に入り") { mode = Mode.FAVORITES; refresh() }
        recentChip = createChip("最近追加") { mode = Mode.RECENT; refresh() }
        chips.addView(allChip)
        chips.addView(favChip)
        chips.addView(recentChip)
        searchAndChips.addView(search)
        searchAndChips.addView(chips)

        header.addView(titleRow)
        header.addView(selectionBar)
        header.addView(counter)
        header.addView(searchAndChips)
        main.addView(header)

        val content = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        recycler = RecyclerView(this).apply {
            setPadding(dp(12), dp(4), dp(12), dp(96))
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutManager = GridLayoutManager(this@MainActivity, calculateColumns())
        }
        adapter = VideoAdapter(
            context = this,
            onOpen = { openVideo(it) },
            onFavorite = { item ->
                item.favorite = !item.favorite
                repository.save(allItems)
                refresh()
            },
            onLongPress = { item -> toggleSelection(item) },
            onMenu = { item, anchor -> showItemMenu(item, anchor) }
        )
        recycler.adapter = adapter

        empty = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            val icon = TextView(this@MainActivity).apply {
                text = "▣"
                textSize = 48f
                gravity = Gravity.CENTER
                setTextColor(Palette.Accent)
            }
            val t = TextView(this@MainActivity).apply {
                text = "まだ動画がありません"
                textSize = 17f
                setTextColor(Palette.Text)
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val s = TextView(this@MainActivity).apply {
                text = "右下の＋から端末内の動画を追加できます"
                textSize = 13f
                setTextColor(Palette.Muted)
                gravity = Gravity.CENTER
                setPaddingDp(20, 6, 20, 0)
            }
            addView(icon)
            addView(t)
            addView(s)
        }

        val add = TextView(this).apply {
            text = "+  動画を追加"
            textSize = 15f
            setTextColor(Palette.AccentText)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = rippleBg(Palette.Accent, 22f, this@MainActivity)
            elevation = dp(12).toFloat()
            setOnClickListener { picker.launch(arrayOf("video/*")) }
            layoutParams = FrameLayout.LayoutParams(dp(144), dp(52), Gravity.BOTTOM or Gravity.END).apply {
                setMargins(0, 0, dp(18), dp(22))
            }
        }

        content.addView(recycler)
        content.addView(empty)
        content.addView(add)
        main.addView(content)
        root.addView(main)
        return root
    }

    private fun iconButton(symbol: String, action: (View) -> Unit): TextView {
        return TextView(this).apply {
            text = symbol
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Palette.Text)
            background = rippleBg(Palette.Surface, 18f, this@MainActivity, Palette.Stroke)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginStart = dp(8) }
            setOnClickListener { action(this) }
        }
    }

    private fun createChip(textValue: String, action: () -> Unit): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Palette.Text)
            setPaddingDp(14, 0, 14, 0)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)).apply {
                rightMargin = dp(8)
            }
            setOnClickListener { action() }
        }
    }

    private fun refresh() {
        if (!hasLoadedOnce) return
        val selected = when (mode) {
            Mode.ALL -> allItems
            Mode.FAVORITES -> allItems.filter { it.favorite }
            Mode.RECENT -> allItems.sortedByDescending { it.addedAt }.take(30)
        }.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .sortedByMode(sortMode)

        adapter.submitList(selected)
        adapter.setSelection(selectionMode, selectedUris)
        empty.visibility = if (selected.isEmpty() && !selectionMode) View.VISIBLE else View.GONE
        recycler.visibility = if (selected.isEmpty()) View.GONE else View.VISIBLE
        counter.text = "${allItems.size}本の動画  •  端末内ローカル保存"
        styleChip(allChip, mode == Mode.ALL)
        styleChip(favChip, mode == Mode.FAVORITES)
        styleChip(recentChip, mode == Mode.RECENT)

        titleRow.visibility = if (selectionMode) View.GONE else View.VISIBLE
        selectionBar.visibility = if (selectionMode) View.VISIBLE else View.GONE
        searchAndChips.visibility = if (selectionMode) View.GONE else View.VISIBLE
        counter.visibility = if (selectionMode) View.GONE else View.VISIBLE
        selectionCountView.text = "${selectedUris.size}件選択中"
    }

    private fun styleChip(view: TextView, selected: Boolean) {
        view.background = if (selected) {
            roundedBg(Palette.Accent, 19f, this)
        } else {
            roundedBg(Palette.Surface, 19f, this, Palette.Stroke)
        }
        view.setTextColor(if (selected) Palette.AccentText else Palette.Text)
    }

    private fun openVideo(item: VideoItem) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_URI, item.uri)
            putExtra(PlayerActivity.EXTRA_TITLE, item.name)
        })
    }

    private fun toggleSelection(item: VideoItem) {
        if (!selectionMode) {
            selectionMode = true
            selectedUris.clear()
        }
        if (!selectedUris.add(item.uri)) {
            selectedUris.remove(item.uri)
        }
        if (selectedUris.isEmpty()) selectionMode = false
        refresh()
    }

    private fun exitSelection() {
        selectionMode = false
        selectedUris.clear()
        refresh()
    }

    private fun bulkFavorite() {
        val toFavorite = allItems.filter { it.uri in selectedUris }
        val makeFavorite = toFavorite.any { !it.favorite }
        toFavorite.forEach { it.favorite = makeFavorite }
        repository.save(allItems)
        exitSelection()
    }

    private fun bulkRemove() {
        val count = selectedUris.size
        AlertDialog.Builder(this)
            .setTitle("リストから削除")
            .setMessage("選択した${count}件をリストから削除します。端末内の動画ファイル自体は削除されません。")
            .setPositiveButton("削除する") { _, _ ->
                allItems.removeAll { it.uri in selectedUris }
                repository.save(allItems)
                exitSelection()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showSortMenu(anchor: View) {
        val menu = PopupMenu(this, anchor)
        val options = linkedMapOf(
            SortMode.NEWEST to "追加日（新しい順）",
            SortMode.OLDEST to "追加日（古い順）",
            SortMode.NAME to "名前順",
            SortMode.DURATION to "再生時間が長い順",
            SortMode.FAVORITE_FIRST to "お気に入り優先"
        )
        options.entries.forEachIndexed { index, (sm, label) ->
            val prefix = if (sm == sortMode) "✓ " else ""
            menu.menu.add(0, index, index, prefix + label)
        }
        menu.setOnMenuItemClickListener { item ->
            val selected = options.keys.toList()[item.itemId]
            sortMode = selected
            repository.setSortMode(selected)
            refresh()
            true
        }
        menu.show()
    }

    private fun showItemMenu(item: VideoItem, anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.menu.add(0, 0, 0, "名前を変更")
        menu.menu.add(0, 1, 1, if (item.favorite) "お気に入りを解除" else "お気に入りに追加")
        menu.menu.add(0, 2, 2, "リストから削除")
        menu.setOnMenuItemClickListener { m ->
            when (m.itemId) {
                0 -> showRenameDialog(item)
                1 -> { item.favorite = !item.favorite; repository.save(allItems); refresh() }
                2 -> confirmRemoveSingle(item)
            }
            true
        }
        menu.show()
    }

    private fun showRenameDialog(item: VideoItem) {
        val input = EditText(this).apply {
            setText(item.name)
            setSelection(text.length)
            setTextColor(Palette.Text)
            inputType = InputType.TYPE_CLASS_TEXT
            background = roundedBg(Palette.Surface2, 12f, this@MainActivity, Palette.Stroke)
            setPaddingDp(14, 10, 14, 10)
        }
        val container = FrameLayout(this).apply {
            setPaddingDp(20, 16, 20, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("名前を変更")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    item.name = newName
                    repository.save(allItems)
                    refresh()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun confirmRemoveSingle(item: VideoItem) {
        AlertDialog.Builder(this)
            .setTitle("リストから削除")
            .setMessage("「${item.name}」をリストから削除します。端末内の動画ファイル自体は削除されません。")
            .setPositiveButton("削除する") { _, _ ->
                allItems.removeAll { it.uri == item.uri }
                repository.save(allItems)
                refresh()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun calculateColumns(): Int {
        val override = repository.getGridSize()
        if (override != GridSize.AUTO) return override.columns
        val widthDp = resources.configuration.screenWidthDp
        return when {
            widthDp >= 900 -> 5
            widthDp >= 700 -> 4
            widthDp >= 520 -> 3
            else -> 2
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        (recycler.layoutManager as? GridLayoutManager)?.spanCount = calculateColumns()
    }

    private enum class Mode { ALL, FAVORITES, RECENT }
}
