// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.downloads

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.OverlappedNavigationBarInsets
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.DragIndicator
import app.campfire.common.compose.icons.rounded.SortAsc
import app.campfire.common.compose.icons.rounded.SortDesc
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.ElevatedContentCard
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.common.compose.widgets.LibraryItemListItem
import app.campfire.common.compose.widgets.MediaFinishedIndicator
import app.campfire.common.compose.widgets.MediaProgressBar
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.common.compose.widgets.OfflineStatusIndicator
import app.campfire.common.compose.widgets.adaptiveEnterAlwaysScrollBehavior
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.settings.DownloadsSort
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import app.campfire.ui.navigation.bar.CampfireNavigationBarWindowInsets
import com.slack.circuit.codegen.annotations.CircuitInject
import kotlin.math.roundToInt
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberScroller

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
        val haptics = LocalHapticFeedback.current
        val density = LocalDensity.current
        val maxAutoScrollPxPerSecond = with(density) { 1800.dp.toPx() }
        val baseViewConfig = LocalViewConfiguration.current
        val reorderViewConfig = remember(baseViewConfig) {
          object : ViewConfiguration by baseViewConfig {
            override val longPressTimeoutMillis: Long = 500L
          }
        }
        val isCustomSort = state.sortMode == DownloadsSort.Custom
        val showHint = isCustomSort && state.items.size > 1

        CompositionLocalProvider(LocalViewConfiguration provides reorderViewConfig) {
          if (state.alternateView) {
            val gridState = rememberLazyGridState()
            val gridScroller = rememberScroller(
              scrollableState = gridState,
              pixelPerSecond = maxAutoScrollPxPerSecond,
            )
            val reorderGridState = rememberReorderableLazyGridState(
              lazyGridState = gridState,
              scrollThresholdPadding = paddingValues,
              scrollThreshold = 120.dp,
              scroller = gridScroller,
            ) { from, to ->
              val fromKey = from.key as? String ?: return@rememberReorderableLazyGridState
              val toKey = to.key as? String ?: return@rememberReorderableLazyGridState
              haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
              state.eventSink(DownloadsUiEvent.ReorderItem(fromKey, toKey))
            }

            LazyVerticalGrid(
              state = gridState,
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
              if (showHint) {
                item(
                  key = "custom_reorder_hint",
                  span = { GridItemSpan(maxLineSpan) },
                  contentType = "hint",
                ) {
                  CustomReorderHintBanner()
                }
              }

              items(
                items = state.items,
                key = { it.libraryItem.id },
              ) { item ->
                ReorderableItem(reorderGridState, key = item.libraryItem.id) { isDragging ->
                  val interactionSource = remember { MutableInteractionSource() }
                  DownloadedBookGridCard(
                    item = item,
                    onBookClick = { state.eventSink(DownloadsUiEvent.ItemClick(item.libraryItem)) },
                    modifier = Modifier
                      .animateItem()
                      .downloadReorderModifier(
                        scope = this@ReorderableItem,
                        enabled = isCustomSort,
                        isDragging = isDragging,
                        interactionSource = interactionSource,
                        onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDragStopped = {
                          haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                          state.eventSink(DownloadsUiEvent.ReorderStopped)
                        },
                      ),
                  )
                }
              }
            }
          } else {
            val listState = rememberLazyListState()
            val listScroller = rememberScroller(
              scrollableState = listState,
              pixelPerSecond = maxAutoScrollPxPerSecond,
            )
            val reorderListState = rememberReorderableLazyListState(
              lazyListState = listState,
              scrollThresholdPadding = paddingValues,
              scrollThreshold = 120.dp,
              scroller = listScroller,
            ) { from, to ->
              val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
              val toKey = to.key as? String ?: return@rememberReorderableLazyListState
              haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
              state.eventSink(DownloadsUiEvent.ReorderItem(fromKey, toKey))
            }

            LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp,
              ),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (showHint) {
                item(
                  key = "custom_reorder_hint",
                  contentType = "hint",
                ) {
                  CustomReorderHintBanner()
                }
              }

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

                ReorderableItem(reorderListState, key = item.libraryItem.id) { isDragging ->
                  val interactionSource = remember { MutableInteractionSource() }
                  LibraryItemListItem(
                    libraryItem = item.libraryItem,
                    onClick = { state.eventSink(DownloadsUiEvent.ItemClick(item.libraryItem)) },
                    mediaProgress = item.progress,
                    offlineStatus = item.download.asWidgetStatus(),
                    subtitleOverride = subtitle.ifEmpty { null },
                    interactionSource = interactionSource,
                    modifier = Modifier
                      .animateItem()
                      .downloadReorderModifier(
                        scope = this@ReorderableItem,
                        enabled = isCustomSort,
                        isDragging = isDragging,
                        interactionSource = interactionSource,
                        onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDragStopped = {
                          haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                          state.eventSink(DownloadsUiEvent.ReorderStopped)
                        },
                      ),
                  )
                }
              }
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
private fun Modifier.downloadReorderModifier(
  scope: ReorderableCollectionItemScope,
  enabled: Boolean,
  isDragging: Boolean,
  interactionSource: MutableInteractionSource,
  onDragStarted: () -> Unit,
  onDragStopped: () -> Unit,
): Modifier {
  val scale by animateFloatAsState(if (isDragging) 1.03f else 1f)
  val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp)
  val cardShape = MaterialTheme.shapes.large
  val base = this
    .zIndex(if (isDragging) 1f else 0f)
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
      shape = cardShape
      shadowElevation = elevation.toPx()
    }
    .then(
      if (isDragging) {
        Modifier.border(
          width = 1.5.dp,
          color = MaterialTheme.colorScheme.primary,
          shape = cardShape,
        )
      } else {
        Modifier
      },
    )
  return with(scope) {
    base.longPressDraggableHandle(
      enabled = enabled,
      interactionSource = interactionSource,
      onDragStarted = { onDragStarted() },
      onDragStopped = onDragStopped,
    )
  }
}

@Composable
private fun CustomReorderHintBanner(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = CampfireIcons.Rounded.DragIndicator,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = "Hold and drag any book to reorder",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
      )
    }
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
