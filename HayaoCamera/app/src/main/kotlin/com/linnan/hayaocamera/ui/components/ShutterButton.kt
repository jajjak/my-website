package com.linnan.hayaocamera.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.linnan.hayaocamera.R
import com.linnan.hayaocamera.ui.theme.HayaoRecording
import com.linnan.hayaocamera.ui.theme.HayaoWhite

@Composable
fun ShutterButton(
    isVideoMode: Boolean,
    isRecording: Boolean,
    hapticsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shutterDescription = stringResource(R.string.cd_shutter_button)

    val innerSize by animateDpAsState(
        targetValue = when {
            isVideoMode && isRecording -> 30.dp
            pressed -> 58.dp
            else -> 64.dp
        },
        animationSpec = tween(150),
        label = "shutterInner"
    )
    val innerCorner by animateDpAsState(
        targetValue = if (isVideoMode && isRecording) 8.dp else 32.dp,
        animationSpec = tween(150),
        label = "shutterInnerCorner"
    )

    Box(
        modifier = modifier
            .size(78.dp)
            .clip(CircleShape)
            .border(width = 3.dp, color = HayaoWhite, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 44.dp),
                onClick = {
                    if (hapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onClick()
                }
            )
            .semantics { contentDescription = "" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(RoundedCornerShape(innerCorner))
                .background(if (isVideoMode) HayaoRecording else HayaoWhite)
                .semantics { contentDescription = shutterDescription }
        )
    }
}
