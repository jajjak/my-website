package com.linnan.instadownloader.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linnan.instadownloader.ui.history.HistoryScreen
import com.linnan.instadownloader.ui.home.HomeScreen
import com.linnan.instadownloader.ui.result.ResultScreen

@Composable
fun LinNanApp(viewModel: AppViewModel) {
    val screen by viewModel.screen.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen-transition"
        ) { target ->
            when (target) {
                Screen.HOME -> HomeScreen(viewModel)
                Screen.RESULT -> ResultScreen(viewModel)
                Screen.HISTORY -> HistoryScreen(viewModel)
            }
        }
    }
}
