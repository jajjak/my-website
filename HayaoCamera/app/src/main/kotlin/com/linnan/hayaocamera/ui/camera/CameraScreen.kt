package com.linnan.hayaocamera.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.linnan.hayaocamera.R
import com.linnan.hayaocamera.camera.CaptureMode
import com.linnan.hayaocamera.ui.components.CameraBottomBar
import com.linnan.hayaocamera.ui.components.CameraPreview
import com.linnan.hayaocamera.ui.components.CameraTopBar
import com.linnan.hayaocamera.ui.components.ExposureSlider
import com.linnan.hayaocamera.ui.components.GridOverlay
import com.linnan.hayaocamera.ui.components.LensSelectorRow
import com.linnan.hayaocamera.ui.components.ModeSwitch
import com.linnan.hayaocamera.ui.components.RecordingIndicator
import com.linnan.hayaocamera.ui.components.ShutterButton
import com.linnan.hayaocamera.ui.components.TimerCountdownOverlay
import com.linnan.hayaocamera.ui.components.ZoomBadge
import com.linnan.hayaocamera.camera.TimerOption
import com.linnan.hayaocamera.camera.ResolutionMode
import com.linnan.hayaocamera.ui.theme.HayaoBlack
import com.linnan.hayaocamera.ui.theme.HayaoWhite
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    val captureMode by viewModel.captureMode.collectAsState()
    val timerCountdown by viewModel.timerCountdown.collectAsState()
    val latestThumbnail by viewModel.latestThumbnail.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val context = LocalContext.current
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setMicPermissionGranted(granted) }

    LaunchedEffect(captureMode) {
        if (captureMode == CaptureMode.VIDEO) {
            val alreadyGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (alreadyGranted) {
                viewModel.setMicPermissionGranted(true)
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(2400)
            viewModel.consumeStatusMessage()
        }
    }

    val lensList = if (engineState.facingBack) engineState.backLenses else engineState.frontLenses

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HayaoBlack)
    ) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onSurfaceReady = { lifecycleOwner, previewView ->
                viewModel.attachPreview(lifecycleOwner, previewView)
            },
            onTap = { previewView, x, y ->
                val point = previewView.meteringPointFactory.createPoint(x, y)
                viewModel.tapToFocus(point)
            },
            onPinchZoom = { factor ->
                val current = viewModel.engineState.value.zoomRatio
                viewModel.setZoomRatio(current * factor)
            }
        )

        if (settings.gridEnabled) {
            GridOverlay(modifier = Modifier.fillMaxSize())
        }

        TimerCountdownOverlay(seconds = timerCountdown, modifier = Modifier.fillMaxSize())

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraTopBar(
                flashMode = settings.flashMode,
                hasFlash = engineState.hasFlashUnit,
                timerOption = settings.timerOption,
                resolutionMode = settings.resolutionMode,
                showQualityToggle = captureMode == CaptureMode.PHOTO,
                onFlashClick = { viewModel.cycleFlash() },
                onTimerClick = {
                    val next = when (settings.timerOption) {
                        TimerOption.OFF -> TimerOption.SEC_3
                        TimerOption.SEC_3 -> TimerOption.SEC_5
                        TimerOption.SEC_5 -> TimerOption.SEC_10
                        TimerOption.SEC_10 -> TimerOption.OFF
                    }
                    viewModel.setTimerOption(next)
                },
                onQualityClick = {
                    val next = if (settings.resolutionMode == ResolutionMode.MAXIMUM) {
                        ResolutionMode.STANDARD
                    } else {
                        ResolutionMode.MAXIMUM
                    }
                    viewModel.setResolutionMode(next)
                }
            )
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                tint = HayaoWhite,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onOpenSettings() }
            )
        }

        if (engineState.isRecording) {
            RecordingIndicator(
                elapsedLabel = formatElapsed(engineState.recordingElapsedMs),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 60.dp)
            )
        }

        // Bottom cluster
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (captureMode == CaptureMode.PHOTO && engineState.exposureRange.first < engineState.exposureRange.last) {
                ExposureSlider(
                    range = engineState.exposureRange,
                    currentIndex = engineState.exposureIndex,
                    stepEv = engineState.exposureStepEv,
                    onValueChange = { viewModel.setExposureIndex(it) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                ZoomBadge(zoomRatio = engineState.zoomRatio)
                if (engineState.facingBack) {
                    LensSelectorRow(
                        lenses = lensList,
                        selectedLensId = engineState.selectedLensId,
                        onSelect = { viewModel.selectBackLens(it) }
                    )
                }
            }

            ModeSwitch(
                mode = captureMode,
                onModeChange = { viewModel.setCaptureMode(it) },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CameraBottomBar(
                shutter = {
                    ShutterButton(
                        isVideoMode = captureMode == CaptureMode.VIDEO,
                        isRecording = engineState.isRecording,
                        hapticsEnabled = settings.hapticsEnabled,
                        onClick = { viewModel.onShutterPressed() }
                    )
                },
                latestThumbnail = latestThumbnail,
                onThumbnailClick = onOpenGallery,
                onSwitchCameraClick = { viewModel.switchCamera() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            )
        }

        AnimatedVisibility(
            visible = statusMessage != null,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = statusMessage ?: "",
                    color = HayaoWhite,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}
