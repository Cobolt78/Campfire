// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.continueseries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.common.screens.ContinueSeriesScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.series.api.SeriesRepository
import app.campfire.user.api.MediaProgressRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import androidx.compose.runtime.LaunchedEffect
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(ContinueSeriesScreen::class, UserScope::class)
@Inject
class ContinueSeriesPresenter(
  @Assisted private val navigator: Navigator,
  private val seriesRepository: SeriesRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val settings: app.campfire.settings.api.CampfireSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<ContinueSeriesUiState> {

  @Composable
  override fun present(): ContinueSeriesUiState {
    val alternateView by remember { settings.observeContinueSeriesAlternateView() }.collectAsState(false)
    var sortMode by rememberRetainedSaveable {
      mutableStateOf(ContinueSeriesSort.Recent)
    }

    var sortAscending by rememberRetainedSaveable {
      mutableStateOf(false)
    }

    var cachedItems by rememberRetained {
      mutableStateOf<List<ContinueSeriesItem>?>(null)
    }

    LaunchedEffect(sortMode, sortAscending) {
      kotlinx.coroutines.flow.combine(
        seriesRepository.observeContinueSeries(),
        mediaProgressRepository.observeAllProgress().map { it.associateBy { p -> p.libraryItemId } },
      ) { seriesList, progressMap ->
        seriesList.mapNotNull { series ->
          val books = series.books?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
          val total = books.size
          val completedCount = books.count { book -> progressMap[book.id]?.isFinished == true }

          val hasStartedAny = series.inProgress || books.any { book ->
            val p = progressMap[book.id]
            p != null && (p.isFinished || p.progress > 0)
          }
          val hasActiveBook = books.any { book ->
            val p = progressMap[book.id]
            p != null && !p.isFinished && p.progress > 0
          }

          if (!hasStartedAny || completedCount >= total || hasActiveBook) {
            return@mapNotNull null
          }

          val nextUp = books.firstOrNull { book ->
            val p = progressMap[book.id]
            p != null && !p.isFinished && p.progress > 0
          } ?: books.firstOrNull { book ->
            val p = progressMap[book.id]
            p == null || !p.isFinished
          } ?: books.first()

          val nextUpProgress = progressMap[nextUp.id]
          val lastReadTime = maxOf(
            series.bookInProgressLastUpdate ?: 0L,
            books.maxOfOrNull { progressMap[it.id]?.lastUpdate ?: 0L } ?: 0L,
            series.updatedAt,
          )

          ContinueSeriesItem(
            series = series,
            nextUpBook = nextUp,
            nextUpBookProgress = nextUpProgress,
            totalBooks = total,
            completedBooks = completedCount,
            lastReadTimestamp = lastReadTime,
          )
        }.let { list ->
          val comparator = when (sortMode) {
            ContinueSeriesSort.Recent -> compareBy<ContinueSeriesItem> { it.lastReadTimestamp }
            ContinueSeriesSort.Name -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.series.name }
            ContinueSeriesSort.Progress -> compareBy<ContinueSeriesItem> {
              it.completedBooks.toFloat() / it.totalBooks.toFloat()
            }
            ContinueSeriesSort.DateAdded -> compareBy<ContinueSeriesItem> { it.series.addedAt }
          }
          if (sortAscending) list.sortedWith(comparator) else list.sortedWith(comparator.reversed())
        }
      }
      .flowOn(Dispatchers.Default)
      .collect {
        cachedItems = it
      }
    }

    val isLoading = cachedItems == null

    return ContinueSeriesUiState(
      items = cachedItems ?: emptyList(),
      isLoading = isLoading,
      sortMode = sortMode,
      sortAscending = sortAscending,
      alternateView = alternateView,
    ) { event ->
      when (event) {
        ContinueSeriesUiEvent.Back -> navigator.pop()

        is ContinueSeriesUiEvent.OpenSeries -> {
          analytics.send(ContentSelected(ContentType.Series))
          navigator.goTo(SeriesDetailScreen(event.series.id, event.series.name))
        }

        is ContinueSeriesUiEvent.OpenBook -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.book.id))
        }

        is ContinueSeriesUiEvent.SelectSort -> {
          if (sortMode == event.sort) {
            sortAscending = !sortAscending
          } else {
            sortMode = event.sort
            sortAscending = when (event.sort) {
              ContinueSeriesSort.Name -> true
              else -> false
            }
          }
          analytics.send(ActionEvent("continue_series_sort", "selected", event.sort.name))
        }

        ContinueSeriesUiEvent.ToggleSortDirection -> {
          sortAscending = !sortAscending
        }
      }
    }
  }
}
