// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.speed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.PlaybackSpeedRange
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PlaybackSpeedSheetTest {

  @Test
  fun `snapToSpeedStep snaps values to nearest 0_05 increment within bounds`() {
    assertThat(1.22f.snapToSpeedStep()).isEqualTo(1.20f)
    assertThat(1.23f.snapToSpeedStep()).isEqualTo(1.25f)
    assertThat(1.27f.snapToSpeedStep()).isEqualTo(1.25f)
    assertThat(1.28f.snapToSpeedStep()).isEqualTo(1.30f)
    assertThat(0.2f.snapToSpeedStep()).isEqualTo(0.50f)
    assertThat(3.4f.snapToSpeedStep()).isEqualTo(3.00f)
  }

  @Test
  fun `stepSpeed snaps off-grid values to nearest 0_05 in requested direction`() {
    assertThat(stepSpeed(1.00f, +1)).isEqualTo(1.05f)
    assertThat(stepSpeed(1.05f, +1)).isEqualTo(1.10f)
    assertThat(stepSpeed(1.25f, -1)).isEqualTo(1.20f)
    assertThat(stepSpeed(1.20f, -1)).isEqualTo(1.15f)
    // Off-grid values snap cleanly to the next/previous 0.05 grid mark
    assertThat(stepSpeed(1.23f, +1)).isEqualTo(1.25f)
    assertThat(stepSpeed(1.23f, -1)).isEqualTo(1.20f)
    assertThat(stepSpeed(1.21f, +1)).isEqualTo(1.25f)
    assertThat(stepSpeed(1.21f, -1)).isEqualTo(1.20f)
    // Clamped at min/max
    assertThat(stepSpeed(0.50f, -1)).isEqualTo(0.50f)
    assertThat(stepSpeed(3.00f, +1)).isEqualTo(3.00f)
  }

  @Test
  fun `stepSpeedFine advances by exact 0_01 increments and clamps within bounds`() {
    assertThat(stepSpeedFine(1.25f, +1)).isEqualTo(1.26f)
    assertThat(stepSpeedFine(1.25f, -1)).isEqualTo(1.24f)
    assertThat(stepSpeedFine(1.29f, +1)).isEqualTo(1.30f)
    assertThat(stepSpeedFine(0.50f, -1)).isEqualTo(0.50f)
    assertThat(stepSpeedFine(3.00f, +1)).isEqualTo(3.00f)
  }

  @Test
  fun `concept A dual-stage capsules step by 0_05x and 0_01x and snap off-grid values`() = runComposeUiTest {
    mainClock.autoAdvance = false
    val speedChanges = mutableListOf<Float>()
    var currentSpeed by mutableStateOf(1.25f)

    setContent {
      CampfireTheme {
        PlaybackSpeedSheet(
          currentSpeed = currentSpeed,
          speedOptions = listOf(1f, 1.25f, 1.5f),
          perBookSpeedEnabled = false,
          useAlternateStepperView = false,
          onSpeedChange = {
            currentSpeed = it
            speedChanges += it
          },
          onPerBookSpeedChange = {},
        )
      }
    }
    mainClock.advanceTimeByFrame()

    onNodeWithContentDescription("Increase speed by 0.01x").performClick()
    mainClock.advanceTimeByFrame()
    assertThat(currentSpeed).isEqualTo(1.26f)

    // Stepping +0.05 from 1.26 snaps to next 0.05 mark (1.30)
    onNodeWithContentDescription("Increase speed by 0.05x").performClick()
    mainClock.advanceTimeByFrame()
    assertThat(currentSpeed).isEqualTo(1.30f)

    onNodeWithContentDescription("Decrease speed by 0.01x").performClick()
    mainClock.advanceTimeByFrame()
    assertThat(currentSpeed).isEqualTo(1.29f)

    onNodeWithContentDescription("Decrease speed by 0.05x").performClick()
    mainClock.advanceTimeByFrame()
    assertThat(currentSpeed).isEqualTo(1.25f)

    assertThat(speedChanges).containsExactly(1.26f, 1.30f, 1.29f, 1.25f)

    currentSpeed = 1.23f
    repeat(2) { mainClock.advanceTimeByFrame() }
    onNodeWithContentDescription("Increase speed by 0.05x").performClick()
    mainClock.advanceTimeByFrame()
    assertThat(currentSpeed).isEqualTo(1.25f)
  }

  @Test
  fun `concept B alternate view toggles between 0_05x and 0_01x and respects min max bounds`() = runComposeUiTest {
    mainClock.autoAdvance = false
    val speedChanges = mutableListOf<Float>()
    var currentSpeed by mutableStateOf(1.25f)
    var fineStep by mutableStateOf(false)

    setContent {
      CampfireTheme {
        PlaybackSpeedSheet(
          currentSpeed = currentSpeed,
          speedOptions = listOf(1f, 1.25f, 1.5f),
          perBookSpeedEnabled = false,
          useAlternateStepperView = true,
          fineStepEnabled = fineStep,
          onFineStepEnabledChange = { fineStep = it },
          onSpeedChange = {
            currentSpeed = it
            speedChanges += it
          },
          onPerBookSpeedChange = {},
        )
      }
    }
    mainClock.advanceTimeByFrame()

    onNodeWithContentDescription("Increase speed by 0.05x").performClick()
    mainClock.advanceTimeByFrame()
    assertThat(currentSpeed).isEqualTo(1.30f)

    // Switch step pill to 0.01x
    onNodeWithText("0.01x").performClick()
    mainClock.advanceTimeByFrame()
    assertThat(fineStep).isEqualTo(true)

    onNodeWithContentDescription("Decrease speed by 0.01x").performClick()
    mainClock.advanceTimeByFrame()
    assertThat(currentSpeed).isEqualTo(1.29f)

    // Switch back to 0.05x and verify directional snap
    onNodeWithText("0.05x").performClick()
    mainClock.advanceTimeByFrame()
    onNodeWithContentDescription("Decrease speed by 0.05x").performClick()
    mainClock.advanceTimeByFrame()
    assertThat(currentSpeed).isEqualTo(1.25f)

    assertThat(speedChanges).containsExactly(1.30f, 1.29f, 1.25f)

    currentSpeed = PlaybackSpeedRange.start
    repeat(2) { mainClock.advanceTimeByFrame() }
    onNodeWithContentDescription("Decrease speed by 0.05x").assertIsNotEnabled()
    onNodeWithContentDescription("Increase speed by 0.05x").assertIsEnabled()

    currentSpeed = PlaybackSpeedRange.endInclusive
    repeat(2) { mainClock.advanceTimeByFrame() }
    onNodeWithContentDescription("Increase speed by 0.05x").assertIsNotEnabled()
    onNodeWithContentDescription("Decrease speed by 0.05x").assertIsEnabled()
  }

  @Test
  fun `slider does not commit speed while dragging and commits snapped 0_05x on release`() = runComposeUiTest {
    mainClock.autoAdvance = false
    val committedSpeeds = mutableListOf<Float>()
    var currentSpeed by mutableStateOf(1.0f)

    setContent {
      CampfireTheme {
        PlaybackSpeedSheet(
          currentSpeed = currentSpeed,
          speedOptions = listOf(1f, 1.5f, 2f),
          perBookSpeedEnabled = false,
          onSpeedChange = {
            currentSpeed = it
            committedSpeeds += it
          },
          onPerBookSpeedChange = {},
        )
      }
    }
    mainClock.advanceTimeByFrame()

    val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

    // Press and move right without lifting yet
    slider.performTouchInput {
      down(centerLeft + Offset(width * 0.2f, 0f))
      moveTo(centerLeft + Offset(width * 0.6f, 0f))
    }
    mainClock.advanceTimeByFrame()

    // While finger is still held down, audio playback speed must NOT have been applied yet
    assertThat(committedSpeeds).isEmpty()
    assertThat(currentSpeed).isEqualTo(1.0f)

    // Release finger -> speed commits now to a clean 0.05x step
    slider.performTouchInput {
      up()
    }
    mainClock.advanceTimeByFrame()

    assertThat(committedSpeeds.size).isEqualTo(1)
    assertThat(currentSpeed).isNotEqualTo(1.0f)
    assertThat(currentSpeed.snapToSpeedStep()).isEqualTo(currentSpeed)
  }
}
