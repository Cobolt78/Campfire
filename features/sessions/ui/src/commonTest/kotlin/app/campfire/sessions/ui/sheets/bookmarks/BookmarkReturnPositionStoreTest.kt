// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.bookmarks

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BookmarkReturnPositionStoreTest {

  @Test
  fun `records furthest jump and preserves high-water mark across bookmark hops`() {
    val store = BookmarkReturnPositionStore()
    val bookId = "book-1"

    store.recordJumpFrom(bookId, fromTime = 3.hours + 45.minutes, toTime = 12.minutes + 30.seconds)
    assertThat(store.current(bookId)).isEqualTo(3.hours + 45.minutes)

    // Hopping between earlier bookmarks must NOT overwrite the furthest position
    store.recordJumpFrom(bookId, fromTime = 15.minutes, toTime = 45.minutes)
    assertThat(store.current(bookId)).isEqualTo(3.hours + 45.minutes)

    // Listening further later and jumping again updates to the new high-water mark
    store.recordJumpFrom(bookId, fromTime = 4.hours + 10.minutes, toTime = 20.minutes)
    assertThat(store.current(bookId)).isEqualTo(4.hours + 10.minutes)
  }

  @Test
  fun `ignores trivial jumps under 15s and zero start time`() {
    val store = BookmarkReturnPositionStore()
    val bookId = "book-2"

    store.recordJumpFrom(bookId, fromTime = 0.seconds, toTime = 30.minutes)
    assertThat(store.current(bookId)).isNull()

    store.recordJumpFrom(bookId, fromTime = 1.hours, toTime = 59.minutes + 50.seconds)
    assertThat(store.current(bookId)).isNull()
  }

  @Test
  fun `isolates return positions per book and clears individually`() {
    val store = BookmarkReturnPositionStore()

    store.recordJumpFrom("book-a", fromTime = 2.hours, toTime = 5.minutes)
    store.recordJumpFrom("book-b", fromTime = 30.minutes, toTime = 1.minutes)

    assertThat(store.current("book-a")).isEqualTo(2.hours)
    assertThat(store.current("book-b")).isEqualTo(30.minutes)

    store.clear("book-a")
    assertThat(store.current("book-a")).isNull()
    assertThat(store.current("book-b")).isEqualTo(30.minutes)
  }
}
