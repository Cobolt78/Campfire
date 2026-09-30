// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.ShelfEntity
import app.campfire.core.offline.OfflineStatus
import app.campfire.home.api.model.ShelfIds
import app.campfire.home.ui.UiShelf

@Composable
fun ShelfListItem(
  shelf: UiShelf<ShelfEntity>,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  progressStatus: (LibraryItemId, PodcastEpisodeId?) -> MediaProgress?,
  onItemClick: (Any) -> Unit,
  onViewAllUpcomingClick: () -> Unit,
  onHeaderClick: ((String) -> Unit)? = null,
  onRefreshDiscoveriesClick: (() -> Unit)? = null,
  isRefreshingDiscoveries: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val isDiscover = shelf.id.startsWith(ShelfIds.Discover)
  val isSupportedClickableShelf = when {
    shelf.id.startsWith(ShelfIds.ContinueListening) -> true
    shelf.id.startsWith(ShelfIds.ListenAgain) -> true
    shelf.id.startsWith(ShelfIds.RecentlyAdded) -> true
    shelf.id.startsWith(ShelfIds.RecentSeries) -> true
    shelf.id.startsWith(ShelfIds.ContinueSeries) -> true
    shelf.id == "downloads" -> true
    shelf.id.startsWith(ShelfIds.UpcomingReleases) -> true
    shelf.id.startsWith(ShelfIds.NewestAuthors) -> true
    else -> false
  }

  val listState = rememberLazyListState()

  if (isDiscover) {
    val entityIds = shelf.entities.dataOrNull?.map { entity ->
      when (entity) {
        is LibraryItem -> entity.id
        else -> entity.toString()
      }
    }
    LaunchedEffect(entityIds) {
      if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
        listState.scrollToItem(0)
      }
    }
  }

  Column(
    modifier = modifier,
  ) {
    ShelfHeader(
      shelf = shelf,
      onHeaderClick = if (isSupportedClickableShelf && onHeaderClick != null) {
        { onHeaderClick(shelf.id) }
      } else {
        null
      },
      onRefreshClick = if (isDiscover && onRefreshDiscoveriesClick != null) onRefreshDiscoveriesClick else null,
      isRefreshing = isDiscover && isRefreshingDiscoveries,
    )
    ShelfContent(
      shelf = shelf,
      offlineStatus = offlineStatus,
      progressStatus = progressStatus,
      onItemClick = onItemClick,
      onViewAllUpcomingClick = onViewAllUpcomingClick,
      state = listState,
    )
  }
}
