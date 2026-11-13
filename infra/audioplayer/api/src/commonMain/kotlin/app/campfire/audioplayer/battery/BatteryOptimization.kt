// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.battery

import kotlinx.coroutines.flow.Flow

interface BatteryOptimization {
  val isSupported: Boolean
  fun isIgnoringBatteryOptimizations(): Boolean
  fun observeIgnoringBatteryOptimizations(): Flow<Boolean>
  fun requestIgnoreBatteryOptimizations()
  fun openSettings()
  fun refresh()
}
