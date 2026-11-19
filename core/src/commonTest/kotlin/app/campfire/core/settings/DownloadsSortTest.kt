// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.settings

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.SeriesSequence
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.model.preview.mediaProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class DownloadsSortTest {

  private data class Entry(
    val item: LibraryItem,
    val prog: MediaProgress? = null,
    val updateMs: Long = 0L,
    val sizeBytes: Long = 0L,
  )

  private fun titledBook(
    id: String,
    title: String,
    durationMinutes: Int = 60,
    series: SeriesSequence? = null,
  ): LibraryItem {
    val base = libraryItem(
      id = id,
      duration = durationMinutes.minutes,
      seriesSequence = series,
    )
    val book = base.media as Media.Book
    return base.copy(
      media = book.copy(
        metadata = book.metadata.copy(title = title),
      ),
    )
  }

  private fun sort(
    entries: List<Entry>,
    mode: DownloadsSort,
    ascending: Boolean = mode.defaultAscending,
    customOrder: List<String> = emptyList(),
  ): List<String> {
    return sortDownloadedItems(
      items = entries,
      sortMode = mode,
      sortAscending = ascending,
      customOrder = customOrder,
      libraryItem = { it.item },
      progress = { it.prog },
      downloadUpdateTimeMs = { it.updateMs },
      downloadContentLength = { it.sizeBytes },
    ).map { it.item.id }
  }

  @Test
  fun fromStorageKeyFallsBackToRecentlyPlayed() {
    assertEquals(DownloadsSort.RecentlyPlayed, DownloadsSort.fromStorageKey(null))
    assertEquals(DownloadsSort.RecentlyPlayed, DownloadsSort.fromStorageKey(""))
    assertEquals(DownloadsSort.RecentlyPlayed, DownloadsSort.fromStorageKey("UnknownMode"))
    assertEquals(DownloadsSort.CurrentlyListening, DownloadsSort.fromStorageKey("CurrentlyListening"))
  }

  @Test
  fun currentlyListeningOrdersInProgressThenUnstartedThenFinished() {
    val finished = Entry(
      titledBook("fin", "Finished"),
      prog = mediaProgress(progress = 1f, isFinished = true).copy(lastUpdate = 5000L),
    )
    val unstarted = Entry(
      titledBook("new", "Unstarted"),
      prog = null,
    )
    val activeOld = Entry(
      titledBook("act_old", "Active Older"),
      prog = mediaProgress(progress = 0.25f).copy(lastUpdate = 1000L),
    )
    val activeNew = Entry(
      titledBook("act_new", "Active Newer"),
      prog = mediaProgress(progress = 0.50f).copy(lastUpdate = 3000L),
    )

    val sorted = sort(listOf(unstarted, finished, activeOld, activeNew), DownloadsSort.CurrentlyListening)
    assertEquals(listOf("act_new", "act_old", "new", "fin"), sorted)
  }

  @Test
  fun progressSortsHighestProgressFirstByDefault() {
    val low = Entry(titledBook("low", "Low"), prog = mediaProgress(progress = 0.10f))
    val high = Entry(titledBook("high", "High"), prog = mediaProgress(progress = 0.85f))
    val done = Entry(titledBook("done", "Done"), prog = mediaProgress(progress = 1.0f, isFinished = true))

    assertEquals(listOf("done", "high", "low"), sort(listOf(low, high, done), DownloadsSort.Progress))
  }

  @Test
  fun remainingTimeSortsShortestRemainingFirstByDefault() {
    val longLeft = Entry(titledBook("long", "Long", durationMinutes = 100), prog = mediaProgress(progress = 0.2f, duration = 100.minutes)) // 80m left
    val shortLeft = Entry(titledBook("short", "Short", durationMinutes = 100), prog = mediaProgress(progress = 0.9f, duration = 100.minutes)) // 10m left
    val midLeft = Entry(titledBook("mid", "Mid", durationMinutes = 100), prog = mediaProgress(progress = 0.5f, duration = 100.minutes)) // 50m left

    assertEquals(listOf("short", "mid", "long"), sort(listOf(longLeft, midLeft, shortLeft), DownloadsSort.RemainingTime))
  }

  @Test
  fun seriesSortsBySeriesNameThenSequenceWithStandaloneLast() {
    val standAlone = Entry(titledBook("solo", "A Standalone", series = null))
    val s2b2 = Entry(titledBook("b2", "Book 2", series = SeriesSequence("s1", "Alpha Series", 2.0)))
    val s2b1_5 = Entry(titledBook("b1_5", "Book 1.5", series = SeriesSequence("s1", "Alpha Series", 1.5)))
    val s1b1 = Entry(titledBook("z1", "Book 1", series = SeriesSequence("s2", "Beta Series", 1.0)))

    assertEquals(listOf("b1_5", "b2", "z1", "solo"), sort(listOf(standAlone, s1b1, s2b2, s2b1_5), DownloadsSort.Series))
  }

  @Test
  fun customOrderPlacesKnownIdsFirstAndAppendsNewItems() {
    val a = Entry(titledBook("a", "Alpha"), prog = mediaProgress(progress = 0.1f).copy(lastUpdate = 100L))
    val b = Entry(titledBook("b", "Beta"))
    val c = Entry(titledBook("c", "Gamma"), prog = mediaProgress(progress = 0.2f).copy(lastUpdate = 500L))
    val d = Entry(titledBook("d", "Delta"), prog = mediaProgress(progress = 0.3f).copy(lastUpdate = 200L))

    val sorted = sort(
      entries = listOf(a, b, c, d),
      mode = DownloadsSort.Custom,
      customOrder = listOf("b", "a"),
    )
    assertEquals(listOf("b", "a", "c", "d"), sorted)
  }
}
