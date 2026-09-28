package com.linnan.hayaophoto.viewmodel

import androidx.lifecycle.ViewModel
import com.linnan.hayaophoto.AppGraph
import com.linnan.hayaophoto.crypto.AppLockState

class AuthViewModel : ViewModel() {
    private val auth = AppGraph.authManager

    fun isPasswordSet(): Boolean = auth.isPasswordSet()

    fun setupPassword(password: String) {
        auth.setPassword(password)
        AppLockState.unlock()
    }

    fun verify(password: String): Boolean {
        val ok = auth.verifyPassword(password)
        if (ok) AppLockState.unlock()
        return ok
    }

    fun changePassword(oldPassword: String, newPassword: String): Boolean =
        auth.changePassword(oldPassword, newPassword)
}
