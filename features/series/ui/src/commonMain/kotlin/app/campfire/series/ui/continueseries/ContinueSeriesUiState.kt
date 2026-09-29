// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.continueseries

import androidx.compose.runtime.Immutable
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Series
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class ContinueSeriesItem(
  val series: Series,
  val nextUpBook: LibraryItem,
  val nextUpBookProgress: MediaProgress?,
  val totalBooks: Int,
  val completedBooks: Int,
  val lastReadTimestamp: Long,
)

enum class ContinueSeriesSort(val label: String) {
  Recent("Recently Played"),
  Name("Series Name"),
  Progress("Progress"),
  DateAdded("Date Added"),
}

@Immutable
data class ContinueSeriesUiState(
  val items: List<ContinueSeriesItem>,
  val isLoading: Boolean,
  val sortMode: ContinueSeriesSort,
  val sortAscending: Boolean,
  val eventSink: (ContinueSeriesUiEvent) -> Unit,
) : CircuitUiState

sealed interface ContinueSeriesUiEvent : CircuitUiEvent {
  data object Back : ContinueSeriesUiEvent
  data class OpenSeries(val series: Series) : ContinueSeriesUiEvent
  data class OpenBook(val book: LibraryItem) : ContinueSeriesUiEvent
  data class SelectSort(val sort: ContinueSeriesSort) : ContinueSeriesUiEvent
  data object ToggleSortDirection : ContinueSeriesUiEvent
}
