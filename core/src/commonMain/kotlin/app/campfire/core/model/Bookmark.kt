// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

data class Bookmark(
  val userId: UserId,
  val libraryItemId: LibraryItemId,
  val title: String,
  val time: Duration,
  val createdAt: LocalDateTime,
)

infix fun Bookmark.isSame(other: Bookmark): Boolean {
  return userId == other.userId &&
    libraryItemId == other.libraryItemId &&
    time == other.time
}

infix fun Bookmark.isContentDifferent(other: Bookmark): Boolean {
  return isSame(other) && title != other.title
}

/**
 * Resolves the default bookmark title for [timestampFormatted], preferring the chapter
 * name when available (sanitized to a single line so it never collides with `Title\nNotes`).
 */
fun defaultBookmarkTitle(
  chapterTitle: String?,
  formattedTimestamp: String,
): String {
  val cleanChapter = chapterTitle?.replace('\n', ' ')?.replace('\r', ' ')?.trim()
  return if (!cleanChapter.isNullOrBlank()) {
    cleanChapter
  } else {
    "Bookmark @ $formattedTimestamp"
  }
}
