package com.linnan.hayaophoto.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.linnan.hayaophoto.AppGraph
import com.linnan.hayaophoto.R
import com.linnan.hayaophoto.crypto.AutoLockOption
import com.linnan.hayaophoto.ui.Routes

@Composable
fun SettingsTabContent(navController: NavHostController) {
    val settings = AppGraph.appSettings
    var autoLock by remember { mutableStateOf(settings.autoLockMinutes) }
    var screenshotBlocked by remember { mutableStateOf(settings.screenshotBlocked) }

    Column(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        Text(
            "セキュリティ",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        ListItem(
            headlineContent = { Text("パスワードを変更") },
            leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .selectable(selected = false, onClick = { navController.navigate(Routes.CHANGE_PASSWORD) })
        )
        ListItem(
            headlineContent = { Text("スクリーンショットを防止") },
            supportingContent = { Text("有効にすると、アプリ内の画面を録画・撮影できなくなります") },
            trailingContent = {
                Switch(
                    checked = screenshotBlocked,
                    onCheckedChange = {
                        screenshotBlocked = it
                        settings.screenshotBlocked = it
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            "自動ロック",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Text(
            "アプリを離れてから、次に開いたときに再度パスワードを求めるタイミングです",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Column(modifier = Modifier.selectableGroup().padding(top = 4.dp)) {
            AutoLockOption.entries.forEach { option ->
                ListItem(
                    headlineContent = { Text(option.label) },
                    leadingContent = {
                        RadioButton(
                            selected = autoLock == option.minutes,
                            onClick = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = autoLock == option.minutes,
                            onClick = {
                                autoLock = option.minutes
                                settings.autoLockMinutes = option.minutes
                            }
                        )
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.developer_credit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                "バージョン 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                "写真・動画はすべて端末内で暗号化して保存され、サーバーへの送信は一切行われません。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
