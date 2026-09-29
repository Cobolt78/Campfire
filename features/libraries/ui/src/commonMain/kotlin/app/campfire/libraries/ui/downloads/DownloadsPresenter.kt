// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.downloads

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
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.di.UserScope
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.user.api.MediaProgressRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@CircuitInject(DownloadsScreen::class, UserScope::class)
@Inject
class DownloadsPresenter(
  @Assisted private val navigator: Navigator,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val libraryItemRepository: LibraryItemRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val analytics: Analytics,
) : NonPausablePresenter<DownloadsUiState> {

  @Composable
  override fun present(): DownloadsUiState {
    var sortMode by rememberRetainedSaveable {
      mutableStateOf(DownloadsSort.RecentlyPlayed)
    }

    var sortAscending by rememberRetainedSaveable {
      mutableStateOf(false)
    }

    val completedDownloads by remember {
      offlineDownloadManager.observeAll()
        .map { downloads ->
          downloads.filter { it.isCompleted }
        }
        .distinctUntilChanged()
        .mapLatest { downloads ->
          downloads.mapNotNull { dl ->
            try {
              val item = libraryItemRepository.getLibraryItem(dl.libraryItemId)
              dl to item
            } catch (e: CancellationException) {
              throw e
            } catch (e: Exception) {
              null
            }
          }
        }
        .catch { emit(emptyList()) }
    }.collectAsState(null)

    val userMediaProgress by remember {
      mediaProgressRepository.observeAllProgress()
        .map { allProgress ->
          allProgress.associateBy { it.libraryItemId }
        }
        .catch { emit(emptyMap()) }
    }.collectAsState(null)

    val isLoading = completedDownloads == null || userMediaProgress == null

    val items = remember(completedDownloads, userMediaProgress, sortMode, sortAscending) {
      val dlList = completedDownloads ?: return@remember emptyList()
      val progressMap = userMediaProgress ?: emptyMap()

      val rawItems = dlList.map { (dl, item) ->
        DownloadedBookItem(
          libraryItem = item,
          download = dl,
          progress = progressMap[item.id],
        )
      }

      val comparator = when (sortMode) {
        DownloadsSort.RecentlyPlayed -> compareBy<DownloadedBookItem> { it.progress?.lastUpdate ?: 0L }
        DownloadsSort.DateAdded -> compareBy<DownloadedBookItem> {
          if (it.download.updateTimeMs > 0) it.download.updateTimeMs else it.libraryItem.addedAtMillis
        }
        DownloadsSort.Title -> compareBy(String.CASE_INSENSITIVE_ORDER) {
          it.libraryItem.media.metadata.title ?: ""
        }
        DownloadsSort.Author -> compareBy(String.CASE_INSENSITIVE_ORDER) {
          it.libraryItem.media.metadata.authorName ?: ""
        }
        DownloadsSort.Size -> compareBy<DownloadedBookItem> {
          it.download.contentLength.coerceAtLeast(0L)
        }
      }

      if (sortAscending) {
        rawItems.sortedWith(comparator)
      } else {
        rawItems.sortedWith(comparator.reversed())
      }
    }

    val totalBytes = remember(items) {
      items.sumOf { it.download.contentLength.coerceAtLeast(0L) }
    }

    return DownloadsUiState(
      items = items,
      totalBytes = totalBytes,
      isLoading = isLoading,
      sortMode = sortMode,
      sortAscending = sortAscending,
    ) { event ->
      when (event) {
        DownloadsUiEvent.Back -> navigator.pop()

        is DownloadsUiEvent.ItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.item.id))
        }

        is DownloadsUiEvent.SelectSort -> {
          if (sortMode == event.sort) {
            sortAscending = !sortAscending
          } else {
            sortMode = event.sort
            sortAscending = when (event.sort) {
              DownloadsSort.Title, DownloadsSort.Author -> true
              else -> false
            }
          }
          analytics.send(ActionEvent("downloads_sort", "selected", event.sort.name))
        }

        DownloadsUiEvent.ToggleSortDirection -> {
          sortAscending = !sortAscending
        }
      }
    }
  }
}
