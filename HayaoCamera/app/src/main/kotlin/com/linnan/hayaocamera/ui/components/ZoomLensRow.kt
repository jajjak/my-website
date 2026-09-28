package com.linnan.hayaocamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.linnan.hayaocamera.R
import com.linnan.hayaocamera.camera.LensInfo
import com.linnan.hayaocamera.ui.theme.HayaoBlack
import com.linnan.hayaocamera.ui.theme.HayaoGold
import com.linnan.hayaocamera.ui.theme.HayaoWhite

@Composable
fun LensSelectorRow(
    lenses: List<LensInfo>,
    selectedLensId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lenses.size < 2) return
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(4.dp)
    ) {
        lenses.forEach { lens ->
            val selected = lens.cameraId == selectedLensId
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) HayaoGold else Color.Transparent)
                    .clickable { onSelect(lens.cameraId) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatZoomLabel(lens.approxZoomRatioVsPrimary),
                    color = if (selected) HayaoBlack else HayaoWhite,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun formatZoomLabel(ratio: Float): String = "%.1f".format(ratio)

@Composable
fun ZoomBadge(zoomRatio: Float, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.camera_zoom_format, zoomRatio),
        color = HayaoWhite,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
