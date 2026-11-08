// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.downloads

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.model.preview.libraryItem
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.settings.test.TestLibraryViewSettings
import app.campfire.user.test.FakeMediaProgressRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class DownloadsPresenterTest {

  private val navigator = FakeNavigator(DownloadsScreen)
  private val offlineDownloadManager = FakeOfflineDownloadManager()
  private val libraryItemRepository = FakeLibraryItemRepository()
  private val mediaProgressRepository = FakeMediaProgressRepository()
  private val libraryViewSettings = TestLibraryViewSettings()
  private val analytics = FakeAnalytics()

  private fun presenter() = DownloadsPresenter(
    navigator = navigator,
    offlineDownloadManager = offlineDownloadManager,
    libraryItemRepository = libraryItemRepository,
    mediaProgressRepository = mediaProgressRepository,
    libraryViewSettings = libraryViewSettings,
    analytics = analytics,
  )

  @Test
  fun respects_alternate_view_setting() = runTest {
    libraryViewSettings.continueSeriesAlternateView = false
    val item = libraryItem(id = "item_1")
    libraryItemRepository.libraryItem = item
    offlineDownloadManager.observeAllFlow.emit(
      listOf(OfflineDownload(libraryItemId = item.id, state = OfflineDownload.State.Completed)),
    )
    mediaProgressRepository.allProgressFlow.emit(emptyList())

    presenter().test {
      var item = awaitItem()
      while (item.isLoading) {
        item = awaitItem()
      }
      assertThat(item.alternateView).isFalse()

      libraryViewSettings.continueSeriesAlternateView = true
      val updated = awaitItem()
      assertThat(updated.alternateView).isTrue()
    }
  }

  @Test
  fun toggles_sort_direction_when_same_sort_selected() = runTest {
    presenter().test {
      val initial = awaitItem()
      assertThat(initial.sortMode).isEqualTo(DownloadsSort.RecentlyPlayed)
      assertThat(initial.sortAscending).isFalse()

      initial.eventSink(DownloadsUiEvent.SelectSort(DownloadsSort.RecentlyPlayed))
      val reversed = awaitItem()
      assertThat(reversed.sortAscending).isTrue()

      initial.eventSink(DownloadsUiEvent.SelectSort(DownloadsSort.Title))
      val titleSort = awaitItem()
      assertThat(titleSort.sortMode).isEqualTo(DownloadsSort.Title)
      assertThat(titleSort.sortAscending).isTrue()
    }
  }
}
