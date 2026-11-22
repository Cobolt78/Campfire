// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.db.LocalDateTimeAdapter
import app.campfire.network.models.AudioBookmark
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class BookmarkMappingTest {

  @Test
  fun epochMillisecondsCreatedAtMapsToModernYear() {
    val network = AudioBookmark(
      libraryItemId = "li_inn_side_shop",
      title = "Bookmark @ 12:34",
      time = 754f,
      createdAt = 1_668_120_083_771L,
    )

    val domain = network.asDomainModel("user-1")
    val dbModel = network.asDbModel("user-1")

    assertThat(domain.createdAt.year).isEqualTo(2022)
    assertThat(dbModel.createdAt.year).isEqualTo(2022)
    assertThat(LocalDateTimeAdapter.decode(LocalDateTimeAdapter.encode(dbModel.createdAt)))
      .isEqualTo(dbModel.createdAt)
  }

  @Test
  fun epochSecondsCreatedAtAlsoMapsToModernYear() {
    val network = AudioBookmark(
      libraryItemId = "li_inn_side_shop",
      title = "Bookmark @ 12:34",
      time = 754f,
      createdAt = 1_668_120_083L,
    )

    val domain = network.asDomainModel("user-1")
    val dbModel = network.asDbModel("user-1")

    assertThat(domain.createdAt.year).isEqualTo(2022)
    assertThat(dbModel.createdAt.year).isEqualTo(2022)
  }
}
