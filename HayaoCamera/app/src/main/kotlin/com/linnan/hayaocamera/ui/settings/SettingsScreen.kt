package com.linnan.hayaocamera.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.linnan.hayaocamera.R
import com.linnan.hayaocamera.camera.FrameRateChoice
import com.linnan.hayaocamera.camera.VideoQualityChoice
import com.linnan.hayaocamera.camera.ResolutionMode
import com.linnan.hayaocamera.ui.camera.CameraViewModel
import com.linnan.hayaocamera.ui.theme.HayaoBlack
import com.linnan.hayaocamera.ui.theme.HayaoDivider
import com.linnan.hayaocamera.ui.theme.HayaoGold
import com.linnan.hayaocamera.ui.theme.HayaoWhite
import com.linnan.hayaocamera.ui.theme.HayaoWhiteMuted

@Composable
fun SettingsScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val engineState by viewModel.engineState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HayaoBlack)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = HayaoWhite,
                modifier = Modifier.clickable(onClick = onBack)
            )
            Text(
                text = stringResource(R.string.settings_title),
                color = HayaoWhite,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        SectionHeader(stringResource(R.string.settings_section_photo))
        ChoiceRow(
            title = stringResource(R.string.settings_resolution_mode),
            value = if (settings.resolutionMode == ResolutionMode.MAXIMUM) {
                stringResource(R.string.quality_max)
            } else {
                stringResource(R.string.quality_standard)
            },
            onClick = {
                val next = if (settings.resolutionMode == ResolutionMode.MAXIMUM) ResolutionMode.STANDARD else ResolutionMode.MAXIMUM
                viewModel.setResolutionMode(next)
            }
        )
        engineState.capabilities?.let { caps ->
            InfoRow(
                title = stringResource(R.string.capability_sensor_label),
                value = "%.1f MP".format(caps.sensorNativeMegapixels)
            )
            val activeSize = engineState.actualPhotoResolution
            if (activeSize != null) {
                InfoRow(
                    title = stringResource(R.string.capability_current_label),
                    value = stringResource(
                        R.string.quality_label_format,
                        activeSize.width,
                        activeSize.height,
                        activeSize.megapixels
                    )
                )
            }
        }
        SwitchRow(
            title = stringResource(R.string.settings_raw_capture),
            subtitle = if (engineState.rawAvailable) {
                stringResource(R.string.settings_raw_capture_desc)
            } else {
                stringResource(R.string.capture_raw_unavailable)
            },
            checked = settings.rawCaptureEnabled,
            enabled = engineState.rawAvailable,
            onCheckedChange = { toggleRaw(viewModel, it) }
        )
        SwitchRow(
            title = stringResource(R.string.settings_hdr),
            subtitle = when {
                !engineState.hdrExtensionAvailable -> stringResource(R.string.settings_hdr_unsupported)
                engineState.hdrConflictWithMaxResolution -> stringResource(R.string.settings_hdr_conflict)
                else -> stringResource(R.string.settings_hdr_desc)
            },
            checked = settings.hdrEnabled,
            enabled = engineState.hdrExtensionAvailable,
            onCheckedChange = { toggleHdr(viewModel, it) }
        )
        SwitchRow(
            title = stringResource(R.string.settings_grid),
            subtitle = stringResource(R.string.settings_grid_desc),
            checked = settings.gridEnabled,
            enabled = true,
            onCheckedChange = { toggleGrid(viewModel, it) }
        )

        SectionHeader(stringResource(R.string.settings_section_video))
        ChoiceRow(
            title = stringResource(R.string.settings_video_quality),
            value = when (settings.videoQuality) {
                VideoQualityChoice.UHD_4K -> stringResource(R.string.video_quality_4k)
                VideoQualityChoice.FHD_1080P -> stringResource(R.string.video_quality_1080p)
                VideoQualityChoice.HD_720P -> stringResource(R.string.video_quality_720p)
            },
            onClick = { cycleVideoQuality(viewModel, settings.videoQuality) }
        )
        if (settings.videoQuality == VideoQualityChoice.UHD_4K && engineState.videoCapability?.supports4k == false) {
            InfoRow(title = stringResource(R.string.video_not_supported_4k), value = "")
        }
        ChoiceRow(
            title = stringResource(R.string.settings_video_fps),
            value = when (settings.videoFrameRate) {
                FrameRateChoice.FPS_30 -> "30 fps"
                FrameRateChoice.FPS_60 -> "60 fps"
            },
            onClick = { cycleFrameRate(viewModel, settings.videoFrameRate) }
        )
        SwitchRow(
            title = stringResource(R.string.settings_video_stabilization),
            subtitle = stringResource(R.string.settings_video_stabilization_desc),
            checked = settings.videoStabilizationEnabled,
            enabled = true,
            onCheckedChange = { toggleStabilization(viewModel, it) }
        )

        SectionHeader(stringResource(R.string.settings_section_general))
        SwitchRow(
            title = stringResource(R.string.settings_haptics),
            subtitle = "",
            checked = settings.hapticsEnabled,
            enabled = true,
            onCheckedChange = { toggleHaptics(viewModel, it) }
        )
        InfoRow(
            title = stringResource(R.string.settings_save_location),
            value = stringResource(R.string.settings_save_location_desc)
        )

        SectionHeader(stringResource(R.string.settings_section_about))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAbout)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.about_app_name), color = HayaoWhite, style = MaterialTheme.typography.bodyLarge)
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = HayaoWhiteMuted)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = HayaoGold,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(text = title, color = HayaoWhite, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, color = HayaoWhiteMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HayaoGold,
                checkedTrackColor = HayaoGold.copy(alpha = 0.4f)
            )
        )
    }
    HorizontalDivider(color = HayaoDivider)
}

@Composable
private fun ChoiceRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = HayaoWhite, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, color = HayaoGold, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(color = HayaoDivider)
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = HayaoWhiteMuted, style = MaterialTheme.typography.bodySmall)
        if (value.isNotEmpty()) {
            Text(text = value, color = HayaoWhiteMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun toggleRaw(viewModel: CameraViewModel, enabled: Boolean) {
    viewModel.setRawEnabled(enabled)
}

private fun toggleHdr(viewModel: CameraViewModel, enabled: Boolean) {
    viewModel.setHdrEnabled(enabled)
}

private fun toggleGrid(viewModel: CameraViewModel, enabled: Boolean) {
    viewModel.setGridEnabled(enabled)
}

private fun toggleStabilization(viewModel: CameraViewModel, enabled: Boolean) {
    viewModel.setVideoStabilizationEnabled(enabled)
}

private fun toggleHaptics(viewModel: CameraViewModel, enabled: Boolean) {
    viewModel.setHapticsEnabled(enabled)
}

private fun cycleVideoQuality(viewModel: CameraViewModel, current: VideoQualityChoice) {
    val next = when (current) {
        VideoQualityChoice.HD_720P -> VideoQualityChoice.FHD_1080P
        VideoQualityChoice.FHD_1080P -> VideoQualityChoice.UHD_4K
        VideoQualityChoice.UHD_4K -> VideoQualityChoice.HD_720P
    }
    viewModel.setVideoQuality(next)
}

private fun cycleFrameRate(viewModel: CameraViewModel, current: FrameRateChoice) {
    val next = if (current == FrameRateChoice.FPS_30) FrameRateChoice.FPS_60 else FrameRateChoice.FPS_30
    viewModel.setVideoFrameRate(next)
}
