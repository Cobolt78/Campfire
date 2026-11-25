// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class BookmarkDefaultTitleTest {

  @Test
  fun usesSanitizedChapterTitleWhenPresent() {
    assertEquals(
      "Chapter 4: Arrival",
      defaultBookmarkTitle("Chapter 4: Arrival", "1h 12m 5s"),
    )
    assertEquals(
      "Part 1 Prologue",
      defaultBookmarkTitle("  Part 1\nPrologue\r ", "12m 34s"),
    )
  }

  @Test
  fun fallsBackToFormattedTimestampWhenChapterMissingOrBlank() {
    assertEquals(
      "Bookmark @ 1h 12m 5s",
      defaultBookmarkTitle(null, "1h 12m 5s"),
    )
    assertEquals(
      "Bookmark @ 5m 10s",
      defaultBookmarkTitle("   \n ", "5m 10s"),
    )
  }
}
