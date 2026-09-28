package com.linnan.hayaocamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.linnan.hayaocamera.R
import com.linnan.hayaocamera.data.GalleryItem
import com.linnan.hayaocamera.ui.theme.HayaoCharcoalLight
import com.linnan.hayaocamera.ui.theme.HayaoWhite

@Composable
fun CameraBottomBar(
    shutter: @Composable () -> Unit,
    latestThumbnail: GalleryItem?,
    onThumbnailClick: () -> Unit,
    onSwitchCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThumbnailButton(latestThumbnail, onThumbnailClick)
        shutter()
        SwitchCameraButton(onSwitchCameraClick)
    }
}

@Composable
private fun ThumbnailButton(item: GalleryItem?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HayaoCharcoalLight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (item != null) {
            if (item.isVideo) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_thumbnail),
                    tint = HayaoWhite,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                RemoteThumbnail(
                    uri = item.uri,
                    contentDescription = stringResource(R.string.cd_thumbnail),
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
                    maxDimension = 120
                )
            }
        }
    }
}

@Composable
private fun SwitchCameraButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Cameraswitch,
            contentDescription = stringResource(R.string.cd_switch_camera),
            tint = HayaoWhite,
            modifier = Modifier.size(26.dp)
        )
    }
}
