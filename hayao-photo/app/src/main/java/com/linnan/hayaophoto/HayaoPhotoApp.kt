package com.linnan.hayaophoto

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.linnan.hayaophoto.crypto.AutoLockObserver

class HayaoPhotoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AutoLockObserver(AppGraph.appSettings))
        // A previous run's decrypted video may still sit in cache if the process was killed
        // mid-playback; clear it so no plaintext lingers longer than a single session.
        AppGraph.mediaCrypto.clearPlaybackCache()
    }
}
