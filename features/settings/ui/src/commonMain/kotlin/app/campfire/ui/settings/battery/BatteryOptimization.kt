// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.battery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface BatteryOptimization {
  val isSupported: Boolean get() = false
  fun isIgnoringBatteryOptimizations(): Boolean = false
  fun observeIgnoringBatteryOptimizations(): Flow<Boolean> = flowOf(isIgnoringBatteryOptimizations())
  fun openSettings()
}
