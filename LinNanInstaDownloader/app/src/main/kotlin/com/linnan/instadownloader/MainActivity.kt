package com.linnan.instadownloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.linnan.instadownloader.ui.AppViewModel
import com.linnan.instadownloader.ui.LinNanApp
import com.linnan.instadownloader.ui.theme.LinNanInstaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        consumeSendIntent(intent)

        setContent {
            LinNanInstaTheme {
                LinNanApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeSendIntent(intent)
    }

    private fun consumeSendIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.onSharedUrlReceived(sharedText.trim())
            }
        }
    }
}
