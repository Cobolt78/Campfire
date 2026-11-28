// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.bookmarks

import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Holds an ephemeral per-book high-water-mark return position so tapping an older bookmark
 * (or previewing one in Edit mode) never leaves the user stranded away from their furthest
 * listening spot.
 */
@SingleIn(UserScope::class)
@Inject
class BookmarkReturnPositionStore() {

  private val positions = MutableStateFlow<Map<LibraryItemId, Duration>>(emptyMap())

  fun observe(libraryItemId: LibraryItemId): Flow<Duration?> {
    return positions.map { it[libraryItemId] }.distinctUntilChanged()
  }

  fun current(libraryItemId: LibraryItemId): Duration? = positions.value[libraryItemId]

  /**
   * Records [fromTime] as the return position for [libraryItemId] when the jump to [toTime] is at
   * least [MIN_JUMP_THRESHOLD] and [fromTime] exceeds any previously saved return position, so
   * hopping between earlier bookmarks preserves the furthest listening point.
   */
  fun recordJumpFrom(
    libraryItemId: LibraryItemId,
    fromTime: Duration,
    toTime: Duration,
  ) {
    if (fromTime <= Duration.ZERO) return
    if ((fromTime - toTime).absoluteValue < MIN_JUMP_THRESHOLD) return
    positions.update { currentMap ->
      val existing = currentMap[libraryItemId]
      if (existing != null && existing >= fromTime) {
        currentMap
      } else {
        currentMap + (libraryItemId to fromTime)
      }
    }
  }

  fun clear(libraryItemId: LibraryItemId) {
    positions.update { it - libraryItemId }
  }

  companion object {
    val MIN_JUMP_THRESHOLD: Duration = 15.seconds
  }
}
