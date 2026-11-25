// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalCoroutinesApi::class)

package app.campfire.audioplayer.impl.sleep

import app.campfire.audioplayer.AudioPlayer.State
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayer.Invocation
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.test.FakeSleepSettings
import app.campfire.shake.ShakeDetector
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

class CoroutineSleepTimerManagerTest {

  private val settings = FakeSleepSettings().apply {
    fadeOutDuration = 5.seconds
  }
  private val player = FakeAudioPlayer()
  private val shakeDetector = ShakeDetector()
  private var fakeNow = LocalDateTime(2026, 11, 25, 22, 30, 0)

  private fun TestScope.manager(): CoroutineSleepTimerManager {
    val dispatcher = UnconfinedTestDispatcher(testScheduler)
    return CoroutineSleepTimerManager(
      player = player,
      sleepSettings = settings,
      shakeDetector = shakeDetector,
      dispatcherProvider = DispatcherProvider(
        io = dispatcher,
        databaseWrite = dispatcher,
        databaseRead = dispatcher,
        computation = dispatcher,
        main = dispatcher,
      ),
      fatherTime = SchedulerFatherTime(this, nowProvider = { fakeNow }),
      applicationScope = backgroundScope,
    )
  }

  private fun fades() = player.invocations.filterIsInstance<Invocation.FadeToPause>()

  @Test
  fun `timer set while playing counts down and fades out with the configured duration`() = runTest {
    val manager = manager()
    player.state.value = State.Playing

    manager.setTimer(PlaybackTimer.Epoch(10_000L))
    assertThat(manager.runningTimer.value?.isPaused).isEqualTo(false)
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(10_000L)

    advanceTimeBy(9_999L)
    assertThat(fades()).isEmpty()

    advanceTimeBy(2L)
    assertThat(fades().single().duration).isEqualTo(5.seconds)
    assertThat(manager.runningTimer.value).isNull()
  }

  @Test
  fun `pausing playback freezes the countdown and resuming picks it back up`() = runTest {
    val manager = manager()
    player.state.value = State.Playing
    manager.setTimer(PlaybackTimer.Epoch(10_000L))

    advanceTimeBy(4_000L)
    player.state.value = State.Paused

    val paused = manager.runningTimer.value
    assertThat(paused?.isPaused).isEqualTo(true)
    assertThat(paused?.remainingMillis(testScheduler.currentTime)).isEqualTo(6_000L)

    // Nothing happens no matter how long playback stays paused, and the readout stays frozen
    advanceTimeBy(10.minutes.inWholeMilliseconds)
    assertThat(fades()).isEmpty()
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(6_000L)

    player.state.value = State.Playing
    assertThat(manager.runningTimer.value?.isPaused).isEqualTo(false)
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(6_000L)

    advanceTimeBy(5_999L)
    assertThat(fades()).isEmpty()
    advanceTimeBy(2L)
    assertThat(fades()).hasSize(1)
  }

  @Test
  fun `timer set while paused does not start until playback begins`() = runTest {
    val manager = manager()
    player.state.value = State.Paused

    manager.setTimer(PlaybackTimer.Epoch(10_000L))
    assertThat(manager.runningTimer.value?.isPaused).isEqualTo(true)

    advanceTimeBy(60_000L)
    assertThat(fades()).isEmpty()

    player.state.value = State.Playing
    advanceTimeBy(10_001L)
    assertThat(fades()).hasSize(1)
  }

  @Test
  fun `resuming playback mid-fade cancels the fade`() = runTest {
    val manager = manager()
    player.state.value = State.Playing
    manager.setTimer(PlaybackTimer.Epoch(1_000L))
    advanceTimeBy(1_001L)
    val fade = player.fadeJobs.single()
    assertThat(fade.isActive).isTrue()

    // A buffering blip while still fading is not a resume
    player.state.value = State.Buffering
    player.state.value = State.Playing
    assertThat(fade.isActive).isTrue()

    // The listener pauses, then presses play before the fade is done
    player.state.value = State.Paused
    player.state.value = State.Playing
    assertThat(fade.isCancelled).isTrue()
  }

  @Test
  fun `setting a new timer or clearing it mid-fade cancels the fade`() = runTest {
    val manager = manager()
    player.state.value = State.Playing

    manager.setTimer(PlaybackTimer.Epoch(1_000L))
    advanceTimeBy(1_001L)
    manager.setTimer(PlaybackTimer.Epoch(5_000L))
    assertThat(player.fadeJobs[0].isCancelled).isTrue()
    assertThat(manager.runningTimer.value).isNotNull()

    advanceTimeBy(5_001L)
    manager.clearTimer()
    assertThat(player.fadeJobs[1].isCancelled).isTrue()
  }

