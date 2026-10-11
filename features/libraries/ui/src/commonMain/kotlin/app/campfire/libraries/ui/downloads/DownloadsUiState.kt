// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.downloads

import androidx.compose.runtime.Immutable
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class DownloadedBookItem(
  val libraryItem: LibraryItem,
  val download: OfflineDownload,
  val progress: MediaProgress?,
)

enum class DownloadsSort(val label: String) {
  RecentlyPlayed("Recently Played"),
  DateAdded("Date Added"),
  Title("Title A–Z"),
  Author("Author A–Z"),
  Size("File Size"),
}

@Immutable
data class DownloadsUiState(
  val items: List<DownloadedBookItem>,
  val totalBytes: Long,
  val isLoading: Boolean,
  val sortMode: DownloadsSort,
  val sortAscending: Boolean,
  val eventSink: (DownloadsUiEvent) -> Unit,
) : CircuitUiState

sealed interface DownloadsUiEvent : CircuitUiEvent {
  data object Back : DownloadsUiEvent
  data class ItemClick(val item: LibraryItem) : DownloadsUiEvent
  data class SelectSort(val sort: DownloadsSort) : DownloadsUiEvent
  data object ToggleSortDirection : DownloadsUiEvent
}
