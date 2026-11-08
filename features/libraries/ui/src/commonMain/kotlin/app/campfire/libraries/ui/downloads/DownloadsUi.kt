// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.OverlappedNavigationBarInsets
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.SortAsc
import app.campfire.common.compose.icons.rounded.SortDesc
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.layout.ContentScale
import app.campfire.common.compose.widgets.ElevatedContentCard
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.common.compose.widgets.LibraryItemListItem
import app.campfire.common.compose.widgets.MediaFinishedIndicator
import app.campfire.common.compose.widgets.MediaProgressBar
import app.campfire.common.compose.widgets.OfflineStatusIndicator
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.common.compose.widgets.adaptiveEnterAlwaysScrollBehavior
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.model.LibraryItem
import app.campfire.core.di.UserScope
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import app.campfire.ui.navigation.bar.CampfireNavigationBarWindowInsets
import com.slack.circuit.codegen.annotations.CircuitInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(DownloadsScreen::class, UserScope::class)
@Composable
fun Downloads(
  state: DownloadsUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = adaptiveEnterAlwaysScrollBehavior()
  AttachScrollBehaviorToLocalNavigationBar(scrollBehavior)
  var showSortMenu by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = {
          Column {
            Text("Downloads")
            if (!state.isLoading && state.items.isNotEmpty()) {
              val countLabel = if (state.items.size == 1) "1 book" else "${state.items.size} books"
              Text(
                text = "$countLabel • ${formatFileSize(state.totalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        navigationIcon = {
          NavigationBackButton(onClick = { state.eventSink(DownloadsUiEvent.Back) })
        },
        actions = {
          Box {
            IconButton(onClick = { showSortMenu = true }) {
              Icon(
                imageVector = if (state.sortAscending) CampfireIcons.Rounded.SortAsc else CampfireIcons.Rounded.SortDesc,
                contentDescription = "Sort options",
              )
            }
            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false },
            ) {
              DownloadsSort.entries.forEach { sort ->
                DropdownMenuItem(
                  text = {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Text(
                        text = sort.label,
                        fontWeight = if (state.sortMode == sort) FontWeight.Bold else FontWeight.Normal,
                      )
                      if (state.sortMode == sort) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                          imageVector = if (state.sortAscending) CampfireIcons.Rounded.SortAsc else CampfireIcons.Rounded.SortDesc,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                          tint = MaterialTheme.colorScheme.primary,
                        )
                      }
                    }
                  },
                  onClick = {
                    state.eventSink(DownloadsUiEvent.SelectSort(sort))
                    showSortMenu = false
                  },
                )
              }
            }
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets
      .exclude(OverlappedNavigationBarInsets)
      .add(CampfireNavigationBarWindowInsets),
  ) { paddingValues ->
    when {
      state.isLoading -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }

      state.items.isEmpty() -> {
        EmptyState(
          message = "No downloaded audiobooks",
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        )
      }

      else -> {
        if (state.alternateView) {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
              top = paddingValues.calculateTopPadding() + 8.dp,
              bottom = paddingValues.calculateBottomPadding() + 16.dp,
              start = 16.dp,
              end = 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(
              items = state.items,
              key = { it.libraryItem.id },
            ) { item ->
              DownloadedBookGridCard(
                item = item,
                onBookClick = { state.eventSink(DownloadsUiEvent.ItemClick(item.libraryItem)) },
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
              top = paddingValues.calculateTopPadding() + 8.dp,
              bottom = paddingValues.calculateBottomPadding() + 16.dp,
              start = 16.dp,
              end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(
              items = state.items,
              key = { it.libraryItem.id },
            ) { item ->
              val subtitle = buildString {
                item.libraryItem.media.metadata.authorName?.let { append(it) }
                if (item.download.contentLength > 0) {
                  if (isNotEmpty()) append(" • ")
                  append(formatFileSize(item.download.contentLength))
                }
              }

              LibraryItemListItem(
                libraryItem = item.libraryItem,
                onClick = { state.eventSink(DownloadsUiEvent.ItemClick(item.libraryItem)) },
                mediaProgress = item.progress,
                offlineStatus = item.download.asWidgetStatus(),
                subtitleOverride = subtitle.ifEmpty { null },
              )
            }
          }
        }
      }
    }
  }
}

private fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val kb = bytes / 1024.0
  val mb = kb / 1024.0
  val gb = mb / 1024.0
  return when {
    gb >= 1.0 -> "${(gb * 10).roundToInt() / 10.0} GB"
    mb >= 1.0 -> "${(mb * 10).roundToInt() / 10.0} MB"
    kb >= 1.0 -> "${(kb * 10).roundToInt() / 10.0} KB"
    else -> "$bytes B"
  }
}

@Composable
private fun DownloadedBookGridCard(
  item: DownloadedBookItem,
  onBookClick: (LibraryItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedContentCard(
    modifier = modifier,
    onClick = { onBookClick(item.libraryItem) },
  ) {
    Column {
      Box(modifier = Modifier.aspectRatio(1f).fillMaxWidth()) {
        ItemImage(
          imageUrl = item.libraryItem.media.coverImageUrl,
          contentDescription = item.libraryItem.media.metadata.title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )

        OfflineStatusIndicator(
          status = item.download.asWidgetStatus(),
          size = 18.dp,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp),
        )

        val isFinished = item.progress?.isFinished == true
        if (isFinished) {
          MediaFinishedIndicator(
            size = 20.dp,
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp),
          )
        } else if (item.progress != null && item.progress.actualProgress > 0f) {
          MediaProgressBar(
            mediaProgress = item.progress,
            trackHeight = 4.dp,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth(),
          )
        }
      }

      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = item.libraryItem.media.metadata.title ?: "Untitled",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          modifier = Modifier.basicMarquee(),
        )

        Spacer(modifier = Modifier.height(2.dp))

        val subtitle = buildString {
          item.libraryItem.media.metadata.authorName?.let { append(it) }
          if (item.download.contentLength > 0) {
            if (isNotEmpty()) append(" • ")
            append(formatFileSize(item.download.contentLength))
          }
        }

        if (subtitle.isNotEmpty()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier.basicMarquee(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
