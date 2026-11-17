// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks transitions back into [AppLifecycleState.Foreground] across the app process so
 * retained UI presenters can detect when the app resumed while they were active or while
 * sitting in a back stack behind a detail screen.
 */
interface ForegroundResumeTracker {
  val resumeEpoch: StateFlow<Long>
}

object NoOpForegroundResumeTracker : ForegroundResumeTracker {
  override val resumeEpoch: StateFlow<Long> = MutableStateFlow(0L)
}