  @Test
  fun `auto rewind applies once the fade of an auto sleep timer completes`() = runTest {
    settings.autoSleepTimerEnabled = true
    settings.autoRewindEnabled = true
    settings.autoRewindAmount = 5.minutes
    val manager = manager()
    player.state.value = State.Playing
    player.overallTime.value = 30.minutes

    manager.setTimer(PlaybackTimer.Epoch(1_000L, isAutoSleepTimer = true))
    advanceTimeBy(1_001L)
    assertThat(player.invocations.filterIsInstance<Invocation.SeekTo>()).isEmpty()

    player.fadeJobs.single().complete()
    val seek = player.invocations.filterIsInstance<Invocation.SeekTo>().single()
    assertThat(seek.value).isEqualTo(25.minutes)
  }

  @Test
  fun `auto rewind is skipped when the fade is cancelled`() = runTest {
    settings.autoSleepTimerEnabled = true
    settings.autoRewindEnabled = true
    val manager = manager()
    player.state.value = State.Playing

    manager.setTimer(PlaybackTimer.Epoch(1_000L, isAutoSleepTimer = true))
    advanceTimeBy(1_001L)
    player.fadeJobs.single().cancel()

    assertThat(player.invocations.filterIsInstance<Invocation.SeekTo>()).isEmpty()
  }

  @Test
  fun `end of chapter timer pauses immediately at the chapter boundary`() = runTest {
    val manager = manager()
    player.state.value = State.Playing

    manager.setTimer(PlaybackTimer.EndOfChapter())
    assertThat(manager.runningTimer.value?.timer).isNotNull().isInstanceOf(PlaybackTimer.EndOfChapter::class)
    assertThat(manager.runningTimer.value?.remainingMillis(0L)).isNull()

    assertThat(manager.endOfChapter()).isTrue()
    assertThat(player.invocations.last()).isEqualTo(Invocation.Pause)
    assertThat(fades()).isEmpty()
    assertThat(manager.runningTimer.value).isNull()
    assertThat(manager.endOfChapter()).isFalse()
  }

  @Test
  fun `persisted sleep timer is restored on manager initialization`() = runTest {
    settings.activeSleepTimer = SleepSettings.PersistedSleepTimer(
      isEndOfChapter = false,
      totalDurationMillis = 30.minutes.inWholeMilliseconds,
      remainingMillis = 15.minutes.inWholeMilliseconds,
      isAutoSleepTimer = false,
    )
    player.state.value = State.Paused

    val manager = manager()
    val running = manager.runningTimer.value
    assertThat(running).isNotNull()
    assertThat(running?.isPaused).isEqualTo(true)
    assertThat(running?.remainingMillis(testScheduler.currentTime)).isEqualTo(15.minutes.inWholeMilliseconds)

    // When resumed, countdown starts from remainingMillis
    player.state.value = State.Playing
    assertThat(manager.runningTimer.value?.isPaused).isEqualTo(false)
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(15.minutes.inWholeMilliseconds)

    advanceTimeBy(15.minutes.inWholeMilliseconds - 1)
    assertThat(fades()).isEmpty()

    advanceTimeBy(2L)
    assertThat(fades().single().duration).isEqualTo(5.seconds)
    assertThat(manager.runningTimer.value).isNull()
    assertThat(settings.activeSleepTimer).isNull()
  }

  @Test
  fun `persisted end of chapter timer is restored on manager initialization`() = runTest {
    settings.activeSleepTimer = SleepSettings.PersistedSleepTimer(
      isEndOfChapter = true,
      totalDurationMillis = 0L,
      remainingMillis = 0L,
      isAutoSleepTimer = false,
    )
    player.state.value = State.Playing

    val manager = manager()
    assertThat(manager.runningTimer.value?.timer).isNotNull().isInstanceOf(PlaybackTimer.EndOfChapter::class)

    assertThat(manager.endOfChapter()).isTrue()
    assertThat(player.invocations.last()).isEqualTo(Invocation.Pause)
    assertThat(settings.activeSleepTimer).isNull()
  }

