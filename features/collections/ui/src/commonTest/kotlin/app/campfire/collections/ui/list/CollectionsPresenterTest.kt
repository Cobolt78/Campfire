// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.collections.ui.list

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.common.screens.CollectionsScreen
import app.campfire.core.lifecycle.ForegroundResumeTracker
import app.campfire.core.time.FatherTime
import app.campfire.core.time.GrandFatherTime
import app.campfire.settings.test.TestLibraryViewSettings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest

class CollectionsPresenterTest {

  @Test
  fun eventSink_Refresh_refreshesCollectionsOnceWhileShowingProgress() = runTest {
    val refreshGate = CompletableDeferred<Unit>()
    val repository = FakeCollectionsRepository(onRefreshCollections = { refreshGate.await() })
    val presenter = CollectionsPresenter(
      navigator = FakeNavigator(CollectionsScreen),
      repository = repository,
      libraryViewSettings = TestLibraryViewSettings(),
      analytics = FakeAnalytics(),
    )

    presenter.test {
      val idle = awaitItem()
      assertThat(idle.isRefreshing).isFalse()

      idle.eventSink(CollectionsUiEvent.Refresh)
      val refreshing = awaitItem()
      assertThat(refreshing.isRefreshing).isTrue()

      // Pulling again while the first refresh is in flight doesn't start a second one
      refreshing.eventSink(CollectionsUiEvent.Refresh)
      refreshGate.complete(Unit)

      assertThat(awaitItem().isRefreshing).isFalse()
      assertThat(repository.refreshCount).isEqualTo(1)
    }
  }

  @Test
  fun foregroundResume_refreshesOnlyAfterStaleOutWindow() = runTest {
    val repository = FakeCollectionsRepository()
    val resumeEpochFlow = MutableStateFlow(0L)
    val resumeTracker = object : ForegroundResumeTracker {
      override val resumeEpoch: StateFlow<Long> = resumeEpochFlow
    }
    var nowMillis = 1_000_000L
    val fakeTime = object : FatherTime {
      override fun now() = GrandFatherTime.now()
      override fun today() = GrandFatherTime.today()
      override fun nowInEpochMillis(): Long = nowMillis
    }

    val presenter = CollectionsPresenter(
      navigator = FakeNavigator(CollectionsScreen),
      repository = repository,
      libraryViewSettings = TestLibraryViewSettings(),
      analytics = FakeAnalytics(),
      foregroundResumeTracker = resumeTracker,
      fatherTime = fakeTime,
    )

    presenter.test {
      val initial = awaitItem()
      assertThat(initial.isRefreshing).isFalse()
      assertThat(repository.refreshCount).isEqualTo(0)

      // Resuming after 4 minutes skips refresh
      nowMillis += 4.minutes.inWholeMilliseconds
      resumeEpochFlow.value = 1L
      testScheduler.advanceUntilIdle()
      assertThat(repository.refreshCount).isEqualTo(0)

      // Resuming after >= 5 minutes triggers silent SWR refresh
      nowMillis += 1.minutes.inWholeMilliseconds
      resumeEpochFlow.value = 2L
      testScheduler.advanceUntilIdle()
      assertThat(repository.refreshCount).isEqualTo(1)

      cancelAndIgnoreRemainingEvents()
    }
  }
}
