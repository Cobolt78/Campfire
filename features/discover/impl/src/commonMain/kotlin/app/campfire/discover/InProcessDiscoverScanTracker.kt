// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.discover.api.DiscoverScanState
import app.campfire.discover.api.DiscoverScanTracker
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [DiscoverScanTracker] for platforms without background work (desktop, iOS):
 * runs [SeriesScanner] on the user scope so a scan survives navigation but not
 * the process. Terminal metadata persists through [DiscoverScanStateStore], so
 * a relaunch rehydrates the last [DiscoverScanState.Completed] — the freshness
 * window holds across restarts and everything the scan cached renders
 * immediately. On Android this binding is replaced by the WorkManager-backed
 * tracker.
 */
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class InProcessDiscoverScanTracker(
  private val seriesScanner: SeriesScanner,
  private val store: DiscoverScanStateStore,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
  private val dispatcherProvider: DispatcherProvider,
) : DiscoverScanTracker {

  private val _state = MutableStateFlow(store.lastCompleted() ?: DiscoverScanState.Idle)
  override val state: StateFlow<DiscoverScanState> = _state.asStateFlow()

  private var scanJob: Job? = null

  override fun startScan() {
    // An explicit scan is the user asking for current data: bypass the series
    // cache and refetch every listing from the provider.
    launchScan(refresh = true, isDelta = false)
  }

  override fun startScanIfStale() {
    val completed = _state.value as? DiscoverScanState.Completed
    if (completed == null || !store.isFresh(completed)) {
      launchScan(refresh = false, isDelta = false)
    } else {
      launchScan(refresh = false, isDelta = true)
    }
  }

  override fun cancelScan() {
    scanJob?.cancel()
  }

  private fun launchScan(refresh: Boolean, isDelta: Boolean = false) {
    if (scanJob?.isActive == true) return
    scanJob = coroutineScopeHolder.get().launch(dispatcherProvider.io) { scan(refresh, isDelta) }
  }

  private suspend fun scan(refresh: Boolean, isDelta: Boolean = false) {
    val since = if (isDelta) store.lastScanEpochMillis() else null
    if (isDelta && since != null && !seriesScanner.hasNewOrUpdatedSeries(since)) {
      return
    }

    var last = SeriesScanner.Progress(done = 0, total = 0, skippedCount = 0, failedCount = 0)
    // The scanner's first emission waits on the series listing.
    _state.value = DiscoverScanState.Running(done = 0, total = null)
    try {
      seriesScanner.scan(refresh, sinceEpochMillis = since).collect { progress ->
        last = progress
        _state.value = DiscoverScanState.Running(progress.done, progress.total)
      }
    } finally {
      // Runs for both completion and cancellation — a cancelled scan freezes
      // its counters, and everything it fetched is already in the series
      // cache. The write also refreshes the persisted freshness window.
      _state.value = store.write(last, isDelta = isDelta)
    }
  }
}
