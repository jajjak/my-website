package com.linnan.girlvideos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.util.concurrent.Executors
import kotlin.math.max

class VideoAdapter(
    private val context: Context,
    private val onOpen: (VideoItem) -> Unit,
    private val onFavorite: (VideoItem) -> Unit,
    private val onLongPress: (VideoItem) -> Unit,
    private val onMenu: (VideoItem, View) -> Unit
) : RecyclerView.Adapter<VideoAdapter.Holder>() {

    private val executor = Executors.newFixedThreadPool(3)
    private val thumbCache = object : LinkedHashMap<String, Bitmap>(40, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 40
    }
    private var items: List<VideoItem> = emptyList()
    private var selectionMode: Boolean = false
    private var selectedUris: Set<String> = emptySet()

    fun submitList(newItems: List<VideoItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelection(active: Boolean, selected: Set<String>) {
        selectionMode = active
        selectedUris = selected
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val card = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            strokeWidth = context.dp(1)
            strokeColor = Palette.Stroke
            setCardBackgroundColor(Palette.Surface)
            useCompatPadding = false
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val m = context.dp(6)
                setMargins(m, m, m, m)
            }
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val media = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(156))
        }
        val image = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(27, 27, 31))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val shade = View(context).apply {
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(Color.argb(185, 0, 0, 0), Color.TRANSPARENT))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(72), Gravity.BOTTOM)
        }
        val duration = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            background = roundedBg(Color.argb(185, 18, 18, 20), 9f, context)
            setPaddingDp(7, 4, 7, 4)
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply {
                setMargins(0, 0, context.dp(10), context.dp(10))
            }
        }
        val play = TextView(context).apply {
            text = "▶"
            textSize = 17f
            setTextColor(Palette.AccentText)
            gravity = Gravity.CENTER
            background = roundedBg(Palette.Accent, 24f, context)
            layoutParams = FrameLayout.LayoutParams(context.dp(46), context.dp(46), Gravity.CENTER)
        }
        val checkBadge = TextView(context).apply {
            text = "✓"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedBg(Palette.Accent, 13f, context)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(context.dp(26), context.dp(26), Gravity.TOP or Gravity.START).apply {
                setMargins(context.dp(8), context.dp(8), 0, 0)
            }
        }
        val dim = View(context).apply {
            setBackgroundColor(Color.argb(0, 0, 0, 0))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        media.addView(image)
        media.addView(shade)
        media.addView(play)
        media.addView(dim)
        media.addView(duration)
        media.addView(checkBadge)

        val meta = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(62))
            setPaddingDp(13, 8, 4, 8)
        }
        val title = TextView(context).apply {
            setTextColor(Palette.Text)
            textSize = 14f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                rightMargin = context.dp(84)
            }
        }
        val menuButton = TextView(context).apply {
            text = "⋮"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Palette.Muted)
            isClickable = true
            isFocusable = true
            applyRippleForeground(context)
            layoutParams = FrameLayout.LayoutParams(context.dp(38), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END).apply {
                rightMargin = context.dp(42)
            }
        }
        val heart = TextView(context).apply {
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Palette.Accent)
            isClickable = true
            isFocusable = true
            applyRippleForeground(context)
            layoutParams = FrameLayout.LayoutParams(context.dp(42), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END)
        }
        meta.addView(title)
        meta.addView(menuButton)
        meta.addView(heart)

        column.addView(media)
        column.addView(meta)
        card.addView(column)
        return Holder(card, image, title, duration, heart, menuButton, checkBadge, dim)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.duration.text = formatDuration(item.durationMs)
        holder.heart.text = if (item.favorite) "♥" else "♡"
        holder.menuButton.visibility = if (selectionMode) View.GONE else View.VISIBLE
        holder.heart.visibility = if (selectionMode) View.GONE else View.VISIBLE

        val selected = selectedUris.contains(item.uri)
        holder.checkBadge.visibility = if (selectionMode && selected) View.VISIBLE else if (selectionMode) View.INVISIBLE else View.GONE
        holder.dim.setBackgroundColor(if (selectionMode && selected) Color.argb(90, 243, 182, 201) else Color.argb(0, 0, 0, 0))

        holder.itemView.setOnClickListener {
            if (selectionMode) onLongPress(item) else onOpen(item)
        }
        holder.itemView.setOnLongClickListener {
            onLongPress(item)
            true
        }
        holder.heart.setOnClickListener { onFavorite(item) }
        holder.menuButton.setOnClickListener { onMenu(item, holder.menuButton) }
        holder.image.setImageDrawable(null)
        holder.image.tag = item.uri

        val cached = synchronized(thumbCache) { thumbCache[item.uri] }
        if (cached != null) {
            holder.image.setImageBitmap(cached)
        } else {
            executor.execute {
                val bmp = loadThumbnail(item.uri)
                if (bmp != null) {
                    synchronized(thumbCache) { thumbCache[item.uri] = bmp }
                    holder.image.post {
                        if (holder.image.tag == item.uri) holder.image.setImageBitmap(bmp)
                    }
                }
            }
        }
    }

    private fun loadThumbnail(rawUri: String): Bitmap? {
        val uri = Uri.parse(rawUri)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(720, 405), null)
            } else {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, uri)
                val frame = mmr.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                mmr.release()
                frame
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDuration(ms: Long): String {
        val total = max(0L, ms / 1000)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    class Holder(
        itemView: View,
        val image: ImageView,
        val title: TextView,
        val duration: TextView,
        val heart: TextView,
        val menuButton: TextView,
        val checkBadge: TextView,
        val dim: View
    ) : RecyclerView.ViewHolder(itemView)
}
