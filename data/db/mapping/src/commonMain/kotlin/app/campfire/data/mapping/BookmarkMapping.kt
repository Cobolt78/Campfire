// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.core.extensions.seconds
import app.campfire.core.model.Bookmark
import app.campfire.data.Bookmarks as DbBookmark
import app.campfire.network.models.AudioBookmark as NetworkBookmark
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val EPOCH_MILLIS_THRESHOLD = 100_000_000_000L

private fun epochToInstant(epoch: Long): Instant {
  return if (epoch > EPOCH_MILLIS_THRESHOLD) {
    Instant.fromEpochMilliseconds(epoch)
  } else {
    Instant.fromEpochSeconds(epoch)
  }
}

fun NetworkBookmark.asDbModel(userId: String): DbBookmark {
  return DbBookmark(
    userId = userId,
    libraryItemId = libraryItemId,
    title = title,
    timeInSeconds = time.roundToInt(),
    createdAt = epochToInstant(createdAt).toLocalDateTime(TimeZone.UTC),
  )
}

fun NetworkBookmark.asDomainModel(userId: String): Bookmark {
  return Bookmark(
    userId = userId,
    libraryItemId = libraryItemId,
    title = title,
    time = time.seconds,
    createdAt = epochToInstant(createdAt).toLocalDateTime(TimeZone.UTC),
  )
}

fun DbBookmark.asDomainModel(): Bookmark {
  return Bookmark(
    userId = userId,
    libraryItemId = libraryItemId,
    title = title,
    time = timeInSeconds.seconds,
    createdAt = createdAt,
  )
}

fun Bookmark.asDbModel(): DbBookmark {
  return DbBookmark(
    userId = userId,
    libraryItemId = libraryItemId,
    title = title,
    timeInSeconds = time.inWholeSeconds.toInt(),
    createdAt = createdAt,
  )
}
