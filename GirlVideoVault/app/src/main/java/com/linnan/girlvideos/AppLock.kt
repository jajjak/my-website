package com.linnan.girlvideos

/** In-memory unlock flag. Reset whenever the process is recreated, so the PIN is asked once per session. */
object AppLock {
    var unlocked: Boolean = false
}
