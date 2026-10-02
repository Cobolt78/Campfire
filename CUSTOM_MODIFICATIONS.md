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
12. [Home Screen: Pull-to-Refresh](#12-home-screen-pull-to-refresh)
13. [Reset Sleep Timer on Pause (User Configurable)](#13-reset-sleep-timer-on-pause-user-configurable)
14. [Dynamic Continue Listening Sorting, Download Integration & Finished Book Pruning](#14-dynamic-continue-listening-sorting-download-integration--finished-book-pruning)
15. [Headset & Remote Control Forward/Rewind Time Skips & Dynamic Lock Screen Notification Icons](#15-headset--remote-control-forwardrewind-time-skips--dynamic-lock-screen-notification-icons)
16. [Equalizer Bottom Sheet Layout Polish](#16-equalizer-bottom-sheet-layout-polish)
17. [Native GitHub In-App Updates & Versioning Strategy](#17-native-github-in-app-updates--versioning-strategy)
18. [Clickable Home Shelf Headings, Dedicated Continue Series & Offline Downloads Screens](#18-clickable-home-shelf-headings-dedicated-continue-series--offline-downloads-screens)
19. [In-App "What's New" Changelog Synchronization Rule](#19-in-app-whats-new-changelog-synchronization-rule)
20. [Continue Series Query Optimization & Freeze Fix](#20-continue-series-query-optimization--freeze-fix)
21. [Continue Series Alternate View, Drawer Version Display & Main Thread Performance Optimization (v1.2.4)](#21-continue-series-alternate-view-drawer-version-display--main-thread-performance-optimization-v124)
22. [Home Screen Update Banner, Fast Retained Continue Series & Layout Polish (v1.2.5)](#22-home-screen-update-banner-fast-retained-continue-series--layout-polish-v125)
23. [Instant Continue Series In-Memory Repository Cache & SQL Query Optimization (v1.2.6)](#23-instant-continue-series-in-memory-repository-cache--sql-query-optimization-v126)
24. [Home Continue Series Background Pre-Warming, Discover Shelf Stabilization & Dedicated Refresh Button (v1.2.7)](#24-home-continue-series-background-pre-warming-discover-shelf-stabilization--dedicated-refresh-button-v127)
25. [Discoveries Refresh Server Cache Bypass & Reactive Pipeline Fix (v1.2.8)](#25-discoveries-refresh-server-cache-bypass--reactive-pipeline-fix-v128)
26. [Dynamic In-App Update Alert (v1.2.9)](#26-dynamic-in-app-update-alert-v129)
27. [Discoveries Refresh Persistence & Book Detail Series Navigation (v1.2.11)](#27-discoveries-refresh-persistence--book-detail-series-navigation-v1211)
28. [Version Code Ordering & In-App Upgrade Delivery (v1.2.12)](#28-version-code-ordering--in-app-upgrade-delivery-v1212)
29. [Shared Element Transition Layout Lockup Fix (v1.2.13)](#29-shared-element-transition-layout-lockup-fix-v1213)
30. [ANR Freeze Resolution: Database Offloading & Batch Series Querying (v1.2.14)](#30-anr-freeze-resolution-database-offloading--batch-series-querying-v1214)

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
In Campfire 1.1.0, Android Auto formats the media screen with the Chapter Title on Line 1 (`MediaMetadata.title`), Book Title on Line 2 (`MediaMetadata.artist`), and the chapter scrubber on the progress bar. We enhanced Line 1 by stripping any raw chapter length suffix (e.g., `- 00:20:40` or `(20:40)`) and dynamically appending the live speed-adjusted total book remaining countdown (e.g., `Chapter 134 - 18h 45m left`), counting down minute-by-minute while you drive.

### Key Details
- **Line 1 - Chapter Title + Total Book Countdown**:
  - The chapter title is formatted as `"$cleanTitle - $remainingFormatted left"` (e.g. `Chapter 134 - 18h 45m left`).
  - Existing durations like `- 00:20:40` or `(20:40)` appended to chapter titles from metadata or track tagging are automatically cleaned using `MediaItemBuilder.cleanChapterTitle()`.
- **Preserved Layout Elements**:
  - **Line 2 (Book Title)**: Retains `albumTitle ?: artist` so the book title remains prominently visible beneath the chapter title.
  - **Scrubber Bar**: Continues tracking the active chapter progress (`3:52 / 20:40`).
- **Playback Speed Adjustment**:
  $$\text{Effective Remaining Time} = \frac{\text{Raw Remaining Audio Duration}}{\text{Playback Speed}}$$
  *(e.g., at 1.25x speed, 5 hours of remaining audio displays as 4h 0m left)*.
- **Initial & Dynamic Updates**:
  - `MediaItemBuilder.kt` initializes `title` on the platform `MediaMetadata` with the formatted countdown.
  - `ChapterWindowForwardingPlayer.kt` dynamically formats `chapterPlaylist` titles with `host.remainingFormatted()`.
  - `ExoPlayerAudioPlayer.kt` updates the active item's title via `exoPlayer.replaceMediaItem(currentIndex, newItem)` whenever the minute string updates, actively dispatching changes to Android Auto and system media controls without interrupting playback.

### Modified Files:
* `core/src/commonMain/kotlin/app/campfire/core/extensions/Duration.kt`
* `infra/audioplayer/impl/src/commonMain/kotlin/app/campfire/audioplayer/impl/mediaitem/MediaItemBuilder.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/forwarding/ChapterWindowForwardingPlayer.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/ExoPlayerAudioPlayer.kt`

---

## 6. Search Cache Invalidation (SUPERSEDED by 1.2.0 upstream)

### Summary
## 6. Search Cache Invalidation (SUPERSEDED by 1.2.0 upstream) (SUPERSEDED)

**Status in 1.2.0:** Dropped. Upstream natively fixed this issue ("Titles removed from the server lingering in the app..."). We now use the upstream implementation.

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
- **Reset Timer on Pause (User Configurable)**:
  - Added `resetTimerOnPause` (`pref_sleep_reset_timer_on_pause`, default `false`) setting in **Settings > Sleep** under the *Reset* section.
  - When enabled, pausing playback (via earbuds, lockscreen, notification, or in-app button) cancels the active countdown job, cancels any pending volume fade, and immediately resets `remainingMillis = timer.epochMillis`.
  - When disabled (default), pausing freezes the timer at its current elapsed time and resumes where it left off.

### Modified Files:
* `features/settings/api/src/commonMain/kotlin/app/campfire/settings/api/SleepSettings.kt`
* `features/settings/impl/src/commonMain/kotlin/app/campfire/settings/SleepSettingsImpl.kt`
* `features/settings/test/src/commonMain/kotlin/app/campfire/settings/test/FakeSleepSettings.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/SettingsUiState.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/SettingsPresenter.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/analytics/SettingsAnalyticUiEventHandler.kt`
* `features/settings/ui/src/commonMain/composeResources/values/ui_settings_strings.xml`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/panes/SleepPane.kt`
* `infra/audioplayer/impl/src/commonMain/kotlin/app/campfire/audioplayer/impl/sleep/CoroutineSleepTimerManager.kt`

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
| `infra/audioplayer/impl/.../MediaItemBuilder.kt` | Clean chapter title & format speed-adjusted remaining book time countdown |
| `infra/audioplayer/impl/.../ChapterWindowForwardingPlayer.kt` | Dynamic chapter playlist titles with remaining book countdown |
| `infra/audioplayer/impl/.../ExoPlayerAudioPlayer.kt` | Real-time minute countdown, replaceMediaItem & speed factor |
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
| `features/home/api/.../HomeRepository.kt` | Added `refreshHomeFeed(): Result<Unit>` method |
| `features/home/impl/.../StoreHomeRepository.kt` | Implemented `refreshHomeFeed()` via Store5 `homeStore.fresh(key)` |
| `features/home/ui/.../HomeUiState.kt` | Added `isRefreshing` state property and `HomeUiEvent.Refresh` |
| `features/home/ui/.../HomePresenter.kt` | Added refresh coroutine handling and state flow |
| `features/home/ui/.../HomeUi.kt` | Integrated `PullToRefreshBox` with `CampfireLoadingIndicator` |
| `features/home/ui/.../FakeHomeRepository.kt` | Implemented `refreshHomeFeed()` in test fake |
| `features/home/ui/.../HomePresenterTest.kt` | Unit tests for refresh event trigger and state transitions |
| `features/series/impl/.../SeriesSourceOfTruthFactory.kt` | Single batch query & offloaded reader to databaseRead |
| `features/series/impl/.../StoreSeriesRepository.kt` | Offloaded observeSeriesLibraryItems to databaseRead |
| `features/libraries/ui/.../BookPresenter.kt` | Direct allSeries observation offloaded to databaseRead |

---

## 12. Home Screen: Pull-to-Refresh (SUPERSEDED by 1.2.0 upstream)

### Summary
Added a standard swipe-down Pull-to-Refresh gesture to the main Home feed. This brings the Home screen in line with all other major tabs (Library, Series, Authors, Podcasts) that already support pull-to-refresh, allowing users to effortlessly check for newly added audiobooks, updated podcasts, and refreshed shelves from their Audiobookshelf server without restarting the app.

### Key Details
- **Non-Destructive Network Refresh**:
  - `StoreHomeRepository.refreshHomeFeed()` requests fresh personalized shelves from Audiobookshelf via Store5's `homeStore.fresh(key)`.
  - Wrapped with `runCatching` so that if the device is offline or the server is momentarily unreachable, the refresh simply dismisses the spinner and preserves all existing cached shelves and downloaded books without presenting an error screen.
- **Animated Flame Indicator**:
  - Reuses Campfire's custom animated flame loader (`CampfireLoadingIndicator`) in `HomeUi.kt` via Material 3's `PullToRefreshBox`.
  - Configured with `padding(top = paddingValues.calculateTopPadding())` to seamlessly respect the collapsing search/app bar without nested scroll conflicts.
- **Spamming Guard**:
  - `HomePresenter` checks `if (!isRefreshing)` before launching a refresh coroutine, preventing duplicate network requests from rapid subsequent swipes.

### Modified Files:
* `features/home/api/src/commonMain/kotlin/app/campfire/home/api/HomeRepository.kt`
* `features/home/impl/src/commonMain/kotlin/app/campfire/home/StoreHomeRepository.kt`
* `features/home/ui/src/commonMain/kotlin/app/campfire/home/ui/HomeUiState.kt`
* `features/home/ui/src/commonMain/kotlin/app/campfire/home/ui/HomePresenter.kt`
* `features/home/ui/src/commonMain/kotlin/app/campfire/home/ui/HomeUi.kt`
* `features/home/ui/src/commonTest/kotlin/app/campfire/home/ui/FakeHomeRepository.kt`
* `features/home/ui/src/commonTest/kotlin/app/campfire/home/ui/HomePresenterTest.kt`

---

## 13. Reset Sleep Timer on Pause (User Configurable)

### Summary
Added a user-configurable toggle in **Settings → Sleep** ("Reset timer on pause"). When enabled, pausing audio playback (via headset button, lockscreen widget, notification, or in-app button) automatically resets the active sleep timer back to its initial configured duration. Playback can then be resumed with the full timer ready.

### Key Details
- **Toggle Setting**: Added `resetTimerOnPause` to `SleepSettings` and `CampfireSettings`.
- **Playback State Hook**: Observed player state in `CoroutineSleepTimerManager`. When the player transitions to paused/idle, the countdown resets to the initial duration rather than running down while paused.

---

## 14. Dynamic Continue Listening Sorting, Download Integration & Finished Book Pruning

### Summary
Enhanced the Continue Listening and Listen Again shelves to provide a responsive, Audiobookshelf-like listening experience even offline.

### Key Details
- **Dynamic Last-Played Sorting**: The Continue Listening shelf sorts items in realtime by `lastUpdate` descending. Pausing or playing a book smoothly moves it to the front of the shelf (the #&#8203;1 spot) using Compose item animations (`Modifier.animateItem()`).
- **Downloaded Books Integration**: Downloaded in-progress books appear on the Continue Listening shelf even without server synchronization.
- **Finished Book Pruning & Listen Again**: When an audiobook or podcast episode reaches 100% or is marked finished, it immediately drops off the Continue Listening shelf and is dynamically placed on the **Listen Again** shelf (synthesized client-side if missing from server feed), ordered by most recently finished.
- **Universal Green Checkmark**: Ensures the green completed badge reliably displays on cover cards, detail screens, and library list items for completed books (whether downloaded or streaming).

---

## 15. Headset & Remote Control Forward/Rewind Time Skips & Dynamic Lock Screen Notification Icons

### Summary
Fixed headset/earphone button behavior so that double-clicking (forward) and triple-clicking (back) skips by the configured time interval (e.g. 30s) instead of jumping entire chapters. Added dynamic lock screen and notification action icons that switch between chapter skip icons (`|<` / `>|`) and time jump icons (`↺` / `↻`) in real time based on the user's setting.

### Key Details
- **Settings Toggle**: "Headset & remote next/prev skips chapters" in **Settings → Playback** (`skipChaptersWithHeadset`).
- **ExoPlayer Forwarding Player**: Intercepts `seekToNextMediaItem()` and `seekToPreviousMediaItem()` from wired headsets, Bluetooth remotes, and Android Auto, delegating to `seekForward()` or `seekBack()` when chapter skipping is disabled.
- **System Media Notification**: Updated `AudioPlayerService` and `MediaSessionCallback` to advertise `COMMAND_SEEK_FORWARD` / `COMMAND_SEEK_BACK` as primary actions instead of chapter skips when disabled, updating Android lockscreen icons in real time.

---

## 16. Equalizer Bottom Sheet Layout Polish

### Summary
Widened the control label column in `EqualizerBottomSheet.kt` from `72.dp` to `90.dp`. This prevents words like "Loudness" from wrapping mid-word ("Loudnes-s") onto a second line on devices with large display scalings or accessibility font sizes.

---

## 17. Native GitHub In-App Updates & Versioning Strategy

### Summary
Integrated a fully native in-app updater backed directly by GitHub Releases (`Cobolt78/Campfire`). Replaces Google Play In-App Updates for standard release builds and adds update support to FOSS builds. Users receive visual notifications in the navigation drawer when a new APK is released on GitHub, can read release notes, and install updates with a single tap.

### Architecture & Key Components
- **`GitHubAppUpdateSource.kt`**: Implements `AppUpdateSource` in `app/android/src/main/java/app/campfire/android/updates/`. Replaces `NoOpUpdateSource` via `@ContributesBinding(AppScope::class, replaces = [NoOpUpdateSource::class])`.
- **GitHub API Integration**: Queries `https://api.github.com/repos/Cobolt78/Campfire/releases/latest`, parses tag name/release name for SemVer versioning, compares against installed `applicationInfo.versionCode`, extracts release notes from the release body, and matches the APK asset based on flavor (`standard` vs `foss`).
- **In-App Download & Stream**: Uses `OkHttpClient` to stream the APK download directly to `cacheDir/updates/campfire-update.apk` while reporting live progress (`AppUpdateProgress`) to `AppUpdateWidgetImpl` in the navigation drawer.
- **Android Package Installer Hand-off**: Declares `REQUEST_INSTALL_PACKAGES` permission in `AndroidManifest.xml` and uses `androidx.core.content.FileProvider` (`${applicationId}.update_provider` with `update_file_paths.xml`) to launch `Intent.ACTION_VIEW` targeting Android's native package installer.
- **Drawer Integration**: Integrated directly into `AppUpdateWidgetCard` and `AppUpdateSheet` in `CampfireDrawer`.

### Versioning Strategy & Rules for Future AI Instances
1. **Semantic Versioning Format**: Always use standard SemVer `major.minor.patch` (e.g. `1.2.1`, `1.2.2`, `1.2.3`). Do NOT use 4-part numbers (like `1.2.1.1`) or skip numbers (like `1.2.11`), as standard tools, CI guards, and Campfire's internal SemVer regex expect 3-segment versions.
2. **Version Code Formula**:
   $$\text{versionCode} = \text{major} \times 1,000,000 + \text{minor} \times 10,000 + \text{patch} \times 100 + 99$$
   - `1.2.0` = `1020099`
   - `1.2.1` = `1020199`
   - `1.2.2` = `1020299`
   - `1.2.3` = `1020399`
3. **Strict Monotonic Increase**: Android OS requires incoming APKs to have a strictly higher `versionCode` than the installed version (`incoming > installed`). With every bug fix, patch, or feature release:
   - Increment `campfire.version` and `campfire.versionCode` in `gradle.properties`.
   - Run `gradlew.bat :app:android:verifyVersionCode` to confirm they match.
   - Tag the release on GitHub as `v<version>-custom` (e.g., `v1.2.1-custom`).
   - Include both APK assets: `cobolt-campfire-standard-release_<version>.apk` and `cobolt-campfire-foss-release_<version>.apk`.
4. **Always Update `CHANGELOG.md` with Every Release**: The in-app "What's New" feature (`infra/whats-new/`) is driven at build time by `CHANGELOG.md`. With every release or patch, add a `## [<version>]` section to `CHANGELOG.md` with `### Added`, `### Changed`, or `### Fixed` bullet points describing the changes. This ensures the What's New drawer item and update notification accurately reflect the custom build's enhancements.



---

## 18. Clickable Home Shelf Headings, Dedicated Continue Series & Offline Downloads Screens

### Summary
Added interactive shelf headers with chevron indicators (Heading ›) on the Home screen to bypass the Audiobookshelf server's 5/10-item cap. Users can tap any supported shelf header to view all items with full custom sorting. Built dedicated, beautifully crafted **Continue Series** and **Offline Downloads** screens.

### Key Components & Architecture
- **Interactive Shelf Headers (ShelfHeader.kt, ShelfListItem.kt)**:
  - ShelfHeader.kt: Displays a subtle CampfireIcons.Rounded.ChevronRight icon next to shelf titles when clickable, with a rounded ripple click target.
  - ShelfListItem.kt: Selectively enables header clicks for supported shelves (ContinueListening, ListenAgain, RecentlyAdded, RecentSeries, ContinueSeries, Downloads, UpcomingReleases, NewestAuthors).
- **Home Navigation Routing (HomePresenter.kt)**:
  - RecentlyAdded › -> LibraryScreen(sortMode = AddedAt, sortDirection = Descending).
  - RecentSeries › -> SeriesScreen(sortMode = AddedAt, sortDirection = Descending).
  - ContinueListening › -> LibraryScreen(filter = ContentFilter.Progress(InProgress)).
  - ListenAgain › -> LibraryScreen(filter = ContentFilter.Progress(Finished)).
  - UpcomingReleases › -> UpcomingScreen.
  - NewestAuthors › -> AuthorsScreen.
  - ContinueSeries › -> ContinueSeriesScreen.
  - Downloads › -> DownloadsScreen.
- **Dynamic Sorting Support on Destination Screens**:
  - LibraryScreen.kt & LibraryPresenter.kt: Extended LibraryScreen to accept optional sortMode and sortDirection parameters so navigation can specify an initial sort order without permanently overriding global preferences unless changed by user.
  - SeriesScreen in Screens.kt & SeriesPresenter.kt: Converted SeriesScreen to a data class accepting optional sortMode and sortDirection parameters.
- **Dedicated Continue Series Screen (features/series/ui/continueseries/)**:
  - Reactive ContinueSeriesPresenter streams all user series via seriesRepository.observeAllSeries() and cross-references mediaProgressRepository.observeAllProgress().
  - Accurately filters for series currently in progress (where at least one book is finished or started, and not all books are completed).
  - Identifies the exact **Next Up** book in sequence (preferring an in-progress book, or the next unread book).
  - Rich UI (ContinueSeriesUi.kt):
    - Series card header with series name, progress readout ("X of Y books completed"), and chevron link to the full series detail view.
    - Series progress indicator bar.
    - Prominent **Next Up** book card with cover art, "NEXT UP" badge, book sequence number ("Book #&#8203;3"), title, author, and listening progress bar.
    - Interactive sort menu: Recently Played (last read), Series Name A–Z, Progress %, Date Added, with ascending/descending toggle.
- **Dedicated Offline Downloads Screen (features/libraries/ui/downloads/)**:
  - Reactive DownloadsPresenter monitors offlineDownloadManager.observeAll() for completed downloads and hydrates full LibraryItem and MediaProgress models.
  - Top app bar displays a real-time storage summary: total offline book count and formatted storage footprint (e.g. "12 books • 3.8 GB").
  - List items render book covers, titles, authors, file sizes (e.g. "740 MB"), listening progress indicators, and offline checkmarks.
  - Interactive sort menu: Recently Played, Date Added/Downloaded, Title A–Z, Author A–Z, File Size, with ascending/descending toggle.


---

## 19. In-App "What's New" Changelog Synchronization Rule

### Summary
Established a permanent development rule ensuring that all custom enhancements, fixes, and version releases are systematically documented in `CHANGELOG.md`.

### Mechanism & Build Architecture
- `infra/whats-new/` generates `changelog.json` at build time from `CHANGELOG.md` via `GenerateChangelogTask`.
- `WhatsNewRepositoryImpl` checks `settings.observeLastSeenVersion()` against `applicationInfo.versionName`. When the version increases and contains changes for the platform, the app automatically surfaces the What's New dialog to the user.
- The "What's New" option in the navigation drawer also reads this JSON to display the full version history.
- Starting from `1.2.1`, `1.2.2`, and `1.2.3`, all custom additions are maintained in `CHANGELOG.md`.

---

## 20. Continue Series Query Optimization & Freeze Fix

### Summary
Diagnosed and fixed an Application Not Responding (ANR) freeze when tapping the Continue Series chevron (`›`) on the Home screen. Replaced an expensive, blocking full-library network and database traversal with a high-performance indexed SQLite query executed off the UI thread.

### Root Cause
- `ContinueSeriesPresenter` previously invoked `seriesRepository.observeAllSeries(refresh = true)`.
- In `StoreSeriesRepository`, `observeAllSeries(refresh = true)` triggered `fetchAllPages`, walking all series in the user's library and writing them to SQLite.
- SqlDelight's `SeriesSourceOfTruthFactory` then ran `db.seriesQueries.selectByLibraryId(key.libraryId)` and iterated through every series in the library, executing `db.libraryItemsQueries.selectForSeries(seriesId).awaitAsList()` in an iterative blocking loop on the database.
- For libraries with hundreds of series, this produced hundreds of sequential queries and heavy object mapping. Because this flow was collected with `collectAsState()` and mapped inside the Composable on the Android main thread, it locked the UI thread for multiple seconds, triggering an immediate app freeze and crash.

### Resolution & Architecture
1. **Targeted SQL Query (`series.sq`)**:
   - Added `selectContinueSeries` to `series.sq`:
     ```sql
     selectContinueSeries:
     SELECT series.* FROM series
     LEFT JOIN shelfJoin ON shelfJoin.entityId = series.id AND shelfJoin.shelfId LIKE 'continue-series_%'
     LEFT JOIN seriesBookJoin ON seriesBookJoin.seriesId = series.id
     LEFT JOIN mediaProgress ON mediaProgress.libraryItemId = seriesBookJoin.libraryItemId AND mediaProgress.userId = :userId AND (mediaProgress.progress > 0 OR mediaProgress.isFinished = 1)
     WHERE series.libraryId = :libraryId
       AND (
         series.inProgress = 1
         OR shelfJoin.entityId IS NOT NULL
         OR mediaProgress.libraryItemId IS NOT NULL
       )
     GROUP BY series.id
     ORDER BY COALESCE(series.bookInProgressLastUpdate, series.updatedAt) DESC;
     ```
   - Filters directly in SQLite to the 5–20 series actually in progress (either marked in-progress, present on the personalized continue series shelf, or having listening progress on a book in `mediaProgress`).
2. **Repository Flow (`SeriesRepository.kt` & `StoreSeriesRepository.kt`)**:
   - Added `fun observeContinueSeries(): Flow<List<Series>>`.
   - Hydrates books only for the filtered in-progress series on `dispatcherProvider.databaseRead`, completing in milliseconds.
3. **Presenter Non-Blocking Offload (`ContinueSeriesPresenter.kt`)**:
   - Switched from `observeAllSeries` to `observeContinueSeries()`.
   - Applied `.flowOn(Dispatchers.Default)` to ensure sorting and filtering execute completely off the UI thread.
   - Preserves reading progress metadata (`inProgress`, `bookInProgressLastUpdate`, `firstBookUnreadId`) across mapping layers.

---

## 21. Continue Series Alternate View, Drawer Version Display & Main Thread Performance Optimization (v1.2.4)

### Summary
Added a user-configurable alternate view for Continue Series in Appearance settings, placed the current app version prominently in the main navigation drawer, and eliminated all UI sluggishness / swipe-back stutter across the app by moving heavy Compose state derivation off the main thread onto background coroutines.

### Key Changes
1. **Continue Series Alternate Grid View (`ContinueSeriesUi.kt`, `AppearancePane.kt`)**:
   - Added `continueSeriesAlternateView` boolean toggle to `CampfireSettings` and `AppearancePane.kt`.
   - When enabled, renders a `LazyVerticalGrid` that mirrors the visual layout of "Continue Listening" using `ContinueSeriesGridCard` / `ElevatedContentCard`.
   - Displays the series title and sequence indicator (`#X in [Series Name]`) underneath the book title.
   - Active books currently in progress are cleanly excluded from the grid so the user only sees subsequent unread books in their series.
2. **Main Navigation Drawer App Version Display (`CampfireDrawer.kt`)**:
   - Integrated the application version string (`v1.2.4`) into the bottom row of `CampfireDrawer.kt`, positioned alongside the theme toggle controls.
   - Powered by `ApplicationInfo.versionName` fed into `NavigationPresenter.kt` and `DrawerUiState.kt`.
3. **UI Snappiness & Main Thread Offload (`HomePresenter.kt`, `ContinueSeriesPresenter.kt`)**:
   - Heavy data transformations (shelf item enrichment, progress map lookups, and list sorting) previously executing synchronously inside Composable `remember { derivedStateOf { ... } }` blocks have been transitioned to Kotlin `Flow.combine()` pipelines offloaded to `Dispatchers.Default`.
   - Eliminates UI freezes and multi-second stutters when swiping back to the Home screen or returning from detail views.

---

## 22. Home Screen Update Banner, Fast Retained Continue Series & Layout Polish (v1.2.5)

### Summary
Added a prominent yet dismissible in-app update banner directly at the top of the Home feed, eliminated the 20–30s reload delay on Continue Series by retaining in-memory state and parallelizing database book queries, formatted alternate grid cards to prioritize the book number with automatic marquee scrolling, and upgraded the default list view with an enlarged left poster cover art layout.

### Key Changes
1. **Home Screen Dismissible Update Banner (`HomeUi.kt`, `features/home/ui/build.gradle.kts`)**:
   - Injected `AppUpdateWidget` directly into `@CircuitInject HomeScreen`.
   - Rendered as the top item of the main Home `LazyColumn` directly beneath the top search bar.
   - Fully dismissible via "✕" (persists dismissed version code in `CampfireSettings` to prevent nagging until next release) with animated expand/collapse transitions.
2. **Instant Continue Series Back Navigation & Concurrent Database Loading (`ContinueSeriesPresenter.kt`, `StoreSeriesRepository.kt`)**:
   - Retained `cachedItems` in memory via Slack Circuit's `rememberRetained` so navigating to a book detail and clicking Back renders instantly (0ms latency) without showing a loading spinner or re-querying disk.
   - Refactored `StoreSeriesRepository.observeContinueSeries()` to load series books concurrently using coroutine `async`/`awaitAll()` on `dispatcherProvider.databaseRead`, slashing query execution times.
3. **Alternate Grid: Book Number First & Marquee (`ContinueSeriesUi.kt`)**:
   - Formatted alternate view subtitle to lead with the book number: `Book #X • Series Name`.
   - Applied `Modifier.basicMarquee()` to both book title and series subtitle so long text smoothly scrolls horizontally without truncating.
   - Stripped redundant progress bars from unread next-up books.
4. **Default View: Enlarged Left Poster Cover Layout (`ContinueSeriesUi.kt`)**:
   - Enlarged the Next Up cover art from a 56dp thumbnail to a prominent `80 × 108 dp` book jacket with `10.dp` rounded corners.
   - Arranged metadata in a clean vertical stack beside the cover (Next Up badge, Book number, 2-line title, and author name), removing redundant progress bars.

---

## 23. Instant Continue Series In-Memory Repository Cache & SQL Query Optimization (v1.2.6)

### Summary
Completely eliminated the 15-second loading delay when navigating between the Home Screen and Continue Series. Stored computed series in an in-memory repository cache with `replay = 1` and presenter-level caching that survives leaving the screen, and rewrote the SQLite series query to filter out completed and active series directly at the database level.

### Key Changes
1. **Repository-Level In-Memory Cache with Replay Buffer (`StoreSeriesRepository.kt`)**:
   - Transformed `StoreSeriesRepository.observeContinueSeries()` into a shared flow (`shareIn`) backed by `repositoryScope` with `SharingStarted.WhileSubscribed(stopTimeoutMillis = 300_000)` and `replay = 1`.
   - Caches the loaded series in UserScope memory for up to 5 minutes after leaving the screen.
2. **Presenter Memory Retention Across Screen Instances (`ContinueSeriesPresenter.kt`)**:
   - Added a static `@Volatile memoryCache` to `ContinueSeriesPresenter`.
   - Initialized `cachedItems` directly from `memoryCache`, ensuring the screen renders in **0 ms** without displaying a loading indicator when opened from the Home Screen.
   - Background `LaunchedEffect` continues silently checking for fresh changes and updates the cache without UI interruption.
3. **SQLite Completed & Active Series Exclusion (`series.sq`)**:
   - Rewrote `selectContinueSeries` to use subquery checks:
     - `EXISTS` on unread books: ensures series where all books are marked `isFinished = 1` are excluded directly by SQLite.
     - `NOT EXISTS` on active books: filters out series with actively playing books (`isFinished = 0 AND progress > 0`).
   - Prevents loading dozens of completed series and hundreds of books from storage into memory only to discard them in Kotlin, reducing database execution time from 15 seconds down to milliseconds.

---

## 24. Home Continue Series Background Pre-Warming, Discover Shelf Stabilization & Dedicated Refresh Button (v1.2.7)

### Summary
Enabled active eager background pre-warming of Continue Series directly while viewing the Home feed so tapping Continue Series is always instantaneous (0ms). Stabilized the Discover shelf so random reshuffling never occurs during the session (on scroll or navigating back from books), and added a dedicated animated Refresh button to the Discover shelf header that fetches fresh recommendations on demand and resets horizontal scroll back to the start.

### Key Changes
1. **Home Screen Continue Series Background Pre-Warming (`StoreSeriesRepository.kt`, `HomePresenter.kt`, `features/home/ui/build.gradle.kts`)**:
   - In `StoreSeriesRepository.kt`, configured `continueSeriesFlow` with `SharingStarted.Eagerly` so it initializes immediately.
   - Injected `SeriesRepository` into `HomePresenter.kt` and added a background `LaunchedEffect(Unit)` collecting `seriesRepository.observeContinueSeries()` in `Dispatchers.Default`.
   - Pre-warms the in-memory cache silently while the user browses the Home feed, ensuring tapping "Continue Series" renders immediately with 0ms delay.
2. **Discoveries Shelf Stability (`StoreHomeRepository.kt`, `HomePresenter.kt`)**:
   - Audiobookshelf's `/personalized` endpoint randomizes the Discover shelf on every API call. Store5 was previously requested with `refresh = true` on every user/session emission, wiping and re-inserting `ShelfJoin` rows in SQLite and reshuffling books whenever the user scrolled or navigated back from a book.
   - Changed `StoreHomeRepository.observeHomeFeed()` to default to `refresh = false`, serving deterministic cached shelves across session navigations.
   - In `HomePresenter.kt`, used `rememberRetained` to track `lastRefreshedLibraryId`, performing the API refresh only once upon cold start / initial library load or library switch.
3. **Dedicated Discoveries Refresh Button (`ShelfHeader.kt`, `ShelfListItem.kt`, `HomeUi.kt`, `HomePresenter.kt`, `HomeUiState.kt`)**:
   - Added an animated Refresh icon button (`CampfireIcons.Rounded.Refresh`) to the "Discover" shelf header.
   - When tapped, triggers `HomeUiEvent.RefreshDiscoveries`, causing the icon to smoothly spin indefinitely while `homeRepository.refreshHomeFeed()` runs.
   - When fresh discoveries are received, `ShelfListItem` automatically resets horizontal scroll position back to the first item (`listState.scrollToItem(0)`).

---

## 25. Discoveries Refresh Server Cache Bypass & Reactive Pipeline Fix (v1.2.8)

### Summary
Fixed the Discoveries refresh button on the Home feed so tapping it reliably fetches brand new random recommendations from Audiobookshelf and immediately updates the shelf on screen. In v1.2.7, tapping refresh appeared to do nothing because the Audiobookshelf server caches `/api/libraries/:id/personalized` responses in its internal `ApiCacheManager` without a query timestamp parameter, and Store5's `ShelfStore` wrapped SQLite reads in an unnecessary 5-minute memory cache.

### Key Changes
1. **Audiobookshelf API Server Cache Bypass (`KtorAudioBookShelfApi.kt`)**:
   - Audiobookshelf's server-side `ApiCacheManager` caches personalized endpoints by full URL string (`req.originalUrl`). Consecutive requests to `/api/libraries/:libraryId/personalized` without parameters hit the server's cache and return identical Discover shelf books.
   - Updated `KtorAudioBookShelfApi.getPersonalizedHome` to append a millisecond timestamp parameter (`parameters.append("t", Clock.System.now().toEpochMilliseconds().toString())`).
   - Every refresh request now produces an `ApiCacheManager` cache miss on the Audiobookshelf server, forcing it to generate a fresh, randomized selection of books.
2. **Store5 Reactive SQLite Pipeline Fix (`ShelfStore.kt`, `StoreHomeRepository.kt`)**:
   - `ShelfStore` previously defined a 5-minute memory cache (`MemoryPolicy.builder<Key, List<ShelfEntity>>().setExpireAfterAccess(5.minutes).build()`). Since `ShelfStore` has no network fetcher and reads purely from SQLite (`shelfJoin`), the 5-minute memory cache prevented SQLite database updates from reaching Compose.
   - Removed `cachePolicy` from `ShelfStore.kt` so SQLDelight query flows stream database updates directly.
   - In `StoreHomeRepository.refreshHomeFeed()`, explicitly called `shelfStore.clear()` after `homeStore.fresh()` to guarantee any in-memory store states are evicted.
3. **Smooth Visual Rotation Feedback (`HomePresenter.kt`, `ShelfHeader.kt`)**:
   - Added a minimum 400ms delay in `HomePresenter.kt` during `HomeUiEvent.RefreshDiscoveries` so fast local network responses still produce smooth, satisfying spin feedback.
   - Simplified rotation graphics layer in `ShelfHeader.kt` using `rotationZ = if (isRefreshing) rotation else 0f`.

---

## 26. Performance Overhaul for Startup, Continue Series, and Home Feed (v1.2.10)

### Summary
Fixed cold startup freezes where the app was stuck on a black screen for 10-15 seconds, eliminated the 10-15s delay opening Continue Series via single-pass SQL batch queries, and resolved micro-stutter when scrolling the Home Screen.

### Key Changes
1. **Startup Connection Pool Saturation Fix (`HomePresenter.kt`, `StoreSeriesRepository.kt`)**:
   - Replaced aggressive background database pre-warming on cold launch with lazy on-demand initialization.
2. **Continue Series N+1 Query Elimination (`series.sq`, `StoreSeriesRepository.kt`)**:
   - Replaced dozens of per-series database queries with an optimized batch SQL `IN` query.
3. **Home Feed Background Thread Offload (`HomePresenter.kt`)**:
   - Offloaded shelf sorting and filtering transformations from the Compose main thread to `Dispatchers.Default`.
4. **Pre-release SemVer Offset (`Versioning.kt`)**:
   - Introduced deterministic +100 version code offset for test builds (`-testN`) to prevent `INSTALL_FAILED_VERSION_DOWNGRADE`.

---

## 27. Discoveries Refresh Persistence & Book Detail Series Navigation (v1.2.11)

### Summary
Fixed Discoveries refresh persistence across screen transitions, added dedicated Series navigation beneath Ratings & Reviews on the Book Detail screen for any series book, and resolved SQLite series junction consistency and API fallbacks across the app.

### Key Changes
1. **Discoveries Shelf Refresh Persistence (`StoreHomeRepository.kt`, `HomeFetcherFactory.kt`)**:
   - Cleared `homeStore` and `shelfStore` synchronously on manual refresh so newly randomized recommendations persist when navigating between book detail and home screens.
2. **Book Detail Series Navigation (`BookPresenter.kt`, `SeriesSlot.kt`)**:
   - Added a dedicated "Series" section directly beneath Ratings & Reviews for any book belonging to a series.
   - Guaranteed immediate display of the current book in `SeriesSlot` and seamless navigation to `SeriesDetailScreen` to browse the full series.
3. **Persistent SQLite Series Linkages (`LibraryItemDao.kt`, `LibraryItemMapping.kt`)**:
   - Ingested series and `seriesBookJoin` rows whenever expanded books are saved to SQLite, ensuring series associations persist locally across all screens (Search, Library, Recently Added, Discoveries).
   - Preserved series metadata even when series sequence is unparsed or null.
4. **Series Book Ingestion & Endpoint Fallback (`StoreSeriesRepository.kt`, `KtorAudioBookShelfApi.kt`)**:
   - Fallback to series endpoint book data when minified filtering fails, and fallback from `/api/libraries/$libraryId/series/$seriesId` to `/api/series/$seriesId`.

---

## 28. Version Code Ordering & In-App Upgrade Delivery (v1.2.12)

### Summary
Normalized version code derivation in build-logic to eliminate artificial test build offsets that inverted release ordering. Bumped release to v1.2.12 (versionCode 1021299) to ensure users running pre-release test builds (`v1.2.11-test2`, versionCode 1021202) seamlessly receive the in-app update prompt and can upgrade cleanly.

### Key Changes
1. **Normalized Version Code Derivation (`Versioning.kt`)**:
   - Removed artificial `+100` offset from test build semver parsing so version codes remain monotonically ordered: `rc/test < final release`.
2. **Version Bump to v1.2.12 (`gradle.properties`)**:
   - Bumped `campfire.version` to `1.2.12` and `campfire.versionCode` to `1021299`, which strictly exceeds `1021202` (test2).

---

## 29. Shared Element Transition Layout Lockup Fix (v1.2.13)

### Summary
Fixed an app freeze / deadlock that occurred when opening series books from the Discover shelf on the Home screen. When navigating with a shared element transition (`sharedBounds`), injecting the series slot into the `LazyColumn` mid-transition caused Compose's layout engine to lock up. Added a 500ms delay to allow the shared bounds transition to settle cleanly before slot insertion.

### Key Changes
1. **Transition Settling Delay for Series Slot (`BookPresenter.kt`)**:
   - Attached `.onStart { delay(500) }` to the `combine` flow in `seriesContentState`.
   - Prevents abrupt insertion of `SeriesSlot` (and its image collages) during Jetpack Compose's active `sharedBounds` measurement pass.
2. **Version Bump to v1.2.13 (`gradle.properties`)**:
   - Bumped `campfire.version` to `1.2.13` and `campfire.versionCode` to `1021399`.

---

## 30. ANR Freeze Resolution: Database Offloading & Batch Series Querying (v1.2.14)

### Summary
Diagnosed and permanently resolved the Application Not Responding (ANR) crash that occurred when opening series books from the Discover shelf on the Home screen. From live Android device stack traces (`SIGQUIT` Signal 3 / tombstone dump), thread `main` was discovered completely blocked in `SQLiteConnection.nativeExecuteForCursorWindow` inside `SeriesSourceOfTruthFactory`. The root cause was synchronous database traversal and N+1 query loops executed on the Android UI thread (`AndroidUiDispatcher`) while competing with background write transactions.

### Root Cause
1. In `BookPresenter.kt`, a fallback flow (`resolvedSeriesFlow`) previously invoked `seriesRepository.observeAllSeries(refresh = false)` whenever `allSeries` was empty.
2. In `StoreSeriesRepository.kt` & `SeriesSourceOfTruthFactory.kt`, `observeAllSeries` queried all series in the user's library and iterated through each one sequentially executing `db.libraryItemsQueries.selectForSeries(...).awaitAsList()`.
3. Because the Flow had no `flowOn(dispatcherProvider.databaseRead)` and was collected by `collectAsState()` in Compose, all queries ran on the main UI thread.
4. Concurrently, the expanded item network fetch opened an exclusive write transaction in SQLite (`db.transaction`).
5. The main UI thread deadlocked on `art::ConditionVariable::WaitHoldingLocks` waiting for the SQLite lock, causing Android to declare an ANR after 10,000ms (`Input dispatching timed out`).

### Key Changes
1. **Batch Series Query & Dispatcher Offloading (`SeriesSourceOfTruthFactory.kt`)**:
   - Replaced iterative N+1 SQLite queries with a single batch query: `db.libraryItemsQueries.selectForMultipleSeries(userId, seriesIds).awaitAsList()`.
   - Enclosed all query execution and mapping within `withContext(dispatcherProvider.databaseRead)`.
   - Attached `.flowOn(dispatcherProvider.databaseRead)` to the returned Flow to guarantee it never executes on the UI thread.
2. **Repository Flow Offloading (`StoreSeriesRepository.kt`)**:
   - Appended `.flowOn(dispatcherProvider.databaseRead)` to `observeSeriesLibraryItems` so all sorting, deduplication, and database streaming execute off the main thread.
3. **Clean Series Observation & Removed Fragile Delays (`BookPresenter.kt`)**:
   - Reverted `seriesContentState` to cleanly observe `allSeries` directly, eliminating the dangerous `observeAllSeries` fallback.
   - Removed the artificial `.onStart { delay(500) }` workaround that caused inconsistent UI delays and failed to prevent the ANR.
   - Attached `.flowOn(dispatcherProvider.databaseRead)` to `combine` to ensure background computation.
4. **Version Bump to v1.2.14 (`gradle.properties`)**:
   - Bumped `campfire.version` to `1.2.14` and `campfire.versionCode` to `1021499`.




