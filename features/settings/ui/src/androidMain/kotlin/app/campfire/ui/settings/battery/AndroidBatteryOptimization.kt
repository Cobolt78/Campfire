// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.battery

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@ContributesBinding(AppScope::class)
@Inject
class AndroidBatteryOptimization(
  private val application: Application,
  appLifecycleObserver: AppLifecycleObserver,
  @ForScope(AppScope::class) scope: CoroutineScope,
) : BatteryOptimization {

  override val isSupported: Boolean = true

  private val ignoringOptimizations = MutableStateFlow(isIgnoringBatteryOptimizations())

  init {
    scope.launch {
      appLifecycleObserver.state
        .filter { it == AppLifecycleState.Foreground }
        .collect {
          ignoringOptimizations.value = isIgnoringBatteryOptimizations()
        }
    }
  }

  override fun isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = application.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(application.packageName) == true
  }

  override fun observeIgnoringBatteryOptimizations(): Flow<Boolean> = ignoringOptimizations.asStateFlow()

  override fun openSettings() {
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", application.packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      application.startActivity(detailsIntent)
    } catch (_: ActivityNotFoundException) {
      try {
        val batteryIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        application.startActivity(batteryIntent)
      } catch (_: Exception) {}
    }
  }
}