  @Test
  fun `persisted sleep timer restoration does not crash when player state is uninitialized`() = runTest {
    settings.activeSleepTimer = SleepSettings.PersistedSleepTimer(
      isEndOfChapter = false,
      totalDurationMillis = 30.minutes.inWholeMilliseconds,
      remainingMillis = 15.minutes.inWholeMilliseconds,
      isAutoSleepTimer = false,
    )
    val throwingPlayer = object : app.campfire.audioplayer.AudioPlayer by player {
      override val state: kotlinx.coroutines.flow.StateFlow<State>
        get() = throw NullPointerException("Uninitialized property")
    }
    val dispatcher = UnconfinedTestDispatcher(testScheduler)
    val manager = CoroutineSleepTimerManager(
      player = throwingPlayer,
      sleepSettings = settings,
      shakeDetector = shakeDetector,
      dispatcherProvider = DispatcherProvider(
        io = dispatcher,
        databaseWrite = dispatcher,
        databaseRead = dispatcher,
        computation = dispatcher,
        main = dispatcher,
      ),
      fatherTime = SchedulerFatherTime(this, nowProvider = { fakeNow }),
      applicationScope = backgroundScope,
    )
    assertThat(manager).isNotNull()
    assertThat(shakeDetector.isRunning).isFalse()
  }

  @Test
  fun `buffering during seek does not reset timer when resetTimerOnPause is enabled`() = runTest {
    settings.resetTimerOnPause = true
    val manager = manager()
    player.state.value = State.Playing
    manager.setTimer(PlaybackTimer.Epoch(10_000L))

    advanceTimeBy(4_000L)

    // Seeking/skipping briefly puts ExoPlayer into Buffering before returning to Playing
    player.state.value = State.Buffering
    advanceTimeBy(500L)
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(6_000L)

    player.state.value = State.Playing
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(6_000L)

    // An actual pause still resets the timer back to full duration
    player.state.value = State.Paused
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(10_000L)
  }

