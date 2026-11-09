// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.continueseries

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.common.screens.ContinueSeriesScreen
import app.campfire.common.test.mediaProgress
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.home.ui.series
import app.campfire.series.test.FakeSeriesRepository
import app.campfire.settings.test.TestLibraryViewSettings
import app.campfire.user.test.FakeMediaProgressRepository
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class ContinueSeriesPresenterTest {

  private val seriesRepository = FakeSeriesRepository()
  private val mediaProgressRepository = FakeMediaProgressRepository()
  private val libraryViewSettings = TestLibraryViewSettings()
  private val navigator = FakeNavigator(ContinueSeriesScreen)
  private val offlineDownloadManager = FakeOfflineDownloadManager()
  private val analytics = FakeAnalytics()

  private fun presenter() = ContinueSeriesPresenter(
    seriesRepository = seriesRepository,
    mediaProgressRepository = mediaProgressRepository,
    libraryViewSettings = libraryViewSettings,
    navigator = navigator,
    offlineDownloadManager = offlineDownloadManager,
    analytics = analytics,
  )

  @BeforeTest
  fun setup() {
    ContinueSeriesPresenter.clearMemoryCache()
  }

  @Test
  fun seriesWithActiveBookInProgressIsExcluded() = runTest {
    val book1 = libraryItem(id = "b1", media = media(metadata = mediaMetadata(title = "Book 1")))
    val book2 = libraryItem(id = "b2", media = media(metadata = mediaMetadata(title = "Book 2")))
    val testSeries = series(id = "s1", name = "Alex Cross", books = listOf(book1, book2))

    // Book 1 is finished, Book 2 has currentTime > 0 and is not finished (active in progress)
    val p1 = mediaProgress(libraryItemId = "b1", isFinished = true, progress = 1f, currentTime = 1000f)
    val p2 = mediaProgress(libraryItemId = "b2", isFinished = false, progress = 0f, currentTime = 45f)

    seriesRepository.continueSeriesFlow.tryEmit(listOf(testSeries))
    mediaProgressRepository.allProgressFlow.tryEmit(listOf(p1, p2))

    presenter().test {
      var state = awaitItem()
      while (state.isLoading) {
        state = awaitItem()
      }
      // Since Book 2 is active in progress, the series must be excluded from Continue Series
      assertThat(state.items).isEmpty()
    }
  }

  @Test
  fun seriesWithFinishedBookAndUnreadSequelIsIncluded() = runTest {
    val book1 = libraryItem(id = "b1", media = media(metadata = mediaMetadata(title = "Book 1")))
    val book2 = libraryItem(id = "b2", media = media(metadata = mediaMetadata(title = "Book 2")))
    val testSeries = series(id = "s1", name = "Alex Cross", books = listOf(book1, book2))

    // Book 1 is finished, Book 2 is unread
    val p1 = mediaProgress(libraryItemId = "b1", isFinished = true, progress = 1f, currentTime = 1000f)

    seriesRepository.continueSeriesFlow.tryEmit(listOf(testSeries))
    mediaProgressRepository.allProgressFlow.tryEmit(listOf(p1))

    presenter().test {
      var state = awaitItem()
      while (state.items.isEmpty()) {
        state = awaitItem()
      }
      assertThat(state.items.size).isEqualTo(1)
      assertThat(state.items.first().nextUpBook.id).isEqualTo("b2")
    }
  }
}
