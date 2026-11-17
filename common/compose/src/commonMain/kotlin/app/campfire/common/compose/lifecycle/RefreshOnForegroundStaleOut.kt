// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.core.lifecycle.ForegroundResumeTracker
import app.campfire.core.time.FatherTime
import com.slack.circuit.retained.rememberRetained
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

val DefaultForegroundStaleOutDuration: Duration = 5.minutes

/**
 * Triggers [onRefresh] when the app returns to the foreground (or when this retained screen returns
 * to the top of the back stack after a foreground resume) and at least [staleOutDuration] has
 * elapsed since the last refresh/load for [keys].
 *
 * Callers with an explicit refresh indicator or paging refresh state can pass
 * [isExternalRefreshInProgress] so completing a manual refresh resets the staleness clock.
 */
@Composable
fun RefreshOnForegroundStaleOut(
  foregroundResumeTracker: ForegroundResumeTracker,
  fatherTime: FatherTime,
  vararg keys: Any?,
  staleOutDuration: Duration = DefaultForegroundStaleOutDuration,
  isExternalRefreshInProgress: Boolean = false,
  onRefresh: suspend () -> Unit,
) {
  val resumeEpoch by foregroundResumeTracker.resumeEpoch.collectAsState()

  var handledResumeEpoch by rememberRetained(*keys) {
    mutableLongStateOf(resumeEpoch)
  }
  var lastRefreshedAtMillis by rememberRetained(*keys) {
    mutableLongStateOf(fatherTime.nowInEpochMillis())
  }
  var wasRefreshing by remember { mutableStateOf(false) }

  LaunchedEffect(isExternalRefreshInProgress) {
    if (wasRefreshing && !isExternalRefreshInProgress) {
      lastRefreshedAtMillis = fatherTime.nowInEpochMillis()
    }
    wasRefreshing = isExternalRefreshInProgress
  }

  LaunchedEffect(resumeEpoch, *keys) {
    if (resumeEpoch > handledResumeEpoch) {
      handledResumeEpoch = resumeEpoch
      val now = fatherTime.nowInEpochMillis()
      if (now - lastRefreshedAtMillis >= staleOutDuration.inWholeMilliseconds) {
        lastRefreshedAtMillis = now
        onRefresh()
      }
    }
  }
}
