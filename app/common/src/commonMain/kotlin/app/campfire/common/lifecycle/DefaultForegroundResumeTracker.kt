// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.lifecycle

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import app.campfire.core.lifecycle.ForegroundResumeTracker
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DefaultForegroundResumeTracker(
  private val appLifecycleObserver: AppLifecycleObserver,
  @ForScope(AppScope::class) private val scope: CoroutineScope,
) : ForegroundResumeTracker {

  private val _resumeEpoch = MutableStateFlow(0L)
  override val resumeEpoch: StateFlow<Long> = _resumeEpoch.asStateFlow()

  @Volatile
  private var started = false

  fun start() {
    if (started) return
    started = true
    scope.launch {
      var previousState = appLifecycleObserver.state.value
      appLifecycleObserver.state.collect { currentState ->
        if (previousState == AppLifecycleState.Background && currentState == AppLifecycleState.Foreground) {
          _resumeEpoch.value++
        }
        previousState = currentState
      }
    }
  }

  @ContributesIntoSet(AppScope::class)
  @Inject
  class Initializer(
    private val tracker: DefaultForegroundResumeTracker,
  ) : AppInitializer {
    override suspend fun onInitialize() {
      tracker.start()
    }
  }
}
