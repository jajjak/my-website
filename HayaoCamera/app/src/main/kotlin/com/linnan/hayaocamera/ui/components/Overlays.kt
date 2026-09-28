package com.linnan.hayaocamera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linnan.hayaocamera.ui.theme.HayaoGold
import com.linnan.hayaocamera.ui.theme.HayaoScrim
import com.linnan.hayaocamera.ui.theme.HayaoWhite

@Composable
fun GridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val thirdW = size.width / 3f
        val thirdH = size.height / 3f
        val lineColor = Color.White.copy(alpha = 0.35f)
        for (i in 1..2) {
            drawLine(lineColor, Offset(thirdW * i, 0f), Offset(thirdW * i, size.height), strokeWidth = 1.dp.toPx())
            drawLine(lineColor, Offset(0f, thirdH * i), Offset(size.width, thirdH * i), strokeWidth = 1.dp.toPx())
        }
    }
}

@Composable
fun TimerCountdownOverlay(seconds: Int?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = seconds != null,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(HayaoScrim),
            contentAlignment = Alignment.Center
        ) {
            val scale by animateFloatAsState(
                targetValue = if (seconds != null) 1f else 0.6f,
                animationSpec = tween(220),
                label = "timerScale"
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = (seconds ?: 0).toString(),
                    style = TextStyle(
                        color = HayaoGold,
                        fontSize = (56 * scale).sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
fun RecordingIndicator(elapsedLabel: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(com.linnan.hayaocamera.ui.theme.HayaoRecording)
            )
            Spacer(Modifier.size(6.dp))
            BasicText(
                text = elapsedLabel,
                style = TextStyle(color = HayaoWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
        }
    }
}
