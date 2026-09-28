package com.linnan.hayaocamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.linnan.hayaocamera.R
import com.linnan.hayaocamera.camera.FlashMode
import com.linnan.hayaocamera.camera.TimerOption
import com.linnan.hayaocamera.camera.ResolutionMode
import com.linnan.hayaocamera.ui.theme.HayaoGold
import com.linnan.hayaocamera.ui.theme.HayaoWhite

@Composable
fun CameraTopBar(
    flashMode: FlashMode,
    hasFlash: Boolean,
    timerOption: TimerOption,
    resolutionMode: ResolutionMode,
    showQualityToggle: Boolean,
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onQualityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasFlash) {
            TopBarIcon(
                icon = when (flashMode) {
                    FlashMode.OFF -> Icons.Filled.FlashOff
                    FlashMode.ON -> Icons.Filled.FlashOn
                    FlashMode.AUTO -> Icons.Filled.FlashAuto
                },
                tinted = flashMode != FlashMode.OFF,
                contentDescription = stringResource(R.string.cd_flash_toggle),
                onClick = onFlashClick
            )
        }
        TopBarIcon(
            icon = when (timerOption) {
                TimerOption.OFF -> Icons.Filled.Timer
                TimerOption.SEC_3 -> Icons.Filled.Timer3
                TimerOption.SEC_5 -> Icons.Filled.Timer
                TimerOption.SEC_10 -> Icons.Filled.Timer10
            },
            tinted = timerOption != TimerOption.OFF,
            contentDescription = stringResource(R.string.cd_timer_toggle),
            onClick = onTimerClick
        )
        if (showQualityToggle) {
            QualityPill(resolutionMode = resolutionMode, onClick = onQualityClick)
        }
    }
}

@Composable
private fun RowScope.TopBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tinted: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (tinted) HayaoGold else HayaoWhite,
        modifier = Modifier
            .padding(end = 18.dp)
            .size(24.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun QualityPill(resolutionMode: ResolutionMode, onClick: () -> Unit) {
    val label = if (resolutionMode == ResolutionMode.MAXIMUM) {
        stringResource(R.string.quality_max)
    } else {
        stringResource(R.string.quality_standard)
    }
    Text(
        text = label,
        color = if (resolutionMode == ResolutionMode.MAXIMUM) HayaoGold else HayaoWhite,
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(CircleShape)
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
