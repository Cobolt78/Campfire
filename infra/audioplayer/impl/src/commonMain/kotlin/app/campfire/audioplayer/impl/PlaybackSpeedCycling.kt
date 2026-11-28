// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import kotlin.math.abs

/**
 * Returns the next preset speed after [currentSpeed] from [rates]. If [currentSpeed] matches a
 * preset (within 0.005x), advances cyclically; otherwise jumps to the next higher preset or wraps
 * around to the lowest preset.
 */
internal fun cycleNextPlaybackSpeed(
  rates: List<Float>,
  currentSpeed: Float,
): Float? {
  val sorted = rates.distinct().sorted()
  if (sorted.isEmpty()) return null
  val exactIndex = sorted.indexOfFirst { abs(it - currentSpeed) < 0.005f }
  return if (exactIndex >= 0) {
    sorted[(exactIndex + 1) % sorted.size]
  } else {
    sorted.firstOrNull { it > currentSpeed } ?: sorted.first()
  }
}
