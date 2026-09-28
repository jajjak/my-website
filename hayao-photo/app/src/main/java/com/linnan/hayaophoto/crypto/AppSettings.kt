package com.linnan.hayaophoto.crypto

import android.content.Context
import android.content.SharedPreferences

enum class AutoLockOption(val minutes: Int, val label: String) {
    IMMEDIATE(0, "すぐに"),
    ONE_MIN(1, "1分後"),
    FIVE_MIN(5, "5分後"),
    FIFTEEN_MIN(15, "15分後"),
    NEVER(-1, "しない");

    companion object {
        fun fromMinutes(minutes: Int): AutoLockOption =
            entries.firstOrNull { it.minutes == minutes } ?: IMMEDIATE
    }
}

enum class SortOrder { NEWEST_FIRST, OLDEST_FIRST }

/** Plain (non-secret) app preferences: none of these values reveal password or media content. */
class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hayao_photo_settings", Context.MODE_PRIVATE)

    var autoLockMinutes: Int
        get() = prefs.getInt(KEY_AUTO_LOCK, AutoLockOption.IMMEDIATE.minutes)
        set(value) = prefs.edit().putInt(KEY_AUTO_LOCK, value).apply()

    var screenshotBlocked: Boolean
        get() = prefs.getBoolean(KEY_SCREENSHOT_BLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSHOT_BLOCK, value).apply()

    var lastBackgroundedAt: Long
        get() = prefs.getLong(KEY_LAST_BG, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BG, value).apply()

    companion object {
        private const val KEY_AUTO_LOCK = "auto_lock_minutes"
        private const val KEY_SCREENSHOT_BLOCK = "screenshot_blocked"
        private const val KEY_LAST_BG = "last_backgrounded_at"
    }
}
