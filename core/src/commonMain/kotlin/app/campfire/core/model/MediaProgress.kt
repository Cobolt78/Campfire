// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import app.campfire.core.extensions.seconds
import kotlin.time.Duration

typealias MediaProgressId = String

data class MediaProgress(
  val id: MediaProgressId,
  val userId: String,
  val libraryItemId: String,
  val episodeId: String? = null,
  val mediaItemId: String,
  val mediaItemType: MediaType,
  val duration: Float?,
  val progress: Float,
  val currentTime: Float,
  val isFinished: Boolean,
  val hideFromContinueListening: Boolean,
  val ebookLocation: String? = null,
  val ebookProgress: Float? = null,
  val lastUpdate: Long,
  val startedAt: Long,
  val finishedAt: Long? = null,
  val source: Source,
) {

  /**
   * Describes where this media progress was written from
   */
  enum class Source {
    /**
     * The media progress was created locally
     */
    Local,

    /**
     * The media progress was written from the server
     */
    Remote,
  }

  val isValid: Boolean
    get() = progress > 0f || (duration ?: 0f) > 0f

  /**
   * True if this progress represents a completed item: either marked finished,
   * progress >= 1.0f, currentTime reached within 2 seconds of duration, or finishedAt is set.
   */
  val isCompleted: Boolean
    get() = isFinished ||
      progress >= 0.99f ||
      (finishedAt != null && (finishedAt ?: 0L) > 0L) ||
      ((duration ?: 0f) > 0f && currentTime >= ((duration ?: 0f) - 2f))

  /**
   * Get the [currentTime] in [Duration] units, accounting for [isFinished] or [isCompleted],
   * where if true, it returns 0 duration.
   */
  val actualTime: Duration
    get() = if (isFinished || isCompleted) Duration.ZERO else currentTime.seconds

  val actualProgress: Float
    get() = if (isFinished || isCompleted) {
      1f
    } else duration?.let {
      currentTime / duration
    } ?: progress

  companion object {
    const val UNKNOWN_ID = "unknown_id"
  }
}
