// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.battery

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import app.campfire.audioplayer.battery.BatteryOptimization
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
  @ForScope(AppScope::class) private val scope: CoroutineScope,
) : BatteryOptimization {

  override val isSupported: Boolean = true

  private val ignoringOptimizations = MutableStateFlow(isIgnoringBatteryOptimizations())
  private var pollingJob: Job? = null

  init {
    scope.launch {
      appLifecycleObserver.state
        .filter { it == AppLifecycleState.Foreground }
        .collect {
          refresh()
        }
    }

    application.registerActivityLifecycleCallbacks(
      object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
          refresh()
        }

        override fun onActivityStarted(activity: Activity) {
          refresh()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
      }
    )

    try {
      val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
          refresh()
        }
      }
      val filter = IntentFilter("android.os.action.POWER_SAVE_WHITELIST_CHANGED")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        application.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
      } else {
        application.registerReceiver(receiver, filter)
      }
    } catch (_: Exception) {
      // Ignore if receiver registration fails on restricted environments
    }
  }

  override fun refresh() {
    val current = isIgnoringBatteryOptimizations()
    if (ignoringOptimizations.value != current) {
      ignoringOptimizations.value = current
    }
  }

  private fun startPolling() {
    pollingJob?.cancel()
    pollingJob = scope.launch {
      val start = System.currentTimeMillis()
      while (System.currentTimeMillis() - start < 30_000) {
        val current = isIgnoringBatteryOptimizations()
        if (ignoringOptimizations.value != current) {
          ignoringOptimizations.value = current
          if (current) break
        }
        delay(200)
      }
    }
  }

  override fun isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = application.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(application.packageName) == true
  }

  override fun observeIgnoringBatteryOptimizations(): Flow<Boolean> = ignoringOptimizations.asStateFlow()

  override fun requestIgnoreBatteryOptimizations() {
    startPolling()
    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
      data = Uri.parse("package:${application.packageName}")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      application.startActivity(requestIntent)
    } catch (_: Exception) {
      openSettings()
    }
  }

  override fun openSettings() {
    startPolling()
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", application.packageName, null)
      putExtra(":settings:fragment_args_key", "battery")
      val args = Bundle().apply {
        putString(":settings:fragment_args_key", "battery")
      }
      putExtra(":settings:show_fragment_args", args)
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
