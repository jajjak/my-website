package com.linnan.hayaophoto

import android.content.Context
import com.linnan.hayaophoto.crypto.AppSettings
import com.linnan.hayaophoto.crypto.AuthManager
import com.linnan.hayaophoto.crypto.MediaCrypto
import com.linnan.hayaophoto.data.AppDatabase
import com.linnan.hayaophoto.data.MediaRepository

/** Minimal hand-rolled dependency graph (no DI framework needed for an app this size). */
object AppGraph {
    lateinit var db: AppDatabase
        private set
    lateinit var mediaCrypto: MediaCrypto
        private set
    lateinit var authManager: AuthManager
        private set
    lateinit var appSettings: AppSettings
        private set
    lateinit var repository: MediaRepository
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        db = AppDatabase.getInstance(app)
        mediaCrypto = MediaCrypto(app)
        authManager = AuthManager(app)
        appSettings = AppSettings(app)
        repository = MediaRepository(app, db.personDao(), db.mediaDao(), mediaCrypto)
    }
}
