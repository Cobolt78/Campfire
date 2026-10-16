// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback

import app.campfire.audioplayer.AudioDevice
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.test.FakeAudioOutputController
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.common.test.mediaProgress
import app.campfire.common.test.session
import app.campfire.core.model.Bookmark
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.preview.libraryItem
import app.campfire.libraries.test.FakeLibraryItemValidator
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.test.FakePlaybackSettings
import app.campfire.settings.test.TestThemeSettings
import app.campfire.ui.theming.test.FakeThemeManager
import app.campfire.user.test.FakeBookmarkRepository
import app.campfire.user.test.FakeMediaProgressRepository
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime

class PlaybackPresenterTest {

  private val sessionQueue = FakeSessionQueue()
  private val sessionsRepository = FakeSessionsRepository()
  private val libraryItemValidator = FakeLibraryItemValidator()
  private val mediaProgressRepository = FakeMediaProgressRepository()
  private val bookmarkRepository = FakeBookmarkRepository()
  private val playbackController = FakePlaybackController()
  private val playbackSettings = FakePlaybackSettings()
  private val audioPlayerHolder = FakeAudioPlayerHolder()
  private val audioOutputController = FakeAudioOutputController()
  private val themeSettings = TestThemeSettings()
  private val themeManager = FakeThemeManager()

  private val presenter = PlaybackPresenter(
    sessionQueue = sessionQueue,
    sessionsRepository = sessionsRepository,
    libraryItemValidator = libraryItemValidator,
    mediaProgressRepository = mediaProgressRepository,
    bookmarkRepository = bookmarkRepository,
    playbackController = playbackController,
    playbackSettings = playbackSettings,
    audioPlayerHolder = audioPlayerHolder,
    audioOutputController = audioOutputController,
    themeSettings = themeSettings,
    themeManager = themeManager,
  )

