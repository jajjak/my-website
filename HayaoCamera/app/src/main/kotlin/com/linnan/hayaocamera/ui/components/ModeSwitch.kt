package com.linnan.hayaocamera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linnan.hayaocamera.R
import com.linnan.hayaocamera.camera.CaptureMode
import com.linnan.hayaocamera.ui.theme.HayaoGold
import com.linnan.hayaocamera.ui.theme.HayaoWhiteMuted

@Composable
fun ModeSwitch(
    mode: CaptureMode,
    onModeChange: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeLabel(
            text = stringResource(R.string.mode_photo),
            selected = mode == CaptureMode.PHOTO,
            onClick = { onModeChange(CaptureMode.PHOTO) }
        )
        Spacer(modifier = Modifier.width(18.dp))
        ModeLabel(
            text = stringResource(R.string.mode_video),
            selected = mode == CaptureMode.VIDEO,
            onClick = { onModeChange(CaptureMode.VIDEO) }
        )
    }
}

@Composable
private fun ModeLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(
        targetValue = if (selected) HayaoGold else HayaoWhiteMuted,
        animationSpec = tween(200),
        label = "modeLabelColor"
    )
    Text(
        text = text,
        color = color,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
