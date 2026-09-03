// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.shake

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import me.tatarka.inject.annotations.Provides

actual class ShakeDetector(
  context: Context,
) : SeismicShakeDetector.Listener {
  private val sensorManager = context.getSystemService(SensorManager::class.java)
  private val seismicShakeDetector = SeismicShakeDetector(this)

  @Suppress("DEPRECATION")
  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  private var listener: Listener? = null

  actual val isAvailable: Boolean
    get() = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

  actual val isRunning: Boolean
    get() = seismicShakeDetector.isRunning

  actual fun start(sensitivity: ShakeSensitivity, listener: Listener) {
    this.listener = listener
    seismicShakeDetector.setSensitivity(sensitivity)
    seismicShakeDetector.start(sensorManager, SensorManager.SENSOR_DELAY_GAME)
  }

  actual fun stop() {
    listener = null
    seismicShakeDetector.stop()
  }

  actual fun interface Listener {

    actual fun onShake()
  }

  override fun hearShake() {
    performHapticFeedback()
    listener?.onShake()
  }

  private fun performHapticFeedback() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(100L)
      }
    } catch (_: Throwable) {
      // Ignore vibration failures if permission or hardware is unavailable
    }
  }
}

actual interface ShakeDetectorPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideAndroidShakeDetector(application: Application): ShakeDetector = ShakeDetector(application)
}
