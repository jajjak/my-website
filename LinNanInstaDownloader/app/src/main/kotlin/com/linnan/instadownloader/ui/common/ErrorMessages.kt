package com.linnan.instadownloader.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.linnan.instadownloader.R
import com.linnan.instadownloader.data.model.FailureReason

@Composable
fun messageFor(reason: FailureReason): String = when (reason) {
    FailureReason.INVALID_URL -> stringResource(R.string.error_invalid_url)
    FailureReason.NOT_FOUND -> stringResource(R.string.error_not_found)
    FailureReason.PRIVATE_OR_LOGIN_REQUIRED -> stringResource(R.string.error_private_content)
    FailureReason.MEDIA_UNAVAILABLE -> stringResource(R.string.error_media_unavailable)
    FailureReason.RATE_LIMITED -> stringResource(R.string.error_rate_limited)
    FailureReason.NETWORK_ERROR -> stringResource(R.string.error_network)
    FailureReason.UNKNOWN -> stringResource(R.string.error_unknown)
}
