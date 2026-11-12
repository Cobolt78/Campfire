// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.battery

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class NoOpBatteryOptimization : BatteryOptimization {
  override val isSupported: Boolean = false
  override fun isIgnoringBatteryOptimizations(): Boolean = false
  override fun openSettings() {}
}
