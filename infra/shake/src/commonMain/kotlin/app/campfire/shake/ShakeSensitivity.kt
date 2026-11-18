// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.shake

enum class ShakeSensitivity(
  val value: Double,
  val minAcceleratingSamples: Int,
  val minWindowNs: Long,
  val minAcceleratingFraction: Double,
) {
  VeryLow(
    value = ShakeSensitivityMagnitudes.veryLow,
    minAcceleratingSamples = 12,
    minWindowNs = 300_000_000L,
    minAcceleratingFraction = 0.75,
  ),
  Low(
    value = ShakeSensitivityMagnitudes.low,
    minAcceleratingSamples = 9,
    minWindowNs = 250_000_000L,
    minAcceleratingFraction = 0.65,
  ),
  Medium(
    value = ShakeSensitivityMagnitudes.medium,
    minAcceleratingSamples = 7,
    minWindowNs = 200_000_000L,
    minAcceleratingFraction = 0.55,
  ),
  High(
    value = ShakeSensitivityMagnitudes.high,
    minAcceleratingSamples = 5,
    minWindowNs = 120_000_000L,
    minAcceleratingFraction = 0.45,
  ),
  VeryHigh(
    value = ShakeSensitivityMagnitudes.veryHigh,
    minAcceleratingSamples = 3,
    minWindowNs = 0L,
    minAcceleratingFraction = 0.0,
  ),
  ;

  val valueSquared: Double get() = value * value

  companion object {
    val Default = Medium
  }
}

expect object ShakeSensitivityMagnitudes {
  val gravity: Double
  val veryLow: Double
  val low: Double
  val medium: Double
  val high: Double
  val veryHigh: Double
}
