// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.shake

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class SamplingShakeDetectorTest {

  private fun feedLinearMotion(
    detector: SamplingShakeDetector,
    deltas: List<Double>,
    startNs: Long = 1_000_000_000L,
    stepNs: Long = 30_000_000L,
  ) {
    deltas.forEachIndexed { index, delta ->
      detector.addAccelerometerEvent(
        AccelerometerEvent(
          x = 0.0,
          y = 0.0,
          z = ShakeSensitivityMagnitudes.gravity + delta,
          timestamp = startNs + index * stepNs,
        ),
      )
    }
  }

  @Test
  fun veryHigh_triggersOnGentlePickUpMotion_whereMediumAndVeryLowDoNot() {
    var veryHighShakes = 0
    val veryHighDetector = SamplingShakeDetector { veryHighShakes++ }.apply {
      sensitivity = ShakeSensitivity.VeryHigh
    }

    var mediumShakes = 0
    val mediumDetector = SamplingShakeDetector { mediumShakes++ }.apply {
      sensitivity = ShakeSensitivity.Medium
    }

    var veryLowShakes = 0
    val veryLowDetector = SamplingShakeDetector { veryLowShakes++ }.apply {
      sensitivity = ShakeSensitivity.VeryLow
    }

    // Gentle movement: 1.5 m/s^2 above gravity (~0.15g) across 3 samples
    val gentleDeltas = listOf(1.5, 1.5, 1.5)
    feedLinearMotion(veryHighDetector, gentleDeltas)
    feedLinearMotion(mediumDetector, gentleDeltas)
    feedLinearMotion(veryLowDetector, gentleDeltas)

    assertThat(veryHighShakes).isEqualTo(1)
    assertThat(mediumShakes).isEqualTo(0)
    assertThat(veryLowShakes).isEqualTo(0)
  }

  @Test
  fun veryHigh_triggersOnNegativeGravityPhase() {
    var shakes = 0
    val detector = SamplingShakeDetector { shakes++ }.apply {
      sensitivity = ShakeSensitivity.VeryHigh
    }

    // Accelerating downward / decreasing Z magnitude by 1.5 m/s^2 below gravity
    feedLinearMotion(detector, listOf(-1.5, -1.5, -1.5))

    assertThat(shakes).isEqualTo(1)
  }

  @Test
  fun veryLow_ignoresMildMotion_andRequiresSustainedFirmShaking() {
    var shakes = 0
    val detector = SamplingShakeDetector { shakes++ }.apply {
      sensitivity = ShakeSensitivity.VeryLow
    }

    // Mild motion below VeryLow threshold (10.0 m/s^2) over 420ms
    feedLinearMotion(detector, List(15) { 6.0 })
    assertThat(shakes).isEqualTo(0)

    // Firm sustained shake (>= 12 samples over >= 300ms above 10.0 m/s^2 delta)
    feedLinearMotion(
      detector = detector,
      deltas = List(13) { 11.0 },
      startNs = 3_000_000_000L,
    )
    assertThat(shakes).isEqualTo(1)
  }

  @Test
  fun cooldownPreventsDoubleFiringWithinHalfSecond() {
    var shakes = 0
    val detector = SamplingShakeDetector { shakes++ }.apply {
      sensitivity = ShakeSensitivity.VeryHigh
    }

    // First shake completes at 1.06s
    feedLinearMotion(
      detector = detector,
      deltas = List(12) { 2.0 },
      startNs = 1_000_000_000L,
      stepNs = 30_000_000L,
    )
    assertThat(shakes).isEqualTo(1)

    // Subsequent movement at 2.0s triggers a second shake
    feedLinearMotion(
      detector = detector,
      deltas = listOf(2.0, 2.0, 2.0),
      startNs = 2_000_000_000L,
      stepNs = 30_000_000L,
    )
    assertThat(shakes).isEqualTo(2)
  }

  @Test
  fun changingSensitivityResetsBufferedSamples() {
    var shakes = 0
    val detector = SamplingShakeDetector { shakes++ }.apply {
      sensitivity = ShakeSensitivity.VeryHigh
    }

    // Two samples (one short of VeryHigh's 3-sample threshold)
    feedLinearMotion(detector, listOf(2.0, 2.0), startNs = 1_000_000_000L)
    assertThat(shakes).isEqualTo(0)

    // Switch to Medium (requires 7 samples & 200ms) and send one weak sample
    detector.sensitivity = ShakeSensitivity.Medium
    feedLinearMotion(detector, listOf(5.0), startNs = 1_100_000_000L)
    assertThat(shakes).isEqualTo(0)
  }
}
