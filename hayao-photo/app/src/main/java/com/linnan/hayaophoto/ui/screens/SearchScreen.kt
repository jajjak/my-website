package com.linnan.hayaophoto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.linnan.hayaophoto.AppGraph
import com.linnan.hayaophoto.data.MediaItemEntity
import com.linnan.hayaophoto.ui.Routes
import com.linnan.hayaophoto.ui.components.DecryptedImage
import com.linnan.hayaophoto.viewmodel.PersonViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SearchTabContent(navController: NavHostController) {
    val personViewModel: PersonViewModel = viewModel()
    val persons by personViewModel.persons.collectAsState()
    var query by remember { mutableStateOf("") }
    val allMedia by remember { AppGraph.repository.observeAllMedia() }.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN) }
    val personById = persons.associateBy { it.id }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("名前・メモ・日付(yyyy/MM/dd)で検索") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))

        if (query.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "人物の名前・メモ、または日付で検索できます",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            return@Column
        }

        val matchedPersons = persons.filter {
            it.name.contains(query, ignoreCase = true) || it.memo.contains(query, ignoreCase = true)
        }
        val matchedMedia = allMedia.filter { item ->
            val dateStr = dateFormat.format(Date(item.createdAt))
            dateStr.contains(query) || personById[item.personId]?.name?.contains(query, ignoreCase = true) == true
        }
        val mediaRows = matchedMedia.chunked(3)

        if (matchedPersons.isEmpty() && matchedMedia.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("見つかりませんでした", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (matchedPersons.isNotEmpty()) {
                item {
                    Text("人物", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(matchedPersons, key = { "p${it.id}" }) { person ->
                    ListItem(
                        headlineContent = { Text(person.name) },
                        supportingContent = { if (person.memo.isNotBlank()) Text(person.memo) },
                        modifier = Modifier.clickable { navController.navigate(Routes.personDetail(person.id)) }
                    )
                }
            }
            if (matchedMedia.isNotEmpty()) {
                item {
                    Text(
                        "写真・動画（${matchedMedia.size}件）",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(mediaRows.size) { rowIndex ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        mediaRows[rowIndex].forEach { item ->
                            SearchMediaThumb(
                                item = item,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate(Routes.mediaViewer(item.personId, item.id)) }
                            )
                        }
                        repeat(3 - mediaRows[rowIndex].size) { Box(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchMediaThumb(item: MediaItemEntity, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
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
    }
}
