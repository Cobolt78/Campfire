// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

/**
 * Return the progress as a [Float] in range of (0f..1f) over [other] duration.
 *
 * @receiver the numerator duration to calculate
 * @param other the denominator duration to calculate
 * @return the progress as a [Float] in range of (0f..1f)
 */
infix fun Duration.progressOver(other: Duration): Float {
  if (other == 0.milliseconds) return 0f
  return div(other).toFloat()
}

/**
 * Return the duration as a [Float] in seconds.
 */
fun Duration.asSeconds(): Float = toDouble(DurationUnit.SECONDS).toFloat()

infix fun ClosedRange<Duration>.isIn(other: ClosedRange<Duration>): Boolean {
  return start >= other.start && endInclusive <= other.endInclusive
}

/**
 * Format duration as human-readable hours and minutes (e.g. "14h 22m", "45m", "1h").
 */
fun Duration.formatHoursAndMinutes(): String {
  val hours = inWholeHours
  val minutes = inWholeMinutes % 60
  return buildString {
    if (hours > 0) {
      append("${hours}h ")
    }
    if (minutes > 0 || hours == 0L) {
      append("${minutes}m")
    }
  }.trim()
}
