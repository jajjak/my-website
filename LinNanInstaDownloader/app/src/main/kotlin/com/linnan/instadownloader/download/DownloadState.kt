package com.linnan.instadownloader.download

import android.net.Uri
import com.linnan.instadownloader.data.model.FailureReason
import com.linnan.instadownloader.data.model.MediaType

sealed class DownloadState {
    data class Progress(val downloadedBytes: Long, val totalBytes: Long?) : DownloadState()
    data class Completed(
        val uri: Uri,
        val displayName: String,
        val mediaType: MediaType,
        val mimeType: String,
        val sizeBytes: Long
    ) : DownloadState()
    data class Failed(val reason: FailureReason) : DownloadState()
}
