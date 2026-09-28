package com.linnan.instadownloader.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "不明"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
}

fun formatResolution(width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return "不明"
    return "$width × $height"
}

fun formatSavedDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.JAPAN)
    return formatter.format(Date(epochMillis))
}
