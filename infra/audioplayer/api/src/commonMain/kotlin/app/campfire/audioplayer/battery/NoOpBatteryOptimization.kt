// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.battery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NoOpBatteryOptimization(
  override val isSupported: Boolean = false,
  isIgnoring: Boolean = true,
) : BatteryOptimization {

  private val ignoringFlow = MutableStateFlow(isIgnoring)

  override fun isIgnoringBatteryOptimizations(): Boolean = ignoringFlow.value

  override fun observeIgnoringBatteryOptimizations(): Flow<Boolean> = ignoringFlow.asStateFlow()

  fun setIgnoringBatteryOptimizations(value: Boolean) {
    ignoringFlow.value = value
  }

  override fun requestIgnoreBatteryOptimizations() {}

  override fun openSettings() {}
}
