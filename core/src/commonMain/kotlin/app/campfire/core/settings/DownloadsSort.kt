// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.settings

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress

enum class DownloadsSort(
  val storageKey: String,
  val label: String,
  val defaultAscending: Boolean,
) {
  Custom("Custom", "Custom Order", defaultAscending = true),
  CurrentlyListening("CurrentlyListening", "Currently Listening", defaultAscending = true),
  RecentlyPlayed("RecentlyPlayed", "Recently Played", defaultAscending = false),
  Progress("Progress", "Progress", defaultAscending = false),
  RemainingTime("RemainingTime", "Time Remaining", defaultAscending = true),
  Series("Series", "Series", defaultAscending = true),
  DateAdded("DateAdded", "Date Added", defaultAscending = false),
  Title("Title", "Title A–Z", defaultAscending = true),
  Author("Author", "Author A–Z", defaultAscending = true),
  Duration("Duration", "Length", defaultAscending = false),
  PublishYear("PublishYear", "Date Published", defaultAscending = false),
  Size("Size", "File Size", defaultAscending = false),
  ;

  companion object {
    fun fromStorageKey(key: String?): DownloadsSort {
      return entries.find { it.storageKey == key } ?: RecentlyPlayed
    }
  }
}

/**
 * Sorts downloaded library items identically for both the Downloads screen and the Home screen Downloads shelf.
 */
fun <T> sortDownloadedItems(
  items: List<T>,
  sortMode: DownloadsSort,
  sortAscending: Boolean,
  customOrder: List<LibraryItemId>,
  libraryItem: (T) -> LibraryItem,
  progress: (T) -> MediaProgress?,
  downloadUpdateTimeMs: (T) -> Long,
  downloadContentLength: (T) -> Long,
): List<T> {
  if (items.size <= 1) return items

  fun titleOf(entry: T): String = libraryItem(entry).media.metadata.title ?: ""

  val comparator: Comparator<T> = when (sortMode) {
    DownloadsSort.Custom -> {
      val orderMap = customOrder.withIndex().associate { (index, id) -> id to index }
      compareBy<T> { orderMap[libraryItem(it).id] ?: Int.MAX_VALUE }
        .thenByDescending { progress(it)?.lastUpdate ?: 0L }
        .thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.CurrentlyListening -> {
      compareBy<T> { entry ->
        val prog = progress(entry)
        when {
          prog != null && (prog.isFinished || prog.isCompleted) -> 2
          prog != null && (prog.progress > 0f || prog.currentTime > 0f) -> 0
          else -> 1
        }
      }.thenByDescending { progress(it)?.lastUpdate ?: 0L }
        .thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.RecentlyPlayed -> {
      compareBy<T> { progress(it)?.lastUpdate ?: 0L }
        .thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.Progress -> {
      compareBy<T> { progress(it)?.actualProgress ?: 0f }
        .thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.RemainingTime -> {
      compareBy<T> { entry ->
        val item = libraryItem(entry)
        val prog = progress(entry)
        val ratio = (prog?.actualProgress ?: 0f).coerceIn(0f, 1f)
        val durationMs = item.media.durationInMillis.coerceAtLeast(0L)
        (durationMs * (1f - ratio)).toLong()
      }.thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.Series -> {
      compareBy<T, String>(String.CASE_INSENSITIVE_ORDER) {
        libraryItem(it).media.metadata.seriesSequence?.name ?: "\uFFFF"
      }.thenBy {
        libraryItem(it).media.metadata.seriesSequence?.sequence ?: Double.MAX_VALUE
      }.thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.DateAdded -> {
      compareBy<T> { entry ->
        val updateMs = downloadUpdateTimeMs(entry)
        if (updateMs > 0L) updateMs else libraryItem(entry).addedAtMillis
      }.thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.Title -> {
      compareBy<T, String>(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.Author -> {
      compareBy<T, String>(String.CASE_INSENSITIVE_ORDER) {
        libraryItem(it).media.metadata.authorName ?: ""
      }.thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.Duration -> {
      compareBy<T> { libraryItem(it).media.durationInMillis }
        .thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.PublishYear -> {
      compareBy<T> { libraryItem(it).media.metadata.publishedYear?.toIntOrNull() ?: 0 }
        .thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }

    DownloadsSort.Size -> {
      compareBy<T> { downloadContentLength(it).coerceAtLeast(0L) }
        .thenBy(String.CASE_INSENSITIVE_ORDER, ::titleOf)
    }
  }

  return if (sortAscending) {
    items.sortedWith(comparator)
  } else {
    items.sortedWith(comparator.reversed())
  }
}
