// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.ui

import app.campfire.common.screens.StatisticsScreen
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryStats
import app.campfire.core.model.ListeningStats
import app.campfire.core.time.FatherTime
import app.campfire.stats.api.StatsRepository
import app.campfire.user.test.FakeMediaProgressRepository
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class StatsPresenterTest {

  private val fakeFatherTime = object : FatherTime {
    override fun now(): LocalDateTime = LocalDateTime(2025, 6, 15, 12, 0)
    override fun nowInEpochMillis(): Long = 1_700_000_000_000L
    override fun today(): LocalDate = LocalDate(2025, 6, 15)
  }

  private fun createPresenter(
    dispatcherProvider: DispatcherProvider,
    libraryStatsFlow: Flow<LibraryStats>,
    listeningStatsFlow: Flow<ListeningStats>,
  ): StatsPresenter {
    val repo = object : StatsRepository {
      override fun getLibraryStats(): Flow<LibraryStats> = libraryStatsFlow
      override fun getUserStats(): Flow<ListeningStats> = listeningStatsFlow
      override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    }
    return StatsPresenter(
      statsRepository = repo,
      mediaProgressRepository = FakeMediaProgressRepository(),
      fatherTime = fakeFatherTime,
      dispatcherProvider = dispatcherProvider,
      navigator = FakeNavigator(StatisticsScreen),
    )
  }

  @Test
  fun emptyLibraryStatListsLoadWithoutNoSuchElementError() = runTest {
    val libFlow = MutableSharedFlow<LibraryStats>(replay = 1)
    val userFlow = MutableSharedFlow<ListeningStats>(replay = 1)
    libFlow.tryEmit(
      LibraryStats(
        totalItems = 0,
        totalAuthors = 0,
        totalGenres = 0,
        totalSizeInBytes = 0L,
        totalDuration = Duration.ZERO,
        numAudioTracks = 0,
        largestItems = emptyList(),
        authorsWithCount = emptyList(),
        genresWithCount = emptyList(),
        longestItems = emptyList(),
      ),
    )
    userFlow.tryEmit(
      ListeningStats(
        totalTime = Duration.ZERO,
        today = Duration.ZERO,
        days = emptyMap(),
        dayOfWeek = emptyMap(),
        items = emptyList(),
        recentSessions = emptyList(),
      ),
    )

    createPresenter(asTestDispatcherProvider(), libFlow, userFlow).test {
      var state = awaitItem()
      while (state.libraryStats !is LoadState.Loaded || state.listeningStats !is LoadState.Loaded) {
        state = awaitItem()
      }

      val libModels = assertIs<LoadState.Loaded<List<StatsUiModel>>>(state.libraryStats).data
      assertEquals(1, libModels.size)
      assertIs<StatsUiModel.LibraryTotals>(libModels.single())

      val userModels = assertIs<LoadState.Loaded<List<StatsUiModel>>>(state.listeningStats).data
      val weekly = userModels.filterIsInstance<StatsUiModel.WeeklyListening>().single()
      assertTrue(weekly.weekOverWeekChange.isFinite())
      assertEquals(0.0, weekly.weekOverWeekChange)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun zeroLastWeekListeningProducesFiniteWeekOverWeekChange() = runTest {
    val libFlow = MutableSharedFlow<LibraryStats>(replay = 1)
    val userFlow = MutableSharedFlow<ListeningStats>(replay = 1)
    userFlow.tryEmit(
      ListeningStats(
        totalTime = 30.minutes,
        today = 30.minutes,
        days = mapOf(LocalDate(2025, 6, 15) to 30.minutes),
        dayOfWeek = emptyMap(),
        items = emptyList(),
        recentSessions = emptyList(),
      ),
    )

    createPresenter(asTestDispatcherProvider(), libFlow, userFlow).test {
      var state = awaitItem()
      while (state.listeningStats !is LoadState.Loaded) {
        state = awaitItem()
      }

      val userModels = assertIs<LoadState.Loaded<List<StatsUiModel>>>(state.listeningStats).data
      val weekly = userModels.filterIsInstance<StatsUiModel.WeeklyListening>().single()
      assertTrue(weekly.weekOverWeekChange.isFinite())
      assertEquals(100.0, weekly.weekOverWeekChange)

      cancelAndIgnoreRemainingEvents()
    }
  }
}
