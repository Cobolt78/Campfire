// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.shake

actual object ShakeSensitivityMagnitudes {
  actual val veryLow: Double = 17.5  // Requires firm, intentional shake
  actual val low: Double = 15.5
  actual val medium: Double = 13.5   // Balanced default
  actual val high: Double = 12.5
  actual val veryHigh: Double = 11.0 // Light flick triggers it
}
