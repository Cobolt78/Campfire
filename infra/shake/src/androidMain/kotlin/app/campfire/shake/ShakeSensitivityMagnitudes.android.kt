// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.shake

actual object ShakeSensitivityMagnitudes {
  actual val gravity: Double = 9.80665
  // Linear acceleration deltas from gravity (m/s^2)
  actual val veryLow: Double = 10.0  // Requires hard, sustained shake
  actual val low: Double = 7.0       // Firm shake
  actual val medium: Double = 4.5    // Balanced default shake
  actual val high: Double = 2.5      // Light wrist flick / gentle shake
  actual val veryHigh: Double = 1.0  // Slight movement or nudge
}
