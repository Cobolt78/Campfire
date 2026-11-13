// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.battery

import app.campfire.audioplayer.battery.BatteryOptimization
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@ContributesBinding(AppScope::class)
@Inject
class JvmBatteryOptimization : BatteryOptimization {
  override val isSupported: Boolean = false
  override fun isIgnoringBatteryOptimizations(): Boolean = true
  override fun observeIgnoringBatteryOptimizations(): Flow<Boolean> = emptyFlow()
  override fun requestIgnoreBatteryOptimizations() {}
  override fun openSettings() {}
}
