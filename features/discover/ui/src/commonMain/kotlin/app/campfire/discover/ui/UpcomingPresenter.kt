// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.common.compose.lifecycle.RefreshOnForegroundStaleOut
import app.campfire.common.screens.UrlScreen
import app.campfire.core.di.UserScope
import app.campfire.core.lifecycle.ForegroundResumeTracker
import app.campfire.core.lifecycle.NoOpForegroundResumeTracker
import app.campfire.core.time.FatherTime
import app.campfire.core.time.GrandFatherTime
import app.campfire.discover.api.DiscoverScanTracker
import app.campfire.discover.api.screen.UpcomingScreen
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.Inject
import kotlin.time.Duration
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

@CircuitInject(UpcomingScreen::class, UserScope::class)
@Inject
class UpcomingPresenter(
  private val navigator: Navigator,
  private val tracker: DiscoverScanTracker,
  private val bookInfoRegistry: BookInfoRegistry,
  private val foregroundResumeTracker: ForegroundResumeTracker = NoOpForegroundResumeTracker,
  private val fatherTime: FatherTime = GrandFatherTime,
) : NonPausablePresenter<UpcomingUiState> {

  @Composable
  override fun present(): UpcomingUiState {
    val scanState by tracker.state.collectAsState()

    // The list renders straight from the series cache: releases stream in
    // live while a scan runs and are already there on reopen — the scan only
    // decides when the cache gets refreshed.
    val upcoming by remember {
      bookInfoRegistry.observeCachedUpcoming().map { it.toImmutableList() }
    }.collectAsState(persistentListOf())

    // Opening the screen scans only when there's nothing fresh to show — the
    // tracker holds a freshness window, so recent results render as-is and the
    // Scan action forces the rescan.
    LaunchedEffect(Unit) {
      tracker.startScanIfStale()
    }

    RefreshOnForegroundStaleOut(
      foregroundResumeTracker = foregroundResumeTracker,
      fatherTime = fatherTime,
      // DiscoverScanTracker manages its own freshness policy cleanly.
      staleOutDuration = Duration.ZERO,
    ) {
      tracker.startScanIfStale()
    }

    return UpcomingUiState(
      scanState = scanState,
      upcoming = upcoming,
    ) { event ->
      when (event) {
        UpcomingUiEvent.Back -> navigator.pop()
        UpcomingUiEvent.Refresh -> tracker.startScan()
        UpcomingUiEvent.CancelScan -> tracker.cancelScan()
        is UpcomingUiEvent.BookClick -> navigator.goTo(UrlScreen(event.url))
      }
    }
  }
}
