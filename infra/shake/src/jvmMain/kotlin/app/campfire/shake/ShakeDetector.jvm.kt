// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.shake

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

actual class ShakeDetector {

  private var listener: Listener? = null
  private var running: Boolean = false

  actual val isAvailable: Boolean = false
  actual val isRunning: Boolean get() = running

  actual fun start(sensitivity: ShakeSensitivity, listener: Listener) {
    this.listener = listener
    this.running = true
  }

  actual fun stop() {
    this.listener = null
    this.running = false
  }

  fun simulateShake() {
    listener?.onShake()
  }

  actual fun interface Listener {
    actual fun onShake()
  }
}

actual interface ShakeDetectorPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideAndroidShakeDetector(): ShakeDetector = ShakeDetector()
}
