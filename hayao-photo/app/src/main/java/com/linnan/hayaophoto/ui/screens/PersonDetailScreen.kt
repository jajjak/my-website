package com.linnan.hayaophoto.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.linnan.hayaophoto.AppGraph
import com.linnan.hayaophoto.crypto.SortOrder
import com.linnan.hayaophoto.data.MediaItemEntity
import com.linnan.hayaophoto.media.MediaStoreExporter
import com.linnan.hayaophoto.ui.Routes
import com.linnan.hayaophoto.ui.components.DecryptedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(personId: Long, navController: NavHostController) {
    val repo = AppGraph.repository
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val person by remember(personId) { repo.observePerson(personId) }.collectAsState(initial = null)
    val mediaList by remember(personId) { repo.observeMedia(personId) }.collectAsState(initial = emptyList())

    var searchMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }
    var selection by remember { mutableStateOf(setOf<Long>()) }
    var showAddMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN) }

    val filtered = remember(mediaList, query, sortOrder) {
        var list = mediaList
        if (query.isNotBlank()) {
            list = list.filter { dateFormat.format(Date(it.createdAt)).contains(query) }
        }
        if (sortOrder == SortOrder.OLDEST_FIRST) list.reversed() else list
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    uris.forEach { uri ->
                        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val isVideo = mime.startsWith("video")
                        runCatching { repo.addMediaFromUri(personId, uri, isVideo, mime) }
                    }
                }
                snackbarHostState.showSnackbar("${uris.size}件を取り込みました")
            }
        }
    }

    fun exitSelection() {
        selection = emptySet()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selection.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selection.size}件を選択中") },
                    navigationIcon = {
                        IconButton(onClick = { exitSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "選択を解除")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                val items = mediaList.filter { it.id in selection }
                                var success = 0
                                withContext(Dispatchers.IO) {
                                    items.forEach {
                                        if (MediaStoreExporter.export(context, AppGraph.mediaCrypto, it) != null) success++
                                    }
                                }
                                snackbarHostState.showSnackbar("$success 件を端末のギャラリーに書き出しました")
                                exitSelection()
                            }
                        }) {
                            Icon(Icons.Filled.FileDownload, contentDescription = "書き出し")
                        }
                        IconButton(onClick = { pendingDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "削除")
                        }
                    }
                )
            } else if (searchMode) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("日付で検索 (yyyy/MM/dd)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { searchMode = false; query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "検索を閉じる")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(person?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "戻る")
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchMode = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "検索")
                        }
                        IconButton(onClick = {
                            sortOrder = if (sortOrder == SortOrder.NEWEST_FIRST) SortOrder.OLDEST_FIRST else SortOrder.NEWEST_FIRST
                        }) {
                            Icon(Icons.Filled.SwapVert, contentDescription = "並べ替え")
                        }
                        IconButton(onClick = { navController.navigate(Routes.addEditPerson(personId)) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "編集")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (selection.isEmpty()) {
                FloatingActionButton(onClick = { showAddMenu = true }) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "追加")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!person?.memo.isNullOrBlank() && selection.isEmpty() && !searchMode) {
                Text(
                    person!!.memo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (query.isNotBlank()) "見つかりませんでした" else "右下のボタンから写真・動画を追加しましょう",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { item ->
                        MediaThumb(
                            item = item,
                            selected = item.id in selection,
                            selectionActive = selection.isNotEmpty(),
                            onClick = {
                                if (selection.isNotEmpty()) {
                                    selection = if (item.id in selection) selection - item.id else selection + item.id
                                } else {
                                    navController.navigate(Routes.mediaViewer(personId, item.id))
                                }
                            },
                            onLongPress = { selection = selection + item.id }
                        )
                    }
                }
            }
        }
    }

    if (showAddMenu) {
        AlertDialog(
            onDismissRequest = { showAddMenu = false },
            title = { Text("追加する方法を選択") },
            text = {
                Column {
                    TextButton(onClick = {
                        showAddMenu = false
                        navController.navigate(Routes.camera(personId))
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Text("  カメラで撮影する")
                    }
                    TextButton(onClick = {
                        showAddMenu = false
                        importLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Text("  端末から取り込む")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMenu = false }) { Text("キャンセル") }
            }
        )
    }

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text("${selection.size}件を削除しますか？") },
            text = { Text("この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    val items = mediaList.filter { it.id in selection }
                    scope.launch {
                        withContext(Dispatchers.IO) { repo.deleteMediaItems(items) }
                        exitSelection()
                        pendingDelete = false
                    }
                }) { Text("削除する", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) { Text("キャンセル") }
            }
        )
    }
}

@Composable
private fun MediaThumb(
    item: MediaItemEntity,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        DecryptedImage(
            personId = item.personId,
            fileName = item.thumbFileName ?: item.fileName,
            maxDimension = 300,
            modifier = Modifier.fillMaxSize()
        )
        if (item.isVideo) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(28.dp)
            )
        }
        if (selectionActive) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