  @Test
  fun `adjustTimer extends and shortens countdown while playing and paused`() = runTest {
    val manager = manager()
    player.state.value = State.Playing
    manager.setTimer(PlaybackTimer.Epoch(10.minutes.inWholeMilliseconds))

    // Elapse 2 minutes -> 8m remaining
    advanceTimeBy(2.minutes.inWholeMilliseconds)
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime))
      .isEqualTo(8.minutes.inWholeMilliseconds)

    // Extend by +5m -> 13m remaining, fires after 13m (not 8m)
    manager.adjustTimer(5.minutes.inWholeMilliseconds)
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime))
      .isEqualTo(13.minutes.inWholeMilliseconds)

    // Shorten by -1m -> 12m remaining
    manager.adjustTimer((-1).minutes.inWholeMilliseconds)
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime))
      .isEqualTo(12.minutes.inWholeMilliseconds)

    // Pause, shorten by -20m -> clamps at 1m (60_000ms) rather than expiring
    player.state.value = State.Paused
    manager.adjustTimer((-20).minutes.inWholeMilliseconds)
    assertThat(manager.runningTimer.value?.isPaused).isEqualTo(true)
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime))
      .isEqualTo(1.minutes.inWholeMilliseconds)

    // Resume and verify it fires at the 1m mark
    player.state.value = State.Playing
    advanceTimeBy(1.minutes.inWholeMilliseconds - 1L)
    assertThat(fades()).isEmpty()
    advanceTimeBy(2L)
    assertThat(fades().single().duration).isEqualTo(5.seconds)
    assertThat(manager.runningTimer.value).isNull()
  }

  @Test
  fun `cleared auto sleep timer stays dismissed for same window across book switch and service restart`() = runTest {
    settings.autoSleepTimerEnabled = true
    settings.autoSleepStart = LocalTime(22, 0)
    settings.autoSleepEnd = LocalTime(6, 0)
    settings.autoSleepTimer = SleepSettings.AutoSleepTimer.Epoch(15.minutes.inWholeMilliseconds)

    fakeNow = LocalDateTime(2026, 11, 25, 22, 30, 0)
    player.state.value = State.Playing
    var manager = manager()

    manager.onSessionStart()
    assertThat(manager.runningTimer.value).isNotNull()

    // Adjust +1m, then clear manually -> stays null even on repeated onSessionStart (resume / book switch)
    manager.adjustTimer(1.minutes.inWholeMilliseconds)
    manager.clearTimer()
    assertThat(manager.runningTimer.value).isNull()

    manager.onSessionStart()
    assertThat(manager.runningTimer.value).isNull()

    // Past midnight within the SAME overnight window (2026-11-25 22:00 .. 2026-11-26 06:00), after service restart
    fakeNow = LocalDateTime(2026, 11, 26, 1, 30, 0)
    manager.release()
    manager = manager()
    manager.onSessionStart()
    assertThat(manager.runningTimer.value).isNull()

    // Next night's window starts automatically even if app was never opened during the day
    fakeNow = LocalDateTime(2026, 11, 26, 22, 15, 0)
    manager.onSessionStart()
    assertThat(manager.runningTimer.value).isNotNull()
    assertThat(manager.runningTimer.value?.timer?.isAutoSleepTimer).isEqualTo(true)
  }

  @Test
  fun `toggling auto sleep off resets dismissal for current window`() = runTest {
    settings.autoSleepTimerEnabled = true
    settings.autoSleepStart = LocalTime(22, 0)
    settings.autoSleepEnd = LocalTime(6, 0)
    fakeNow = LocalDateTime(2026, 11, 25, 23, 0, 0)
    player.state.value = State.Playing

    val manager = manager()
    manager.onSessionStart()
    manager.clearTimer()
    assertThat(manager.runningTimer.value).isNull()

    settings.autoSleepTimerEnabled = false
    settings.autoSleepTimerEnabled = true
    manager.onSessionStart()
    assertThat(manager.runningTimer.value).isNotNull()
  }

  @Test
  fun `shake detector stops when playback pauses or service releases and does not wake paused player`() = runTest {
    settings.shakeToResetEnabled = true
    val manager = manager()
    player.state.value = State.Playing

    manager.setTimer(PlaybackTimer.Epoch(10_000L))
    assertThat(shakeDetector.isRunning).isTrue()

    // Another app takes audio focus / user pauses -> accelerometer must unregister immediately
    player.state.value = State.Paused
    assertThat(shakeDetector.isRunning).isFalse()

    // Changing shake sensitivity while paused must not turn the sensor back on
    settings.shakeSensitivity = SleepSettings.ShakeSensitivity.VeryLow
    assertThat(shakeDetector.isRunning).isFalse()

    // Shaking while paused never wakes playback
    shakeDetector.simulateShake()
    assertThat(player.invocations.filterIsInstance<Invocation.PlayPause>()).isEmpty()

    // Resuming playback re-enables shake detection
    player.state.value = State.Playing
    assertThat(shakeDetector.isRunning).isTrue()

    // Releasing player stops shake detector even while state is Playing, keeping persisted timer on disk
    manager.release()
    assertThat(shakeDetector.isRunning).isFalse()
    assertThat(settings.activeSleepTimer).isNotNull()
  }

  @Test
  fun `shake detector allows resume only during 30s grace window after timer expires`() = runTest {
    settings.shakeToResetEnabled = true
    val manager = manager()
    player.state.value = State.Playing

    manager.setTimer(PlaybackTimer.Epoch(10_000L))
    advanceTimeBy(4_000L)

    // Shake while playing resets the timer countdown cleanly without calling playPause
    shakeDetector.simulateShake()
    assertThat(manager.runningTimer.value?.remainingMillis(testScheduler.currentTime)).isEqualTo(10_000L)
    assertThat(player.invocations.filterIsInstance<Invocation.PlayPause>()).isEmpty()

    // Let timer expire -> post-expiry 30s grace window allows shake to resume
    advanceTimeBy(10_001L)
    assertThat(fades()).hasSize(1)
    assertThat(shakeDetector.isRunning).isTrue()
    player.state.value = State.Paused

    shakeDetector.simulateShake()
    assertThat(player.invocations.filterIsInstance<Invocation.PlayPause>()).hasSize(1)
    assertThat(manager.runningTimer.value).isNotNull()

    // Simulate player resumed and let it expire again; after 30s, shake stops cleanly
    player.state.value = State.Playing
    advanceTimeBy(10_001L)
    assertThat(fades()).hasSize(2)
    assertThat(shakeDetector.isRunning).isTrue()

    advanceTimeBy(30_001L)
    assertThat(shakeDetector.isRunning).isFalse()
  }

  private class SchedulerFatherTime(
    private val scope: TestScope,
    private val nowProvider: () -> LocalDateTime = { error("not used in tests") },
  ) : FatherTime {
    override fun now(): LocalDateTime = nowProvider()
    override fun today(): LocalDate = nowProvider().date
    override fun nowInEpochMillis(): Long = scope.testScheduler.currentTime
  }
}
