// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
  modifier: Modifier = Modifier,
) {
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
    )
    ShelfContent(
      shelf = shelf,
      offlineStatus = offlineStatus,
      progressStatus = progressStatus,
      onItemClick = onItemClick,
      onViewAllUpcomingClick = onViewAllUpcomingClick,
    )
  }
}
