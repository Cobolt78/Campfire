// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.shake

actual object ShakeSensitivityMagnitudes {
  actual val gravity: Double = 1.0
  // Linear acceleration deltas from 1g gravity
  actual val veryLow: Double = 1.02
  actual val low: Double = 0.71
  actual val medium: Double = 0.46
  actual val high: Double = 0.25
  actual val veryHigh: Double = 0.10
}
