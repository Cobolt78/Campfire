// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.lifecycle

import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultForegroundResumeTrackerTest {

  private class FakeAppLifecycleObserver(
    initial: AppLifecycleState,
  ) : AppLifecycleObserver {
    val flow = MutableStateFlow(initial)
    override val state: StateFlow<AppLifecycleState> = flow
  }

  @Test
  fun `increments resumeEpoch only on Background to Foreground transitions`() = runTest(UnconfinedTestDispatcher()) {
    val lifecycleObserver = FakeAppLifecycleObserver(AppLifecycleState.Foreground)
    val tracker = DefaultForegroundResumeTracker(
      appLifecycleObserver = lifecycleObserver,
      scope = backgroundScope,
    )
    tracker.start()

    assertThat(tracker.resumeEpoch.value).isEqualTo(0L)

    // Transition to Background should not increment
    lifecycleObserver.flow.value = AppLifecycleState.Background
    assertThat(tracker.resumeEpoch.value).isEqualTo(0L)

    // Returning to Foreground increments
    lifecycleObserver.flow.value = AppLifecycleState.Foreground
    assertThat(tracker.resumeEpoch.value).isEqualTo(1L)

    // Another Background -> Foreground cycle increments again
    lifecycleObserver.flow.value = AppLifecycleState.Background
    lifecycleObserver.flow.value = AppLifecycleState.Foreground
    assertThat(tracker.resumeEpoch.value).isEqualTo(2L)
  }

  @Test
  fun `start is idempotent and handles initial Background state`() = runTest(UnconfinedTestDispatcher()) {
    val lifecycleObserver = FakeAppLifecycleObserver(AppLifecycleState.Background)
    val tracker = DefaultForegroundResumeTracker(
      appLifecycleObserver = lifecycleObserver,
      scope = backgroundScope,
    )
    tracker.start()
    tracker.start()

    assertThat(tracker.resumeEpoch.value).isEqualTo(0L)

    lifecycleObserver.flow.value = AppLifecycleState.Foreground
    assertThat(tracker.resumeEpoch.value).isEqualTo(1L)
  }
}
