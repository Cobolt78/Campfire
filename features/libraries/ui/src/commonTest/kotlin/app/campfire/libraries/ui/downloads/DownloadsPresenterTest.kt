// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.downloads

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.settings.DownloadsSort
import app.campfire.libraries.api.LibraryItemRepository
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest

class DownloadsPresenterTest {

  private val navigator = FakeNavigator(DownloadsScreen)
  private val offlineDownloadManager = FakeOfflineDownloadManager()
  private val libraryItemRepository = FakeLibraryItemRepository()
  private val mediaProgressRepository = FakeMediaProgressRepository()
  private val libraryViewSettings = TestLibraryViewSettings()
  private val analytics = FakeAnalytics()

  private fun presenter(
    itemRepo: LibraryItemRepository = libraryItemRepository,
  ) = DownloadsPresenter(
    navigator = navigator,
    offlineDownloadManager = offlineDownloadManager,
    libraryItemRepository = itemRepo,
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

  @Test
  fun custom_order_moves_items_and_persists_in_settings() = runTest {
    val itemA = libraryItem(id = "item_a").let { base ->
      val book = base.media as Media.Book
      base.copy(media = book.copy(metadata = book.metadata.copy(title = "Alpha")))
    }
    val itemB = libraryItem(id = "item_b").let { base ->
      val book = base.media as Media.Book
      base.copy(media = book.copy(metadata = book.metadata.copy(title = "Beta")))
    }
    val itemsById = mapOf(itemA.id to itemA, itemB.id to itemB)
    val multiRepo = object : LibraryItemRepository {
      override fun observeLibraryItem(itemId: LibraryItemId) = emptyFlow<LibraryItem>()
      override suspend fun getLibraryItem(itemId: LibraryItemId) = itemsById.getValue(itemId)
      override suspend fun deleteLibraryItem(itemId: LibraryItemId, hardDelete: Boolean) = Result.success(Unit)
    }

    offlineDownloadManager.observeAllFlow.emit(
      listOf(
        OfflineDownload(libraryItemId = itemA.id, state = OfflineDownload.State.Completed),
        OfflineDownload(libraryItemId = itemB.id, state = OfflineDownload.State.Completed),
      ),
    )
    mediaProgressRepository.allProgressFlow.emit(emptyList())
    libraryViewSettings.downloadsSortMode = DownloadsSort.Custom.storageKey
    libraryViewSettings.downloadsSortAscending = true
    libraryViewSettings.downloadsCustomOrder = listOf("item_a", "item_b")

    presenter(itemRepo = multiRepo).test {
      var loaded = awaitItem()
      while (loaded.isLoading) {
        loaded = awaitItem()
      }
      assertThat(loaded.items.map { it.libraryItem.id }).isEqualTo(listOf("item_a", "item_b"))

      loaded.eventSink(DownloadsUiEvent.MoveItem("item_b", -1))
      var reordered = awaitItem()
      while (reordered.items.map { it.libraryItem.id } != listOf("item_b", "item_a")) {
        reordered = awaitItem()
      }
      assertThat(libraryViewSettings.downloadsCustomOrder).isEqualTo(listOf("item_b", "item_a"))
      cancelAndIgnoreRemainingEvents()
    }
  }
}
