package com.linnan.hayaophoto

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linnan.hayaophoto.crypto.AppLockState
import com.linnan.hayaophoto.ui.HayaoNavHost
import com.linnan.hayaophoto.ui.screens.LockScreen
import com.linnan.hayaophoto.ui.screens.SetupPasswordScreen
import com.linnan.hayaophoto.ui.theme.HayaoPhotoTheme
import com.linnan.hayaophoto.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applySecureFlag()

        setContent {
            HayaoPhotoTheme {
                Surface(modifier = Modifier) {
                    val authViewModel: AuthViewModel = viewModel()
                    var passwordSet by remember { mutableStateOf(authViewModel.isPasswordSet()) }
                    val isUnlocked by AppLockState.isUnlocked.collectAsState()

                    when {
                        !passwordSet -> SetupPasswordScreen(
                            authViewModel = authViewModel,
                            onSetupComplete = { passwordSet = true }
                        )
                        !isUnlocked -> LockScreen(authViewModel = authViewModel)
                        else -> HayaoNavHost()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applySecureFlag()
    }

    private fun applySecureFlag() {
        if (AppGraph.appSettings.screenshotBlocked) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
