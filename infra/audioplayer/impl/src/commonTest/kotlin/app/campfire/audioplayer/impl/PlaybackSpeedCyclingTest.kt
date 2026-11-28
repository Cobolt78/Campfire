// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class PlaybackSpeedCyclingTest {

  private val defaultRates = listOf(1.0f, 1.1f, 1.25f, 1.5f, 2.0f)

  @Test
  fun `cycles through sorted presets and wraps to start`() {
    assertThat(cycleNextPlaybackSpeed(defaultRates, 1.0f)).isEqualTo(1.1f)
    assertThat(cycleNextPlaybackSpeed(defaultRates, 1.25f)).isEqualTo(1.5f)
    assertThat(cycleNextPlaybackSpeed(defaultRates, 2.0f)).isEqualTo(1.0f)
  }

  @Test
  fun `advances from off-preset fine speed to next higher preset`() {
    assertThat(cycleNextPlaybackSpeed(defaultRates, 1.23f)).isEqualTo(1.25f)
    assertThat(cycleNextPlaybackSpeed(defaultRates, 0.85f)).isEqualTo(1.0f)
    assertThat(cycleNextPlaybackSpeed(defaultRates, 2.5f)).isEqualTo(1.0f)
  }

  @Test
  fun `returns null when presets list is empty`() {
    assertThat(cycleNextPlaybackSpeed(emptyList(), 1.25f)).isNull()
  }
}
