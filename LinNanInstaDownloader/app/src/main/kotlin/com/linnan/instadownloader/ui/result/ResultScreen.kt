package com.linnan.instadownloader.ui.result

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.linnan.instadownloader.R
import com.linnan.instadownloader.data.model.InstagramPost
import com.linnan.instadownloader.data.model.MediaItem
import com.linnan.instadownloader.data.model.MediaType
import com.linnan.instadownloader.data.model.PostType
import com.linnan.instadownloader.download.DownloadState
import com.linnan.instadownloader.ui.AnalyzeState
import com.linnan.instadownloader.ui.AppViewModel
import com.linnan.instadownloader.ui.common.formatFileSize
import com.linnan.instadownloader.ui.common.formatResolution

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(viewModel: AppViewModel) {
    val analyzeState by viewModel.analyzeState.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val savedInfo by viewModel.savedInfo.collectAsStateWithLifecycle()
    val post = (analyzeState as? AnalyzeState.Success)?.post ?: return

    val pagerState = rememberPagerState(pageCount = { post.items.size })
    val currentItem = post.items[pagerState.currentPage.coerceIn(post.items.indices)]
    val currentDownload = downloadStates[currentItem.id]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.goHome() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val item = post.items[page]
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (item.type == MediaType.VIDEO) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(56.dp)
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.result_type_video),
                                tint = Color.White,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            if (post.items.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(post.items.size) { index ->
                        val active = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(if (active) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text(
                text = "Instagram" + (post.ownerUsername?.let { " ・ @$it" } ?: ""),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text(postTypeLabel(post.postType)) })
                AssistChip(onClick = {}, label = { Text(formatResolution(currentItem.width, currentItem.height)) })
                AssistChip(onClick = {}, label = { Text(formatFileSize(currentItem.fileSizeBytes)) })
            }

            SaveButton(
                downloadState = currentDownload,
                onClick = { viewModel.saveItem(currentItem) },
                modifier = Modifier.padding(top = 24.dp)
            )

            if (post.items.size > 1) {
                OutlinedButton(
                    onClick = { viewModel.saveAll() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(stringResource(R.string.result_save_all))
                }

                Text(
                    text = stringResource(R.string.result_media_count, post.items.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(post.items, key = { it.id }) { item ->
                        MediaThumbCard(
                            item = item,
                            downloadState = downloadStates[item.id],
                            onSave = { viewModel.saveItem(item) }
                        )
                    }
                }
            }
        }
    }

    savedInfo?.let { info ->
        SavedDialog(
            savedInfo = info,
            onDismiss = { viewModel.dismissSavedInfo() }
        )
    }
}

@Composable
private fun postTypeLabel(type: PostType): String = when (type) {
    PostType.PHOTO -> stringResource(R.string.result_type_photo)
    PostType.VIDEO -> stringResource(R.string.result_type_video)
    PostType.REEL -> stringResource(R.string.result_type_reel)
    PostType.CAROUSEL -> stringResource(R.string.result_type_carousel)
}

@Composable
private fun SaveButton(
    downloadState: DownloadState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = onClick,
            enabled = downloadState !is DownloadState.Progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            when (downloadState) {
                is DownloadState.Progress -> {
                    val percent = downloadState.totalBytes?.let {
                        ((downloadState.downloadedBytes * 100) / it).toInt()
                    }
                    Text(if (percent != null) "$percent%" else stringResource(R.string.download_title))
                }
                is DownloadState.Completed -> {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(stringResource(R.string.download_done_title))
                }
                else -> Text(stringResource(R.string.result_save_best), fontWeight = FontWeight.Bold)
            }
        }
        if (downloadState is DownloadState.Progress) {
            val progress = downloadState.totalBytes?.let {
                (downloadState.downloadedBytes.toFloat() / it.toFloat()).coerceIn(0f, 1f)
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun MediaThumbCard(
    item: MediaItem,
    downloadState: DownloadState?,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.type == MediaType.VIDEO) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }

        when (downloadState) {
            is DownloadState.Progress -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
            is DownloadState.Completed -> {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            else -> {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                        Text(
                            text = stringResource(R.string.result_save_one),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
