// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.di.UserScope
import app.campfire.core.settings.DownloadsSort
import app.campfire.core.settings.sortDownloadedItems
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.settings.api.LibraryViewSettings
import app.campfire.user.api.MediaProgressRepository
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
@CircuitInject(DownloadsScreen::class, UserScope::class)
@Inject
class DownloadsPresenter(
  private val navigator: Navigator,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val libraryItemRepository: LibraryItemRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val libraryViewSettings: LibraryViewSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<DownloadsUiState> {

  @Composable
  override fun present(): DownloadsUiState {
    val sortModeRaw by remember {
      libraryViewSettings.observeDownloadsSortMode()
    }.collectAsState()
    val sortMode = DownloadsSort.fromStorageKey(sortModeRaw)

    val sortAscending by remember {
      libraryViewSettings.observeDownloadsSortAscending()
    }.collectAsState()

    val customOrder by remember {
      libraryViewSettings.observeDownloadsCustomOrder()
    }.collectAsState()

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

    val alternateView by remember {
      libraryViewSettings.observeContinueSeriesAlternateView()
    }.collectAsState(false)

    val isLoading = completedDownloads == null || userMediaProgress == null

    val items = remember(completedDownloads, userMediaProgress, sortMode, sortAscending, customOrder) {
      val dlList = completedDownloads ?: return@remember emptyList()
      val progressMap = userMediaProgress ?: emptyMap()

      val rawItems = dlList.map { (dl, item) ->
        DownloadedBookItem(
          libraryItem = item,
          download = dl,
          progress = progressMap[item.id],
        )
      }

      sortDownloadedItems(
        items = rawItems,
        sortMode = sortMode,
        sortAscending = sortAscending,
        customOrder = customOrder,
        libraryItem = { it.libraryItem },
        progress = { it.progress },
        downloadUpdateTimeMs = { it.download.updateTimeMs },
        downloadContentLength = { it.download.contentLength },
      )
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
      alternateView = alternateView,
    ) { event ->
      when (event) {
        DownloadsUiEvent.Back -> navigator.pop()

        is DownloadsUiEvent.ItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.item.id))
        }

        is DownloadsUiEvent.SelectSort -> {
          if (sortMode == event.sort) {
            libraryViewSettings.downloadsSortAscending = !sortAscending
          } else {
            if (event.sort == DownloadsSort.Custom && customOrder.isEmpty() && items.isNotEmpty()) {
              libraryViewSettings.downloadsCustomOrder = items.map { it.libraryItem.id }
            }
            libraryViewSettings.downloadsSortMode = event.sort.storageKey
            libraryViewSettings.downloadsSortAscending = event.sort.defaultAscending
          }
          analytics.send(ActionEvent("downloads_sort", "selected", event.sort.name))
        }

        DownloadsUiEvent.ToggleSortDirection -> {
          libraryViewSettings.downloadsSortAscending = !sortAscending
        }

        is DownloadsUiEvent.MoveItem -> {
          val currentIds = items.map { it.libraryItem.id }.toMutableList()
          val index = currentIds.indexOf(event.itemId)
          if (index != -1) {
            val targetIndex = (index + event.delta).coerceIn(0, currentIds.lastIndex)
            if (targetIndex != index) {
              val moved = currentIds.removeAt(index)
              currentIds.add(targetIndex, moved)
              libraryViewSettings.downloadsCustomOrder = currentIds
              if (!sortAscending) {
                libraryViewSettings.downloadsSortAscending = true
              }
            }
          }
        }
      }
    }
  }
}
