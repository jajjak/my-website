package com.linnan.instadownloader.ui.result

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.linnan.instadownloader.R
import com.linnan.instadownloader.ui.SavedInfo

@Composable
fun SavedDialog(savedInfo: SavedInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(stringResource(R.string.download_done_title)) },
        text = { Text(savedInfo.mimeType) },
        confirmButton = {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(savedInfo.uri, savedInfo.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, null))
            }) {
                Text(stringResource(R.string.action_open))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = savedInfo.mimeType
                    putExtra(Intent.EXTRA_STREAM, savedInfo.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, null))
            }) {
                Text(stringResource(R.string.action_share))
            }
        }
    )
}
