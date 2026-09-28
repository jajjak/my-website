package com.linnan.hayaophoto.crypto

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Single source of truth for whether the vault is currently unlocked. Starts locked on every
 * fresh process, and flips back to locked by [AutoLockObserver] once the configured idle
 * timeout elapses while the app was backgrounded. */
object AppLockState {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
    }
}

class AutoLockObserver(private val settings: AppSettings) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) {
        if (AppLockState.isUnlocked.value) {
            settings.lastBackgroundedAt = System.currentTimeMillis()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val minutes = settings.autoLockMinutes
        if (minutes < 0) return // "しない"
        val last = settings.lastBackgroundedAt
        if (last == 0L) return
        val elapsed = System.currentTimeMillis() - last
        if (elapsed >= minutes * 60_000L) {
            AppLockState.lock()
        }
    }
}