  @Test
  fun `player state shows session-derived timing while no player is prepared`() = runTest {
    // 10 hours across 10 chapters; 90 minutes in = 30 minutes into "Chapter 2"
    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = libraryItem(duration = 10.hours, numOfChapters = 10),
      currentTime = 90.minutes,
    )

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.playerState.duration > 0.minutes }
      assertThat(state.playerState.time).isEqualTo(30.minutes)
      assertThat(state.playerState.bookTime).isEqualTo(90.minutes)
      assertThat(state.playerState.duration).isEqualTo(1.hours)
      assertThat(state.playerState.metadata.title).isEqualTo("Chapter 2")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `placeholder persists while a present player is still disabled`() = runTest {
    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = libraryItem(duration = 10.hours, numOfChapters = 10),
      currentTime = 90.minutes,
    )
    // A player exists (service came up) but hasn't prepared this session yet — its flows
    // are all at defaults and its state is Disabled
    audioPlayerHolder.setCurrentPlayer(FakeAudioPlayer())

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.playerState.duration > 0.minutes }
      assertThat(state.playerState.time).isEqualTo(30.minutes)
      assertThat(state.playerState.metadata.title).isEqualTo("Chapter 2")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `player state carries the current item's bookmarks`() = runTest {
    val item = libraryItem(duration = 10.hours, numOfChapters = 10)
    val bookmark = Bookmark(
      userId = "u",
      libraryItemId = item.id,
      title = "Great line",
      time = 22.minutes,
      createdAt = LocalDateTime(2026, 1, 1, 0, 0),
    )
    bookmarkRepository.bookmarksFlow.value = listOf(bookmark)
    sessionsRepository.currentSessionFlow.value = session(libraryItem = item, currentTime = 90.minutes)

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.playerState.bookmarks.isNotEmpty() }
      assertThat(state.playerState.bookmarks).isEqualTo(listOf(bookmark))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `volume state tracks the controller and its events reach it`() = runTest {
    sessionsRepository.currentSessionFlow.value = session(libraryItem = libraryItem())
    audioOutputController.setVolume(0.4f)

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.volume?.volume == 0.4f }
      assertThat(state.volume?.isMuted).isEqualTo(false)

      state.volume!!.eventSink(VolumeUiEvent.ToggleMute)
      assertThat(audioOutputController.isMuted.value).isEqualTo(true)

      state.volume!!.eventSink(VolumeUiEvent.SetVolume(0.8f))
      assertThat(audioOutputController.volume.value).isEqualTo(0.8f)
      // Dragging up off a mute is how people expect to undo one
      assertThat(audioOutputController.isMuted.value).isEqualTo(false)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `no volume state where the platform has no app volume`() = runTest {
    val presenter = PlaybackPresenter(
      sessionQueue = sessionQueue,
      sessionsRepository = sessionsRepository,
      libraryItemValidator = libraryItemValidator,
      mediaProgressRepository = mediaProgressRepository,
      bookmarkRepository = bookmarkRepository,
      playbackController = playbackController,
      playbackSettings = playbackSettings,
      audioPlayerHolder = audioPlayerHolder,
      audioOutputController = FakeAudioOutputController(isSupported = false),
      themeSettings = themeSettings,
      themeManager = themeManager,
    )
    sessionsRepository.currentSessionFlow.value = session(libraryItem = libraryItem())

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      assertThat(awaitItem().volume).isNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `output device state exposes the devices and routes a selection`() = runTest {
    val speakers = AudioDevice("Speakers", "Speakers")
    val controller = FakeAudioOutputController(
      supportsDeviceSelection = true,
      devices = listOf(speakers),
    )
    val presenter = presenterWith(controller)
    sessionsRepository.currentSessionFlow.value = session(libraryItem = libraryItem())

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.outputDevices?.devices?.isNotEmpty() == true }
      assertThat(state.outputDevices?.selectedName).isNull()
      assertThat(state.outputDevices?.selectedIsMissing).isEqualTo(false)

      state.outputDevices!!.eventSink(OutputDeviceUiEvent.SelectDevice(speakers))
      assertThat(controller.selectedDeviceName.value).isEqualTo("Speakers")

      state.outputDevices!!.eventSink(OutputDeviceUiEvent.Refresh)
      assertThat(controller.refreshCount).isEqualTo(1)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a pinned device that is gone reads as missing`() = runTest {
    val controller = FakeAudioOutputController(supportsDeviceSelection = true, devices = emptyList())
    controller.selectDevice(AudioDevice("Headphones", "Headphones"))
    val presenter = presenterWith(controller)
    sessionsRepository.currentSessionFlow.value = session(libraryItem = libraryItem())

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.outputDevices?.selectedName != null }
      assertThat(state.outputDevices?.selectedIsMissing).isEqualTo(true)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `no device picker where routing is unavailable`() = runTest {
    val presenter = presenterWith(FakeAudioOutputController(supportsDeviceSelection = false))
    sessionsRepository.currentSessionFlow.value = session(libraryItem = libraryItem())

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      assertThat(awaitItem().outputDevices).isNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `availableSync is null when remote progress difference is below 20 seconds threshold`() = runTest {
    val item = libraryItem()
    playbackSettings.syncEnabled = true
    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = item,
      currentTime = 100.seconds,
      lastPlayedAt = null,
    )
    // Server progress is 5 seconds ahead (e.g. 105s vs 100s)
    mediaProgressRepository.progressFlow.value = mediaProgress(
      libraryItemId = item.id,
      currentTime = 105f,
      source = MediaProgress.Source.Remote,
      lastUpdate = 1000L,
    )

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.session != null }
      assertThat(state.syncUiState.availableSync).isNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `availableSync is null when playing and remote progress lags behind local position`() = runTest {
    val item = libraryItem()
    playbackSettings.syncEnabled = true
    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = item,
      currentTime = 100.seconds,
      lastPlayedAt = null,
    )
    val player = FakeAudioPlayer()
    player.state.value = AudioPlayer.State.Playing
    audioPlayerHolder.setCurrentPlayer(player)

    // Server progress is 30 seconds behind while playing (e.g. 70s vs 100s)
    mediaProgressRepository.progressFlow.value = mediaProgress(
      libraryItemId = item.id,
      currentTime = 70f,
      source = MediaProgress.Source.Remote,
      lastUpdate = 1000L,
    )

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.session != null }
      assertThat(state.syncUiState.availableSync).isNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `availableSync is present when remote progress difference is at least 20 seconds ahead`() = runTest {
    val item = libraryItem()
    playbackSettings.syncEnabled = true
    sessionsRepository.currentSessionFlow.value = session(
      libraryItem = item,
      currentTime = 100.seconds,
      lastPlayedAt = null,
    )
    // Server progress is 30 seconds ahead (e.g. 130s vs 100s)
    mediaProgressRepository.progressFlow.value = mediaProgress(
      libraryItemId = item.id,
      currentTime = 130f,
      source = MediaProgress.Source.Remote,
      lastUpdate = 1000L,
    )

    moleculeFlow(RecompositionMode.Immediate) {
      presenter.present(expanded = false)
    }.test {
      val state = awaitItemMatching { it.syncUiState.availableSync != null }
      assertThat(state.syncUiState.availableSync?.targetTime).isEqualTo(130.seconds)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun presenterWith(controller: FakeAudioOutputController) = PlaybackPresenter(
    sessionQueue = sessionQueue,
    sessionsRepository = sessionsRepository,
    libraryItemValidator = libraryItemValidator,
    mediaProgressRepository = mediaProgressRepository,
    bookmarkRepository = bookmarkRepository,
    playbackController = playbackController,
    playbackSettings = playbackSettings,
    audioPlayerHolder = audioPlayerHolder,
    audioOutputController = controller,
    themeSettings = themeSettings,
    themeManager = themeManager,
  )

  private suspend fun app.cash.turbine.ReceiveTurbine<PlaybackUiState>.awaitItemMatching(
    predicate: (PlaybackUiState) -> Boolean,
  ): PlaybackUiState {
    while (true) {
      val item = awaitItem()
      if (predicate(item)) return item
    }
  }
}
