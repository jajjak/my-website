package com.linnan.hayaocamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.linnan.hayaocamera.R
import com.linnan.hayaocamera.ui.theme.HayaoGold
import com.linnan.hayaocamera.ui.theme.HayaoWhite
import kotlin.math.roundToInt

@Composable
fun ExposureSlider(
    range: IntRange,
    currentIndex: Int,
    stepEv: Float,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (range.first >= range.last) return
    val exposureDescription = stringResource(R.string.cd_exposure_slider)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .semantics { contentDescription = exposureDescription },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val evValue = currentIndex * stepEv
        Text(
            text = "${if (evValue >= 0) "+" else ""}${"%.1f".format(evValue)}",
            color = HayaoGold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(44.dp)
        )
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = HayaoGold,
                activeTrackColor = HayaoGold,
                inactiveTrackColor = HayaoWhite.copy(alpha = 0.25f)
            ),
            modifier = Modifier.width(160.dp)
        )
    }
}
