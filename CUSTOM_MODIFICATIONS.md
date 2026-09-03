# Campfire Custom Modifications Guide

This document captures all custom features, bug fixes, and UI improvements added to Campfire v1.05. Use this guide and the accompanying `custom_changes.patch` to easily reapply and migrate these enhancements whenever updating to future versions of Campfire (e.g., v1.06+).

---

## Table of Contents
1. [Audiobookshelf-Style Cover Progress Bars](#1-audiobookshelf-style-cover-progress-bars)
2. [Global Progress Bar Support (Series, Authors, Collections, Search)](#2-global-progress-bar-support)
3. [Home Screen: Replaced "Newest Authors" with "Downloads" Shelf](#3-home-screen-replaced-newest-authors-with-downloads-shelf)
4. [Book & Podcast Details: File Size in Megabytes](#4-book--podcast-details-file-size-in-megabytes)
5. [Android Auto & System Media: Dynamic Speed-Adjusted Total Book Countdown](#5-android-auto--system-media-dynamic-speed-adjusted-total-book-countdown)
6. [Search Cache Invalidation & Duplicate Purge](#6-search-cache-invalidation--duplicate-purge)
7. [Sleep Timer Pause & Freeze Fix](#7-sleep-timer-pause--freeze-fix)
8. [Configurable Sleep Timer Fade-Out & Settings UI](#8-configurable-sleep-timer-fade-out--settings-ui)
9. [Perceptual (Logarithmic) Audio Fade Curve](#9-perceptual-logarithmic-audio-fade-curve)
10. [FOSS Release Build & Deployment Commands](#10-foss-release-build--deployment-commands)
11. [Modified Files Inventory](#11-modified-files-inventory)

---

## 1. Audiobookshelf-Style Cover Progress Bars

### Summary
Replaced the tiny top-left badge checkmark with a clean, Audiobookshelf-style bottom progress bar on all item cards and list items.

### Key Details
- **Bar Thickness**: 6dp on cards, 4dp on list items and cover headers.
- **Color Scheme**:
  - **100% Completed**: Solid Green `#4CAF50`.
  - **In Progress (0% < progress < 100%)**: Solid Orange `#E58B22` with a subtle dark backdrop (`#40000000`).
- **Removed**: Top-left corner checkmark badge (which was difficult to see on varied cover art).

### Modified Files:
* `common/compose/src/commonMain/kotlin/app/campfire/common/compose/widgets/LibraryItemCard.kt`
* `common/compose/src/commonMain/kotlin/app/campfire/common/compose/widgets/LibraryItemListItem.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/composables/slots/CoverImageSlot.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/book/BookPresenter.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/podcast/PodcastPresenter.kt`

---

## 2. Global Progress Bar Support (Series, Authors, Collections, Search)

### Summary
Upstream Campfire only passed media progress to Home screen cards. We connected `MediaProgressRepository` to all presenters across the app and passed progress to `LibraryItemCard` so progress bars display uniformly everywhere.

### Key Details
- Injected `MediaProgressRepository` into presenters.
- Collected realtime progress state as a map of `itemId -> MediaProgress`.
- Passed progress to each book card in Series details, Author details, Collection details, and Search results.

### Modified Files:
* `features/series/ui/src/commonMain/kotlin/app/campfire/series/ui/detail/SeriesDetailPresenter.kt`
* `features/series/ui/src/commonMain/kotlin/app/campfire/series/ui/detail/SeriesDetailUiState.kt`
* `features/series/ui/src/commonMain/kotlin/app/campfire/series/ui/detail/SeriesDetailUi.kt`
* `features/author/ui/src/commonMain/kotlin/app/campfire/author/ui/detail/AuthorDetailPresenter.kt`
* `features/author/ui/src/commonMain/kotlin/app/campfire/author/ui/detail/AuthorDetailUiState.kt`
* `features/author/ui/src/commonMain/kotlin/app/campfire/author/ui/detail/AuthorDetailUi.kt`
* `features/collections/ui/build.gradle.kts` (added `:data:media-progress:api` dependency)
* `features/collections/ui/src/commonMain/kotlin/app/campfire/collections/ui/detail/CollectionDetailPresenter.kt`
* `features/collections/ui/src/commonMain/kotlin/app/campfire/collections/ui/detail/CollectionDetailUiState.kt`
* `features/collections/ui/src/commonMain/kotlin/app/campfire/collections/ui/detail/CollectionDetailUi.kt`
* `features/search/ui/build.gradle.kts` (added `:data:media-progress:api` dependency)
* `features/search/ui/src/commonMain/kotlin/app/campfire/search/ui/SearchPresenter.kt`
* `features/search/ui/src/commonMain/kotlin/app/campfire/search/ui/SearchUiState.kt`
* `features/search/ui/src/commonMain/kotlin/app/campfire/search/ui/CampfireSearchComponent.kt`
* `features/search/ui/src/commonMain/kotlin/app/campfire/search/ui/composables/SearchResultContent.kt`

---

## 3. Home Screen: Replaced "Newest Authors" with "Downloads" Shelf

### Summary
Removed the "Newest Authors" shelf from the main feed and replaced it with a dynamic "Downloads" shelf that lists all audiobooks and podcast episodes downloaded locally to the device.

### Key Details
- Injected `LibraryItemRepository` into `HomePresenter.kt`.
- Filtered out `ShelfIds.NewestAuthors` / `ShelfType.AUTHOR` from the domain feed.
- Observed `offlineDownloadManager.observeAll()` and resolved completed downloads (`State.Completed`) to `LibraryItem` / `EpisodeShelfEntry` entities.
- Dynamically appends a `UiShelf(id = "downloads", label = "Downloads", total = count, entities = LoadState.Loaded(downloadedEntities))` to the bottom of the home feed when downloaded items are present.
- Tapping any card in the Downloads shelf opens the book/episode for playback or offline detail view.

### Modified Files:
* `features/home/ui/src/commonMain/kotlin/app/campfire/home/ui/HomePresenter.kt`
* `features/home/ui/build.gradle.kts`
* `features/home/ui/src/commonTest/kotlin/app/campfire/home/ui/HomePresenterTest.kt`

---

## 4. Book & Podcast Details: File Size in Megabytes

### Summary
Added the media file size in megabytes directly alongside the total duration on the book and podcast detail screens.

### Key Details
- In `TitleSlot.kt`, displays `libraryItem.media.sizeInBytes.asReadableBytes()` (e.g. `12 hr 45 min • 345.2 MB`) centered beneath the title and subtitle.

### Modified Files:
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/composables/slots/TitleSlot.kt`

---

## 5. Android Auto & System Media: Dynamic Speed-Adjusted Total Book Countdown

### Summary
Enriched the `MediaMetadata` artist line for Android Auto and system media notifications to display real-time speed-adjusted remaining listening time that counts down minute-by-minute as you drive.

### Key Details
- **Time-First Formatting for Screen Width**:
  - The artist line is formatted as `"$remainingFormatted left • $author"` (e.g. `4h 12m left • Stephen King, Owen King`).
  - By placing the remaining time first, long author lists or multiple authors on Android Auto screens will never cut off or obscure the remaining listening time.
- **Playback Speed Adjustment**:
  $$\text{Effective Remaining Time} = \frac{\text{Raw Remaining Audio Duration}}{\text{Playback Speed}}$$
  *(e.g., at 1.25x speed, 5 hours of audio displays as 4h 0m remaining)*.
- **Initial & Dynamic Updates**:
  - `MediaItemBuilder.kt` initializes `.setArtist(metadata.artist)` on the platform `MediaMetadata`.
  - `ExoPlayerAudioPlayer.kt` updates the active item's metadata via `exoPlayer.replaceMediaItem(currentIndex, newItem)` whenever the minute string updates, actively dispatching changes to Android Auto and the system media notification without interrupting audio playback.

### Modified Files:
* `core/src/commonMain/kotlin/app/campfire/core/extensions/Duration.kt`
* `infra/audioplayer/impl/src/commonMain/kotlin/app/campfire/audioplayer/impl/mediaitem/MediaItemBuilder.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/ExoPlayerAudioPlayer.kt`
* `infra/audioplayer/impl/src/commonTest/kotlin/app/campfire/audioplayer/impl/mediaitem/MediaItemBuilderTest.kt`

---

## 6. Search Cache Invalidation & Duplicate Purge

### Summary
Fixed the search database cache junction retention bug where old/deleted books persisted in search results alongside replaced versions.

### Key Details
- In `SearchSourceOfTruthFactory.kt`:
  - Added `db.searchQueries.delete(query.databaseKey)` before inserting new search keys and junctions. Through CASCADE deletion, this wipes old `search_books` mappings for the query so that search results always strictly reflect the live server response.
  - Added `db.seriesBookJoinQueries.deleteForSeries(series.id)` so that series search matches do not retain references to deleted book IDs.
- Fully isolated to Search—zero impact on player playback, library item caching, or Compose UI rendering.

### Modified Files:
* `features/search/impl/src/commonMain/kotlin/app/campfire/search/store/SearchSourceOfTruthFactory.kt`

---

## 7. Sleep Timer Pause, Volume Restoration & Freeze Fix

### Summary
Fixed a bug where pausing playback did not freeze the sleep timer countdown on the UI, and resolved an issue where pausing during an active volume fade allowed the background fade job to continue decreasing volume to 0%, causing playback to silence and pause again upon resumption.

### Key Details
- **Immediate Fade Cancellation & Volume Restoration (`cancelFade`)**:
  - Added `cancelFade()` to `AudioPlayer` interface and `ExoPlayerAudioPlayer`.
  - Calling `pause()`, `playPause()`, `stop()`, or handling playback state change cancels the background `fadeJob` immediately and restores audio volume to 100% (`previousVolumeLevel`).
  - In `CoroutineSleepTimerManager.kt`, pausing during a fade immediately cancels the fade job and restores 100% volume. Resuming resets the sleep timer countdown from the beginning at 100% volume.
- **UI Freeze on Pause**:
  - Added `isPaused: Boolean = false` field to `RunningTimer`.
  - In `RunningTimerText.kt` and `SleepTimerButton.kt`, when `isPaused` is true, the timer displays the frozen remaining duration without continuing to subtract elapsed wall-clock time.
- **Reset Pending Resume**:
  - In `CoroutineSleepTimerManager.kt`, pausing playback with `resetTimerOnPauseEnabled` cancels the active countdown job, sets `isPaused = true`, and preserves the original duration so resuming restarts the full countdown.

### Modified Files:
* `infra/audioplayer/api/src/commonMain/kotlin/app/campfire/audioplayer/AudioPlayer.kt`
* `infra/audioplayer/api/src/commonMain/kotlin/app/campfire/audioplayer/model/RunningTimer.kt`
* `infra/audioplayer/public-ui/src/commonMain/kotlin/app/campfire/audioplayer/ui/composables/RunningTimerText.kt`
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/composables/RunningTimerText.kt`
* `infra/audioplayer/public-ui/src/commonMain/kotlin/app/campfire/audioplayer/ui/sleep/SleepTimerButton.kt`
* `infra/audioplayer/impl/src/commonMain/kotlin/app/campfire/audioplayer/impl/sleep/CoroutineSleepTimerManager.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/ExoPlayerAudioPlayer.kt`

---

## 8. Configurable Sleep Timer Fade-Out & Settings UI

### Summary
Added a user-selectable **"Fade out before stopping"** preference under **Settings > Sleep** with 10-second increments (`Off`, `10s`, `20s`, `30s` (Default), `40s`, `50s`, `60s`).

### Key Details
- Backed by `ObservableSettings` property `pref_sleep_fade_out_duration`.
- In `CoroutineSleepTimerManager.kt`, for an Epoch timer of total duration T and fade duration F:
  - Audio plays at 100% volume for (T - F).
  - At (T - F), `player.fadeToPause(duration = F)` is launched.
  - Automatically bounds F <= T / 2 for very short timers (e.g. 1-minute timer).
- Wrapped volume fades in `try ... finally { setVolume(startVolume) }` in `VolumeFadeController.kt` so that tapping pause to reset or shaking immediately cancels the fade and restores 100% volume.

### Modified Files:
* `features/settings/api/src/commonMain/kotlin/app/campfire/settings/api/SleepSettings.kt`
* `features/settings/impl/src/commonMain/kotlin/app/campfire/settings/SleepSettingsImpl.kt`
* `features/settings/ui/src/commonMain/composeResources/values/ui_settings_strings.xml`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/composables/TimeJumpSetting.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/SettingsUiState.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/SettingsPresenter.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/analytics/SettingsAnalyticUiEventHandler.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/panes/SleepPane.kt`

---

## 9. Perceptual (Logarithmic) Audio Fade Curve

### Summary
Human hearing perceives sound level logarithmically (decibels). A linear volume ramp (1.0 -> 0.0) sounds almost full-volume for the first 70% of time and suddenly drops off in the last few seconds. We replaced it with a human-ear calibrated perceptual curve.

### Mathematical Formula
Volume(t) = StartVolume * ((T - t) / T)^2
Where:
- T = total fade duration in milliseconds
- t = elapsed fade time in milliseconds
- StartVolume = initial player volume (1.0)

### Loudness Drop Profile (for 30s fade):
- **30s – 25s remaining**: Volume drops audibly from 100% to ~70% perceived level within 5 seconds (immediate cue).
- **20s remaining**: Down to ~45% volume (ample time to pause or shake).
- **10s remaining**: Down to ~11% volume (soft whisper).
- **0s remaining**: Mutes and pauses cleanly.

### Modified Files:
* `infra/audioplayer/impl/src/commonMain/kotlin/app/campfire/audioplayer/impl/sleep/VolumeFadeController.kt`

---

## 10. Action Confirmations & Warn on Cellular Downloads

### Summary
Added configurable safeguards under **Settings > Downloads** to prevent accidental destructive actions (deleting offline downloads, discarding progress, marking in-progress books as finished) and accidental downloads over mobile data.

### Key Details
- **Settings Added**:
  - **Action Confirmations** (`pref_confirm_actions`, default `true`): Asks for confirmation before deleting downloads, discarding progress, or marking books with active progress as finished.
  - **Warn on Cellular Downloads** (`pref_warn_on_cellular_download`, default `true`): Checks if connected to mobile data / metered network and warns before downloading media.
- **Smart Mark as Finished Popup**:
  - Only prompts if the user has actual progress on the item (`progress > 0` and not already finished). If the book has not been started yet (`progress == 0`), it marks as finished immediately without a popup.
- **Cross-Platform Dialog Support**:
  - Added reusable `ConfirmActionDialog` in `common/compose`.
  - Added `rememberIsCellularOrMetered()` using Android `ConnectivityManager` (and stubbed for other platforms).

### Modified Files:
* `common/compose/src/commonMain/kotlin/app/campfire/common/compose/network/NetworkType.kt`
* `common/compose/src/androidMain/kotlin/app/campfire/common/compose/network/NetworkType.android.kt`
* `common/compose/src/appleMain/kotlin/app/campfire/common/compose/network/NetworkType.apple.kt`
* `common/compose/src/commonMain/kotlin/app/campfire/common/compose/widgets/dialog/ConfirmActionDialog.kt`
* `features/settings/api/src/commonMain/kotlin/app/campfire/settings/api/CampfireSettings.kt`
* `features/settings/impl/src/commonMain/kotlin/app/campfire/settings/CampfireSettingsImpl.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/SettingsUiState.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/SettingsPresenter.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/analytics/SettingsAnalyticUiEventHandler.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/panes/DownloadsPane.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/book/BookPresenter.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/composables/slots/ExpressiveControlSlot.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/podcast/episode/PodcastEpisodeUiState.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/podcast/episode/PodcastEpisodePresenter.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/podcast/episode/PodcastEpisodeBottomSheet.kt`

---

## 11. Shake-to-Reset Sensor Polling, Sensitivity Calibration & Haptic Feedback

### Summary
Fixed the "Shake to reset" feature in **Settings > Sleep** so that shaking the phone reliably and immediately resets the sleep timer:
1. **Sensor Polling Rate**: Upgraded accelerometer listener from `SENSOR_DELAY_NORMAL` (5 Hz, ~200ms) to `SENSOR_DELAY_GAME` (50 Hz, ~20ms). 5 Hz was too slow to capture rapid back-and-forth shake peaks within the 500ms sliding sampling window.
2. **Sensitivity Threshold Calibration**: Corrected the inverted thresholds where "High" had previously set a 15.0 m/s² threshold and "Very Low" set 10.0 m/s². The calibrated values now accurately map higher sensitivity to lower acceleration thresholds:
   - **Very High**: 11.0 m/s² (light flick triggers it)
   - **High**: 12.5 m/s²
   - **Medium**: 13.5 m/s² (balanced default)
   - **Low**: 15.5 m/s²
   - **Very Low**: 17.5 m/s² (requires firm shake)
3. **Haptic Vibration Feedback**: Added an immediate 100ms vibration pulse via `Vibrator` / `VibratorManager` when a shake is detected so the user feels confirmation that the shake was recognized.
4. **Live Dynamic Setting Observation**: `CoroutineSleepTimerManager` now collects `sleepSettings.observeShakeSensitivity()` and `sleepSettings.observeShakeToResetEnabled()` in real-time, instantly applying sensitivity or toggle changes while a timer is counting down.

### Modified Files:
* `infra/shake/src/androidMain/kotlin/app/campfire/shake/SeismicShakeDetector.kt`
* `infra/shake/src/androidMain/kotlin/app/campfire/shake/ShakeDetector.android.kt`
* `infra/shake/src/androidMain/kotlin/app/campfire/shake/ShakeSensitivityMagnitudes.android.kt`
* `infra/audioplayer/impl/src/commonMain/kotlin/app/campfire/audioplayer/impl/sleep/CoroutineSleepTimerManager.kt`

---

## 12. Release Build & Deployment Commands

### Build Command (PowerShell):
```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
$env:ANDROID_HOME = "C:\Android\Sdk"
.\gradlew.bat :app:android:assembleFossRelease
.\gradlew.bat :app:android:assembleStandardRelease
```

### Direct Install via ADB (Samsung Galaxy S24 Ultra):
```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" install -r "C:\Campfire_source_1.05\app\android\build\outputs\apk\foss\release\android-foss-release.apk"
```

---

## 13. Modified Files Inventory

| File Path | Description |
| :--- | :--- |
| `common/compose/.../LibraryItemCard.kt` | Bottom 6dp green/orange progress bar, badge removed |
| `common/compose/.../LibraryItemListItem.kt` | Bottom 4dp progress bar |
| `common/compose/.../network/NetworkType.kt` | Expect/actual for cellular/metered connection |
| `common/compose/.../dialog/ConfirmActionDialog.kt` | Reusable confirmation alert dialog |
| `core/.../Duration.kt` | Added `formatHoursAndMinutes()` extension |
| `features/libraries/ui/.../CoverImageSlot.kt` | Detail page cover art progress bar overlay |
| `features/libraries/ui/.../BookPresenter.kt` | Pass media progress and confirmation settings to slots |
| `features/libraries/ui/.../ExpressiveControlSlot.kt` | Confirmation dialogs for delete, discard, finish, cellular download |
| `features/libraries/ui/.../PodcastPresenter.kt` | Pass media progress to CoverImageSlot |
| `features/libraries/ui/.../PodcastEpisodeBottomSheet.kt` | Confirmation dialogs for episode actions & cellular download |
| `features/libraries/ui/.../TitleSlot.kt` | Display duration and file size in megabytes |
| `features/series/ui/.../SeriesDetailPresenter.kt` | Injected MediaProgressRepository for series |
| `features/series/ui/.../SeriesDetailUiState.kt` | Added mediaProgress map to UI state |
| `features/series/ui/.../SeriesDetailUi.kt` | Passed progress to series LibraryItemCards |
| `features/author/ui/.../AuthorDetailPresenter.kt` | Injected MediaProgressRepository for author |
| `features/author/ui/.../AuthorDetailUiState.kt` | Added mediaProgress map to UI state |
| `features/author/ui/.../AuthorDetailUi.kt` | Passed progress to author LibraryItemCards |
| `features/collections/ui/build.gradle.kts` | Added media-progress:api dependency |
| `features/collections/ui/.../CollectionDetailPresenter.kt` | Injected MediaProgressRepository for collection |
| `features/collections/ui/.../CollectionDetailUiState.kt` | Added mediaProgress map to UI state |
| `features/collections/ui/.../CollectionDetailUi.kt` | Passed progress to collection LibraryItemCards |
| `features/search/ui/build.gradle.kts` | Added media-progress:api dependency |
| `features/search/ui/.../SearchPresenter.kt` | Injected MediaProgressRepository for search |
| `features/search/ui/.../SearchUiState.kt` | Added mediaProgress map to UI state |
| `features/search/ui/.../CampfireSearchComponent.kt` | Connected presenter to search UI |
| `features/search/ui/.../SearchResultContent.kt` | Passed progress to search LibraryItemCards |
| `features/search/impl/.../SearchSourceOfTruthFactory.kt` | Clear stale search records before writing fresh results |
| `features/home/ui/.../HomePresenter.kt` | Replaced Newest Authors with Downloads shelf |
| `features/home/ui/build.gradle.kts` | Added test dependencies |
| `features/home/ui/.../HomePresenterTest.kt` | Updated tests with FakeLibraryItemRepository |
| `infra/audioplayer/api/.../RunningTimer.kt` | Added `isPaused: Boolean` property |
| `infra/audioplayer/public-ui/.../RunningTimerText.kt` | Countdown pause freeze support |
| `features/sessions/ui/.../RunningTimerText.kt` | Countdown pause freeze support in player |
| `infra/audioplayer/public-ui/.../SleepTimerButton.kt` | Timer button pause freeze support |
| `infra/audioplayer/impl/.../CoroutineSleepTimerManager.kt` | Managed pause reset & fade timing |
| `infra/audioplayer/impl/.../VolumeFadeController.kt` | Perceptual quadratic fade & try-finally reset |
| `infra/audioplayer/impl/.../MediaItemBuilder.kt` | Dynamic playlistMetadata for live remaining book time |
| `infra/audioplayer/impl/.../ExoPlayerAudioPlayer.kt` | Real-time minute countdown, replaceMediaItem & speed factor |
| `infra/audioplayer/impl/.../MediaItemBuilderTest.kt` | Unit tests for remaining book time & playback speed |
| `features/settings/api/.../CampfireSettings.kt` | Added `confirmActions` and `warnOnCellularDownload` |
| `features/settings/impl/.../CampfireSettingsImpl.kt` | Persistent storage for `confirmActions` & `warnOnCellularDownload` |
| `features/settings/api/.../SleepSettings.kt` | Added `fadeOutDuration` setting |
| `features/settings/impl/.../SleepSettingsImpl.kt` | Persistent storage for `fadeOutDuration` |
| `features/settings/ui/.../ui_settings_strings.xml` | Added string resources for fade out setting |
| `features/settings/ui/.../TimeJumpSetting.kt` | Added `textFormat` parameter |
| `features/settings/ui/.../SettingsUiState.kt` | Added confirmation settings to DownloadsSettingsInfo |
| `features/settings/ui/.../SettingsPresenter.kt` | Connected confirmation settings to UI & events |
| `features/settings/ui/.../SettingsAnalyticUiEventHandler.kt` | Handled confirmation settings analytics |
| `features/settings/ui/.../DownloadsPane.kt` | Added Action Confirmations and Warn on Cellular toggles |
| `features/settings/ui/.../SleepPane.kt` | Added `FadeOutJumps` enum and TimeJumpSetting |
| `features/settings/test/.../TestCampfireSettings.kt` | Test implementation of confirmActions & warnOnCellularDownload |
| `infra/audioplayer/api/.../AudioPlayer.kt` | Added `cancelFade()` to AudioPlayer interface |
| `infra/shake/.../SeismicShakeDetector.kt` | Default sensor delay updated to SENSOR_DELAY_GAME (50 Hz) |
| `infra/shake/.../ShakeDetector.android.kt` | Added 100ms haptic feedback vibration pulse & game delay |
| `infra/shake/.../ShakeSensitivityMagnitudes.android.kt` | Corrected inverted thresholds (VeryHigh=11.0, VeryLow=17.5) |
