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
31. [Book Detail Series Slot Repositioning (v1.2.15)](#31-book-detail-series-slot-repositioning-v1215)
32. [Series Position Number Parsing & Polish (v1.2.16)](#32-series-position-number-parsing--polish-v1216)
33. [Global Database Lock Resolution (v1.2.17)](#33-global-database-lock-resolution-v1217)
34. [Series Book Number Badge on Item Cards (v1.2.18)](#34-series-book-number-badge-on-item-cards-v1218)
35. [Downloaded Indicator Modernization (v1.2.18)](#35-downloaded-indicator-modernization-v1218)
36. [Porting Custom Features to Upstream 1.2.1 Base & Metro DI (v1.2.18)](#36-porting-custom-features-to-upstream-121-base--metro-di-v1218)
37. [Series Badge Refinement & Crash Fix (v1.2.20)](#37-series-badge-refinement--crash-fix-v1220)
38. [Desktop App Stability, Database Concurrency & Session Restore Guard (v1.2.20)](#38-desktop-app-stability-database-concurrency--session-restore-guard-v1220)
39. [Audit & Comprehensive Feature Restoration (v1.2.20)](#40-audit--comprehensive-feature-restoration-v1220)
40. [Upcoming Releases: 28-Day Freshness Window & Delta Series Scanning (v1.2.20)](#41-upcoming-releases-28-day-freshness-window--delta-series-scanning-client-side)
41. [Audit & Comprehensive Feature Restoration (v1.2.20)](#40-audit--comprehensive-feature-restoration-v1220)
42. [Audiobookshelf Server-Synced Audible Series Tracking & Completion Filters (v1.2.21)](#42-audiobookshelf-server-synced-audible-series-tracking--completion-filters-v1221)
43. [Downloaded Indicator on Continue Series Cards (v1.2.22)](#43-downloaded-indicator-on-continue-series-cards-v1221)
44. [Eliminate Book Details to Home Screen Back Navigation Lag (v1.2.22)](#44-eliminate-book-details-to-home-screen-back-navigation-lag-v1221)
45. [Audiobookshelf Playback Progress Sync & Server Session Cleanup (v1.2.22)](#45-audiobookshelf-playback-progress-sync--server-session-cleanup-v1221)
46. [Audiobookshelf Real-Time Open Listening Sessions & Progress Sync Lifecycle (v1.2.22)](#46-audiobookshelf-real-time-open-listening-sessions--progress-sync-lifecycle-v1221)
47. [Cellular Download Check Simplification & False-Positive Warning Resolution (v1.2.22)](#47-cellular-download-check-simplification--false-positive-warning-resolution-v1221)
48. [Playback & Navigation Bar Refinements: Fast Forward, Zero Gap & Card Clearance (v1.2.23)](#48-playback--navigation-bar-refinements-fast-forward-zero-gap--card-clearance-v1223)
49. [Instant Background Paging Refresh & Blank Cover Measurement Fix (v1.2.23)](#49-instant-background-paging-refresh--blank-cover-measurement-fix-v1223)
50. [Multi-Device Playback Sync False-Positive Suppression & Tolerance Threshold (v1.2.24)](#50-multi-device-playback-sync-false-positive-suppression--tolerance-threshold-v1224)
51. [Listen Again: Sort by Last Listened (v1.2.24)](#51-listen-again-sort-by-last-listened-v1224)
52. [Multi-Tier Resilient Fallback for Cover Thumbnails (v1.2.24)](#52-multi-tier-resilient-fallback-for-cover-thumbnails-v1224)
53. [Paging Stabilization: Card Reshuffle Removal & Non-Destructive Background Refresh (v1.2.25)](#53-paging-stabilization-card-reshuffle-removal--non-destructive-background-refresh-v1225)
54. [Home Shelf Auto-Scroll Reset on Content Update (v1.2.25)](#54-home-shelf-auto-scroll-reset-on-content-update-v1225)
55. [Voice Pitch Adjustment in Equalizer (v1.2.26)](#55-voice-pitch-adjustment-in-equalizer-v1226)
56. [Uniform Segmented Scope Selector (Global vs. This Book) in Equalizer & Speed (v1.2.26)](#56-uniform-segmented-scope-selector-global-vs-this-book-in-equalizer--speed-v1226)
57. [Granular Playback Skip Forward & Backward Duration Sliders (v1.2.27)](#57-granular-playback-skip-forward--backward-duration-sliders-v1227)
58. [Wired Headset 3.5mm Multi-Click Detection & Navigation Drawer String Fix (v1.2.28)](#58-wired-headset-35mm-multi-click-detection--navigation-drawer-string-fix-v1228)
59. [Dual Book-Time Readouts & Action Row Alignment Fix (v1.2.29)](#59-dual-book-time-readouts--action-row-alignment-fix-v1229)
60. [Google Assistant & Gemini Hands-Free Voice Controls & Enlarged Book-Time Readouts (v1.2.30)](#60-google-assistant--gemini-hands-free-voice-controls--enlarged-book-time-readouts-v1230)
61. [Android Auto & Notification Dual Progress Hierarchy (v1.2.31)](#61-android-auto--notification-dual-progress-hierarchy-v1231)
62. [Android Auto Single-File Chaptered Book Title Restoration (v1.2.32)](#62-android-auto-single-file-chaptered-book-title-restoration-v1232)

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
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/sheets/sleeptimer/SleepTimerBottomSheet.kt`
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
| `features/series/ui/.../ContinueSeriesPresenter.kt` | Observe offline download states for next-up books |
| `features/series/ui/.../ContinueSeriesUi.kt` | Render OfflineStatusIndicator on list & grid cards |
| `features/series/ui/.../ContinueSeriesUiState.kt` | Added offlineStates map to UI state |
| `features/libraries/ui/.../LibraryItemUi.kt` | Removed SharedElementTransitionScope & sharedBounds for instant back navigation |
| `features/libraries/ui/.../CoverImageSlot.kt` | Removed SharedElementTransitionScope & sharedElement for instant back navigation |
| `features/libraries/ui/.../TitleSlot.kt` | Removed SharedElementTransitionScope & sharedBounds for instant back navigation |
| `core/.../settings/ContentSortMode.kt` | Added `LastListened` ("Last Listened", mapping to `progress`) |
| `features/filters/ui/.../SortModeBottomSheet.kt` | Mapped `LastListened` to "Last Listened" string resource |
| `features/libraries/ui/.../LibraryPresenter.kt` | Keyed `rememberRetainedSaveable(screen)` for reliable sort/filter adoption |
| `core/.../image/CoverUrls.kt` | Added `CoverUrls.raw(url)` raw cover URL generator |
| `app/common/.../image/CoverSizingInterceptor.kt` | Multi-tier fallback for thumbnail covers (sized -> original -> raw) |
| `features/libraries/impl/.../LibraryItemRemoteMediator.kt` | Instant background paging refresh (`LAUNCH_INITIAL_REFRESH`) |
| `features/series/impl/.../SeriesRemoteMediator.kt` | Instant background paging refresh (`LAUNCH_INITIAL_REFRESH`) |
| `features/author/impl/.../AuthorsRemoteMediatorFactory.kt` | Instant background paging refresh (`LAUNCH_INITIAL_REFRESH`) |
| `features/podcasts/impl/.../RecentEpisodesRemoteMediator.kt` | Instant background paging refresh (`LAUNCH_INITIAL_REFRESH`) |
| `features/sessions/ui/.../PlaybackPresenter.kt` | 20s sync threshold & playing state echo suppression |
| `features/settings/ui/.../PlaybackPane.kt` | Granular DurationSliderSetting for forward and backward skip times |
| `common/compose/.../Forward.kt` | Symmetrical clockwise circular arrow loop matching Replay |
| `common/compose/.../Forward15.kt` | Dedicated 15s forward circular loop vector icon |
| `common/compose/.../Replay15.kt` | Dedicated 15s rewind circular loop vector icon |
| `features/sessions/ui/.../TimeSkipIcons.kt` | Added Forward15 and Replay15 mappings |
| `features/sessions/ui/.../BookTimeProgressIndicator.kt` | Dual book-time readouts enlarged by 20% to 13.2sp base and 14sp accelerated |
| `features/sessions/ui/.../PlaybackSpeedAction.kt` | Standard IconButton alignment with single-line 16sp Bold speed text |
| `features/sessions/ui/.../RunningTimerAction.kt` | Standard IconButton alignment with single-line 16sp Bold timer text |
| `app/android/src/main/res/xml/shortcuts.xml` | App Actions voice capabilities for Google Assistant & Gemini |
| `app/android/src/main/java/app/campfire/android/MainActivity.kt` | Handle MEDIA_PLAY_FROM_SEARCH voice intents and bind MediaController |
| `infra/audioplayer/impl/.../MediaSessionCallback.kt` | Voice search query resolution against mediaTree & playback resumption |

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

---

## 31. Book Detail Series Slot Repositioning (v1.2.15)

### Summary
Repositioned the "Series" section on the Book Detail view (`BookPresenter.kt`). Previously, the Series slot was rendered beneath the "Ratings & Reviews" section (`CommunitySlot`). Moved the Series section directly above "Ratings & Reviews", placing it between the "Summary" section (`SummarySlot`) and "Ratings & Reviews". This provides immediate, natural context for series books and quicker access to other books in the series without having to scroll past community reviews.

### Key Changes
1. **Slot Reordering (`BookPresenter.kt`)**:
   - Placed `seriesContentState.onLoaded { ... }` immediately following `SummarySlot` and ahead of `communityInfoState?.let { ... }`.
2. **Version Bump to v1.2.15 (`gradle.properties`)**:
   - Bumped `campfire.version` to `1.2.15` and `campfire.versionCode` to `1021599`.

---

## 32. Playback Database Throttling & High CPU / Battery Drain Resolution (v1.2.16)

### Summary
Diagnosed and resolved a major performance regression causing 39%–46% CPU utilization and rapid battery drain during audiobook playback on modern multi-core devices (tested on Samsung Galaxy S24 Ultra). Thread inspection using live ADB profiling revealed four coroutine worker threads (`DefaultDispatcher-worker-1` through `worker-4`) pinned at 75%–91% CPU each during active playback, dropping immediately to 0% when playback was paused.

### Root Cause
1. In `ExoPlayerAudioPlayer.kt`, an unthrottled 500ms ticker continuously dispatched position updates to `PlaybackSynchronizer`s.
2. In `MediaProgressPlaybackSynchronizer.kt`, every 500ms tick invoked `mediaProgressRepository.updateProgress(..., skipUpload = true)` with a SQLite write transaction.
3. In `LocalSessionUpdateSynchronizer.kt`, every 500ms tick invoked `sessionsRepository.updateCurrentTime(libraryItemId, overallTime)` with another SQLite write transaction.
4. Each SQLite write transaction invalidated SQLDelight table queries (including `selectContinueSeries` which performs 3 subqueries across all series in the library, and `HomePresenter` shelf sorting). As a result, all four background worker threads were constantly re-evaluating heavy database queries twice per second.

### Key Changes
1. **Throttled Progress Synchronization (`MediaProgressPlaybackSynchronizer.kt`)**:
   - Injected `FatherTime` from `AppScope`.
   - Throttled `onOverallTimeChanged` progress database writes to once every 15 seconds (`PROGRESS_UPDATE_INTERVAL_MS = 15_000L`) during active playback.
   - Guaranteed immediate synchronous flush (`force = true`) on pause, stop, and completion.
2. **Throttled Local Session Current Time Updates (`LocalSessionUpdateSynchronizer.kt`)**:
   - Throttled `onOverallTimeChanged` session time updates to once every 5 seconds (`CURRENT_TIME_UPDATE_INTERVAL_MS = 5_000L`).
   - Cached `lastKnownOverallTime` and immediately persisted it when state changes to `Paused`, `Disabled`, or `Finished`.
   - Limited `remoteSessionsUpdater.update()` calls to the 5-second interval instead of every 500ms.

---

## 33. Android Auto & Dedicated Controller Chapter Skip Restoration (v1.2.16)

### Summary
Fixed an issue where Android Auto next and previous track buttons only jumped forward or back 30 seconds instead of skipping chapters, despite Android Auto already having dedicated 30-second fast-forward and rewind buttons on its interface.

### Root Cause
In commit `2e65be6`, `isRemoteControllerRequest` logic was omitted, causing `!settings.remoteNextPrevSkipsChapters` to unconditionally hijack `COMMAND_SEEK_TO_NEXT` and `COMMAND_SEEK_TO_PREVIOUS` into interval seeks for all controllers indiscriminately (including Android Auto and the media notification).

### Key Changes
1. **Remote Controller Identification (`RemoteControlForwardingPlayer.kt`)**:
   - Restored and centralized `MediaSession?.isRemoteControllerRequest(appPackageName: String): Boolean`.
   - Exempts Android Auto (`isAutoCompanionController`) and media notifications (`isMediaNotificationController`) since both provide dedicated fast-forward and rewind buttons.
2. **Chapter Forwarding Integration (`ChapterWindowForwardingPlayer.kt`)**:
   - Updated `handleSeek` for `COMMAND_SEEK_TO_NEXT` and `COMMAND_SEEK_TO_PREVIOUS` to check `remoteJumpPreferred()`.
   - Added `private fun remoteJumpPreferred(): Boolean = session.isRemoteControllerRequest(appPackageName) && !settings.remoteNextPrevSkipsChapters`.
   - Ensures Android Auto next/previous track controls always skip chapters while Bluetooth car stereos without seek buttons respect the user preference.
3. **Version Bump to v1.2.16 (`gradle.properties`)**:
   - Bumped `campfire.version` to `1.2.16` and `campfire.versionCode` to `1021699`.

---

## 34. Series Book Number Badges on Covers (v1.2.17)

### Summary
Added a book sequence number badge (`#1`, `#2`, `#2.5`) directly on the top-left corner of each book cover in the Series Detail view (`SeriesDetailUi.kt`). When browsing a series, users can immediately identify the volume number and reading order at a glance without having to check the detail screen.

### Key Changes
1. **Reusable Series Badge (`LibraryItemCard.kt`)**:
   - Created `@Composable fun SeriesBadge(badge: String, modifier: Modifier = Modifier, large: Boolean = true)`.
   - Rendered as a sleek, semi-transparent dark pill badge (`scrim.copy(0.68f)`) with bold white text and rounded corners (`CircleShape`), providing high contrast across all light and dark cover artwork.
3. **Series Badge Ordering (`LibraryItemCard.kt`)**:
   - In `ProgressDecorator`, arranged `SeriesBadge` and `MediaFinishedIndicator` in a `Row` at `Alignment.TopStart` with the `#N` badge placed *before* the checkmark (`[#1] [✓]`).
   - If unread: only `[#1]` is shown.
   - If finished: `[#1]` followed by `[✓]`.
   - The bottom edge remains reserved for the Audiobookshelf-style 6dp progress bar.
4. **Missing Series Books Parity (`MissingSeriesBookCard.kt`)**:
   - Added `SeriesBadge` in `TopStart` on missing book covers matching their catalog position (`#${formatPosition(position)}`), providing unified visual consistency across both owned and unowned books in the series.

---

## 35. Downloaded Indicator Modernization (v1.2.18)

### Summary
Modernized the offline downloaded status indicator across all book cards from the confusing white cloud (`CloudDone`) to the standard downward arrow into a tray (`CampfireIcons.Rounded.Download`). The icon is tinted vibrant finished green (`#4CAF50` / `Color.Green`) with a semi-transparent dark circle backing (`scrim.copy(0.68f)`) for maximum contrast and readability across light and dark cover artwork.

### Key Changes
1. **Icon & Tint Replacement (`LibraryItemCard.kt`)**:
   - Replaced `CampfireIcons.Filled.CloudDone` with `CampfireIcons.Rounded.Download`.
   - Updated icon tint to `Color(0xFF4CAF50)` matching the completed checkmark and finished progress bar.

---

## 36. Porting Custom Features to Upstream 1.2.1 Base & Metro DI (v1.2.18)

### Summary
Cleanly ported all custom enhancements and critical performance bug fixes from fork `v1.2.17` onto the official upstream `1.2.1` release base (`05939f58`), adopting upstream's new Metro dependency injection architecture (`dev.zacsweers.metro`) across all feature presenters, UI screens, and update sources. Obsolete workarounds (such as artificial layout delays and custom decibel volume calculation) were retired in favor of upstream's native decibel volume fading (`VolumeFadeController`).

### Key Architecture Updates
1. **Metro Dependency Injection**:
   - Converted all custom and extended presenters (`ContinueSeriesPresenter`, `DownloadsPresenter`, `SettingsPresenter`, `HomePresenter`) to use Metro DI annotations (`@CircuitInject`, `@Inject constructor`).
   - Replaced Tatarka/Kimchi DI imports with `dev.zacsweers.metro.*`.
2. **Settings API Decomposition**:
   - Adapted custom preferences (`resetTimerOnPause`, `confirmActions`, `warnOnCellularDownload`, `continueSeriesAlternateView`, `dismissedUpdateVersionCode`) to upstream's decomposed settings architecture (`SleepSettings`, `LibraryViewSettings`, `DownloadsSettings`, `AppStateSettings`).
3. **GitHub In-App Updates**:
   - Contributed `GitHubAppUpdateSource` via Metro `@ContributesBinding(AppScope::class, replaces = [NoOpUpdateSource::class])`.
   - Disabled conflicting Play Store and Firebase update source bindings on standard releases.
4. **Version Bump to v1.2.20 (`gradle.properties`)**:
   - Set `campfire.version=1.2.20` and `campfire.versionCode=1022099`.

---

## 37. Series Badge Refinement & Crash Fix (v1.2.20)

### Summary
Refined the series book number badge (`#N`) on book cover art to reduce visual footprint while preserving legibility. Replaced the bulky circular bubble with a compact rounded rectangular pill (`RoundedCornerShape(4.dp)`), tightened padding and corner offsets, and calibrated text sizing (11sp on standard cards, 10sp on small grids). Handled missing `google-services.json` gracefully on custom release builds.

### Key Changes
1. **Series Badge Footprint & Positioning (`LibraryItemCard.kt`, `MissingSeriesBookCard.kt`)**:
   - Replaced `CircleShape` with `RoundedCornerShape(4.dp)`.
   - Reduced corner inset margin from `8.dp` to `5.dp` start and `5.dp` top, hugging the corner neatly without intruding on cover art.
   - Reduced scrim opacity to `0.60f` for subtle transparency.
   - Calibrated font sizing to `11.sp` (standard) and `10.sp` (small grids/lists), with bold weight and `0.sp` tracking.
2. **Crash Prevention (`app/android/build.gradle.kts`)**:
   - Gracefully handled absent `google-services.json` on custom build flavors to avoid runtime startup crashes.

---

## 38. Desktop App Stability, Database Concurrency & Session Restore Guard (v1.2.20)

### Summary
Addressed runtime freezes and startup crashes on the desktop client caused by background upcoming series scans, SQLite database lock contention, atomic file flushing under Windows, and unauthenticated session reads during session restoration.

### Key Changes
1. **Upcoming Scan Threading & Flow Conflation (`InProcessDiscoverScanTracker.kt`)**:
   - Offloaded disk/network scans for upcoming books to `Dispatchers.IO` so the UI/AWT thread is never blocked.
   - Conflated the cached upcoming flow to prevent flood-induced UI stalls.
2. **Desktop SQLite Concurrency (`DesktopSqliteDriver.kt`, `BookInfoDatabaseComponent.jvm.kt`)**:
   - Configured SQLite WAL (`Write-Ahead Logging`) mode and a 5,000ms busy timeout on desktop to prevent lock contention between concurrent readers and writers.
3. **Windows Atomic File Flush Fallback (`FileSystemPreferences.kt`)**:
   - Implemented an atomic move with a `REPLACE_EXISTING` retry fallback to resolve `AccessDeniedException` during preference flush on Windows file systems.
4. **Session Restore & Non-LoggedIn User Safety (`UserSession.kt`, `StoreUserRepository.kt`)**:
   - Added `UserSession.NeedsAuthentication` handling to `UserSession.serverUrl`, `userId`, and `user` extensions, safely extracting `server.url`, `server.user.id`, and `server.user`.
   - Guarded `StoreUserRepository.userFlow` and `observeCurrentUser()` to return cached user state immediately when not logged in, eliminating unauthenticated `/api/me` network calls and preventing uncaught `StoreReadResponse.Error` exceptions on the Compose event loop.
5. **Audible Product URL International Redirect Override (`AudibleBookInfoProvider.kt`)**:
   - Appended `ipRedirectOverride=1` and `overrideBaseCountry=1` parameters to Audible product page URLs to prevent unwanted geo-redirection.

---

## 39. Existing Account Re-Authentication & Desktop Session Restore Fallback (v1.2.20)

### Summary
Fixed a lockout bug where users attempting to log in on a previously registered server encountered the error `"You're already signed in to this server as <user>. Switch to that account, or sign out of it first."` despite entering the identical username. Additionally added an automatic session restore fallback in `DatabaseUserSessionRestorer` to recover the active account from the database if `deviceSettings.currentUserId` was lost or unset in `config.properties`.

### Root Cause
1. **False Positive in `DefaultAuthRepository.authenticate`**:
   - When entering credentials on the login screen (`userId == null`), the app called `checkServerAvailable(serverUrl)`.
   - `checkServerAvailable` checked only whether the server URL was present in `serverRepository.getAllServers()`. If found, it immediately failed with `AuthException.ServerAlreadyAdded(existing.user.name)`, blocking the *same* user from logging back into their account.
2. **Missing Fallback in `DatabaseUserSessionRestorer`**:
   - If `deviceSettings.currentUserId` was null in `config.properties` (e.g. from an unclean shutdown, cleared preferences, or missing key), `restore()` immediately returned `UserSession.LoggedOut`, dropping the desktop app onto the welcome login screen even though `campfire.db` still had a valid server and user record.
3. **Out-of-Sync Desktop Shortcut**:
   - The desktop shortcut `Campfire.lnk` pointed to an older build in `C:\Campfire Desktop\bin\Campfire.exe` from an earlier packaging format, bypassing newer fixes.

### Key Changes
1. **Same-User Re-Authentication (`DefaultAuthRepository.kt`)**:
   - In `authenticate(serverUrl, serverName, username, password, userId, ...)`:
     When `userId == null`, inspect `existingServer = serverRepository.getAllServers().firstOrNull { it.url.isSameServerAs(serverUrl) }`.
     If `existingServer` exists and `existingServer.user.name.equals(username, ignoreCase = true)`, resolve `targetUserId = existingServer.user.id`.
     Only throw `ServerAlreadyAdded` if `existingServer` belongs to a *different* username, preserving the one-account-per-server invariant while allowing seamless re-login for the account owner.
   - Verified with unit test `signing into an existing account with the same username is allowed even if userId is null` in `DefaultAuthRepositoryTest.kt`.
2. **Database Fallback for Current User ID (`UserSessionRestorer.kt`)**:
   - If `deviceSettings.currentUserId` is null, query `db.serversQueries.selectAll(::ServerWithUser).awaitAsList().firstOrNull()?.userId`.
   - If found, recover the user ID and persist it to `deviceSettings.currentUserId = it`, smoothly restoring `UserSession.LoggedIn` (if token exists) or `UserSession.NeedsAuthentication` (prompting password re-auth) instead of getting stranded in `LoggedOut`.
   - Verified with unit test `restores existing account when current user id setting is missing` in `DatabaseUserSessionRestorerTest.kt`.
3. **Desktop Packaging & Shortcut Synchronization**:
   - Re-packaged distributable via `:app:desktop:createDistributable`.
   - Mirrored the output to `C:\Campfire Desktop`.
   - Updated `C:\Users\David\Desktop\Campfire.lnk` to point directly to `C:\Campfire Desktop\Campfire.exe`.

---

## 40. Audit & Comprehensive Feature Restoration (v1.2.20)

### Summary
Conducted a thorough section-by-section audit of all custom features from `v1.2.17` against the ported `1.2.20` codebase. Restored all missing and regressed functionality including destructive action confirmations, headset/remote media button controls, cellular download warnings, navigation drawer What's New entry, series and progress mapping invariants, and home feed cache busting.

### Restored Implementations
1. **Destructive Action Confirmations & Cellular Download Warnings**:
   - Restored `ConfirmActionDialog.kt` across common and platform targets (`NetworkType.kt`, `NetworkType.android.kt`, `NetworkType.apple.kt`, `NetworkType.jvm.kt`).
   - Wired `confirmActionsSetting` and `warnOnCellularDownloadSetting` into `ExpressiveControlSlot.kt` for book details (deleting download, discarding progress, marking finished, downloading on cellular) and podcast episode sheets (`PodcastEpisodeBottomSheet.kt`, `PodcastEpisodePresenter.kt`, `PodcastEpisodeUiState.kt`).
2. **Headset & Remote Next/Prev Skip Settings & Logic**:
   - Updated setting labels in `ui_settings_strings.xml` to `"Headset & remote next/prev skips chapters"` to explicitly include wired headsets and external remotes alongside Bluetooth devices.
   - Refined `RemoteControlForwardingPlayer.kt` so media buttons from wired headsets (dispatched via the app's local MediaController) are not falsely rejected. Only notification and Android Auto controllers (which have dedicated seek buttons) are excluded from the chapter skip setting.
3. **Progress State Invariants & Instant Local DB Updates**:
   - Restored `MediaProgress.isCompleted` (evaluating `isFinished`, `progress >= 0.99f`, `finishedAt > 0`, or playback time within 2s of duration).
   - Restored immediate local database updates (`markFinished` and `deleteLocalProgress`) in `StoreMediaProgressRepository.kt` before remote API network calls for zero UI lag and instant offline responsiveness.
   - Restored `MediaProgressBar.kt` completed checks and safe fallback to `"Finished"`.
4. **Series Persistence & Join Query Restoration**:
   - Restored `DbSeries.asDomainModel` mapping in `SeriesMapping.kt` (`inProgress`, `hasActiveBook`, `hideFromContinueListening`, `bookInProgressLastUpdate`, `firstBookUnreadId`).
   - Restored series and `SeriesBookJoin` inserts in `LibraryItemDao.kt` (`insert(expanded)` and `insertBookDomain`).
   - Restored fallback to `SeriesSequence.UNKNOWN_SEQUENCE` in `LibraryItemMapping.kt`.
5. **Navigation Drawer "What's New" Item**:
   - Added `ChangelogScreen` entry with `CampfireIcons.Rounded.NewReleases` to `NavigationPresenter.kt` secondary navigation items.
   - Added drawer string resources `nav_whats_new_label` and `nav_whats_new_content_description` in `ui_drawer_strings.xml`.
6. **Home Feed Cache Busting & Fresh Store Purge**:
   - Passed `bustCache = true` in `HomeFetcherFactory.kt` on `api.getPersonalizedHome(key.libraryId)` to bypass Audiobookshelf server's internal home feed caching.
   - Purged in-memory `homeStore.clear()` and `shelfStore.clear()` on `refreshHomeFeed()` in `StoreHomeRepository.kt` to ensure instant propagation on pull-to-refresh.

---

## 41. Upcoming Releases: 28-Day Freshness Window & Delta Series Scanning (Client-Side)

### Summary
Optimized the Upcoming Releases scanner to eliminate redundant Audible API network traffic, prevent battery drain, and respect rate limits. Extended the scan freshness window from 1 hour to 28 days, matched individual series cache TTL to 28 days, and implemented intelligent delta scanning so that only new or updated series are scanned when entering the Upcoming screen within the freshness window.

### Key Details
1. **28-Day Freshness Window**:
   - Updated `SCAN_FRESHNESS_TTL = 28.days` in `DiscoverScanStateStore.kt` (previously `1.hours`).
   - Updated `SERIES_INFO_CACHE_TTL = 28.days` in `CachedSeriesInfo.kt` (previously `7.days`) so cached individual series entries from Audible remain fresh across the full 28-day cycle.
2. **Dual-Key Timestamp Tracking (`fullAtKey` vs `atKey`)**:
   - `DiscoverScanStateStore.kt` tracks `fullAtKey` (`discover_full_scan_at_${userId}`) for full library scans alongside `atKey` (`discover_scan_at_${userId}`) for all scan checkpoints.
   - A delta scan updates `atKey` to mark the latest inspection time without postponing the 28-day expiration timer for full library rescans.
3. **Delta Series Scanner**:
   - Added `SeriesScanner.hasNewOrUpdatedSeries(sinceEpochMillis: Long): Boolean` to inspect whether any library series have `addedAt > sinceEpochMillis || updatedAt > sinceEpochMillis`.
   - Updated `SeriesScanner.scan(refresh: Boolean, sinceEpochMillis: Long? = null)` to filter and scan only series added/modified since `sinceEpochMillis` when provided.
4. **Android WorkManager & In-Process Coordination**:
   - In `InProcessDiscoverScanTracker.kt` and `AndroidDiscoverScanTracker.kt`, `startScanIfStale()` checks `store.isFresh(completed)`. If fresh, it launches/enqueues a delta scan (`isDelta = true`).
   - If no new or updated series exist, `InProcessDiscoverScanTracker` and `DiscoverScanWorker` abort immediately without setting progress state or posting foreground notifications, ensuring instantaneous opening and zero notification flicker.
   - If new series exist, only those series are scanned against Audible, emitting focused progress and persisting discovered books immediately.

### Modified Files:
* `data/bookinfo/impl/src/commonMain/kotlin/app/campfire/bookinfo/store/CachedSeriesInfo.kt`
* `features/discover/impl/src/commonMain/kotlin/app/campfire/discover/DiscoverScanStateStore.kt`
* `features/discover/impl/src/commonMain/kotlin/app/campfire/discover/SeriesScanner.kt`
* `features/discover/impl/src/commonMain/kotlin/app/campfire/discover/InProcessDiscoverScanTracker.kt`
* `features/discover/impl/src/androidMain/kotlin/app/campfire/discover/AndroidDiscoverScanTracker.kt`
* `features/discover/impl/src/androidMain/kotlin/app/campfire/discover/DiscoverScanWorker.kt`
* `features/discover/impl/src/commonTest/kotlin/app/campfire/discover/InProcessDiscoverScanTrackerTest.kt`
* `features/discover/impl/src/commonTest/kotlin/app/campfire/discover/SeriesScannerTest.kt`

---

## 42. Audiobookshelf Server-Synced Audible Series Tracking & Completion Filters (v1.2.21)

### Summary
Wired Campfire client to the new Audiobookshelf server Audible series endpoints (`/api/libraries/:id/upcoming-releases`, `/api/libraries/:id/missing-books`, `/api/series/:id/provider-entries`, `/api/libraries/:id/series-provider/scan`), adding Series completion filter options (`Has Missing Books`, `Complete`, `Has Upcoming Releases`) and sort options (`Missing Books Count`, `Upcoming Release Date`), while maintaining 100% backwards compatibility with upstream vanilla ABS servers via automatic fallback to local 28-day scanning.

### Key Details
1. **Series Completion Filters & Sorting**:
   - Added `ContentFilter.Completion` with `HasMissing`, `Complete`, `HasUpcoming` to `core/src/commonMain/kotlin/app/campfire/core/filter/ContentFilter.kt`.
   - Added `ContentSortMode.MissingBooks` (`missingBooks`) and `ContentSortMode.UpcomingDate` (`upcomingDate`) to `core/src/commonMain/kotlin/app/campfire/core/settings/ContentSortMode.kt`.
   - Enabled completion filtering in series filter bottom sheet (`ContentFilterBottomSheet.kt`, `ContentFilterPresenter.kt`, `ContentFilterUiState.kt`).
   - Enabled sort options in `SortModeBottomSheet.kt`.
   - Added strings in `features/filters/ui/src/commonMain/composeResources/values/filters_ui_strings.xml`.
2. **Network DTOs & API Methods**:
   - Created `SeriesProviderEntryDto.kt` in `data/network/api/src/commonMain/kotlin/app/campfire/network/models/SeriesProviderEntryDto.kt` modeling upcoming releases, missing books, entries, and scan status.
   - Added `getUpcomingReleases`, `getMissingBooks`, `getSeriesProviderEntries`, `getSeriesProviderStatus`, `startSeriesProviderScan`, and `cancelSeriesProviderScan` to `AudioBookShelfApi.kt` and `KtorAudioBookShelfApi.kt`.
3. **Database & Cache Sync (`SeriesInfoStore` & `BookInfoRegistry`)**:
   - Added `SeriesInfoStore.putServerEntries(userId, providerId, seriesName, series)` to write server-scraped series entries directly into `seriesInfoCacheQueries`.
   - Added `syncServerUpcomingAndMissing` to `BookInfoRegistry` and `DefaultBookInfoRegistry.kt`: queries `/api/libraries/:id/upcoming-releases` and `/api/libraries/:id/missing-books`, writes directly into cache, and returns `true` on success.
4. **Adaptive Fallback in `SeriesScanner`**:
   - `SeriesScanner` checks `syncServerUpcomingAndMissing` first. If server returns upcoming entries, it marks progress as `isServerSynced = true` and finishes instantly with zero mobile battery drain.
   - If the server returns 404 (vanilla ABS upstream server without the Audible provider extension), `syncServerUpcomingAndMissing` returns `false`, and `SeriesScanner` immediately falls back to its 28-day local delta scanning against the Audible catalog.
5. **UI Scan Source Indicator**:
   - `DiscoverScanState.Completed` tracks `isServerSynced: Boolean`.
   - `DiscoverScanStateStore` persists `isServerSynced` per user.
   - `UpcomingUi.kt` top app bar displays "Synced from server" or "Scanned on device" under "Upcoming" when a scan has completed.

### Modified Files:
* `core/src/commonMain/kotlin/app/campfire/core/filter/ContentFilter.kt`
* `core/src/commonMain/kotlin/app/campfire/core/settings/ContentSortMode.kt`
* `data/bookinfo/api/src/commonMain/kotlin/app/campfire/bookinfo/api/BookInfoRegistry.kt`
* `data/bookinfo/impl/build.gradle.kts`
* `data/bookinfo/impl/src/commonMain/kotlin/app/campfire/bookinfo/DefaultBookInfoRegistry.kt`
* `data/bookinfo/impl/src/commonMain/kotlin/app/campfire/bookinfo/store/SeriesInfoStore.kt`
* `data/bookinfo/test/src/commonMain/kotlin/app/campfire/bookinfo/test/FakeBookInfoRegistry.kt`
* `data/network/api/src/commonMain/kotlin/app/campfire/network/AudioBookShelfApi.kt`
* `data/network/api/src/commonMain/kotlin/app/campfire/network/models/SeriesProviderEntryDto.kt` (new)
* `data/network/impl/src/commonMain/kotlin/app/campfire/network/KtorAudioBookShelfApi.kt`
* `data/network/test/src/commonMain/kotlin/app/campfire/network/test/FakeAudioBookShelfApi.kt`
* `features/discover/api/src/commonMain/kotlin/app/campfire/discover/api/DiscoverScanTracker.kt`
* `features/discover/impl/build.gradle.kts`
* `features/discover/impl/src/commonMain/kotlin/app/campfire/discover/DiscoverScanStateStore.kt`
* `features/discover/impl/src/commonMain/kotlin/app/campfire/discover/SeriesScanner.kt`
* `features/discover/ui/src/commonMain/composeResources/values/discover_ui_strings.xml`
* `features/discover/ui/src/commonMain/kotlin/app/campfire/discover/ui/UpcomingUi.kt`
* `features/filters/ui/src/commonMain/composeResources/values/filters_ui_strings.xml`
* `features/filters/ui/src/commonMain/kotlin/app/campfire/filters/ui/filters/ContentFilterBottomSheet.kt`
* `features/filters/ui/src/commonMain/kotlin/app/campfire/filters/ui/filters/ContentFilterPresenter.kt`
* `features/filters/ui/src/commonMain/kotlin/app/campfire/filters/ui/filters/ContentFilterUiState.kt`
* `features/filters/ui/src/commonMain/kotlin/app/campfire/filters/ui/sort/SortModeBottomSheet.kt`
* `features/libraries/impl/src/commonMain/kotlin/app/campfire/libraries/db/FilteredItemQueryHelper.kt`

---

## 43. Continue Series Downloaded Indicator in List & Grid Views (v1.2.21)

### Summary
Added offline downloaded status indicators (`OfflineStatusIndicator`) to book covers in the dedicated Continue Series screen (`ContinueSeriesUi.kt`) across both the standard list card view and the alternate grid card view. Users can now immediately identify which next-up series books are downloaded to the device without having to open individual book detail screens.

### Key Details
1. **Presenter & State Ingestion (`ContinueSeriesUiState.kt`, `ContinueSeriesPresenter.kt`)**:
   - Added `offlineStates: Map<LibraryItemId, OfflineDownload> = emptyMap()` to `ContinueSeriesUiState`.
   - Injected `OfflineDownloadManager` into `ContinueSeriesPresenter`.
   - Used `snapshotFlow` over `cachedItems` to dynamically observe `offlineDownloadManager.observeForItems(books)` for all `nextUpBook` items with flow distinct-until-changed conflation.
2. **Visual Parity across List & Grid Views (`ContinueSeriesUi.kt`)**:
   - Added `OfflineStatusIndicator` with size `18.dp` at `Alignment.TopEnd` with `padding(6.dp)` on the book cover poster in `ContinueSeriesCard` (list view).
   - Added `OfflineStatusIndicator` with size `18.dp` at `Alignment.TopEnd` with `padding(8.dp)` on the cover box in `ContinueSeriesGridCard` (alternate grid view).
   - Mapped `state.offlineStates[item.nextUpBook.id].asWidgetStatus()` to both cards, rendering the standard green download arrow on dark scrim circle for downloaded books, progress wheels for active downloads, and nothing when not downloaded.

### Modified Files:
* `features/series/ui/src/commonMain/kotlin/app/campfire/series/ui/continueseries/ContinueSeriesPresenter.kt`
* `features/series/ui/src/commonMain/kotlin/app/campfire/series/ui/continueseries/ContinueSeriesUi.kt`
* `features/series/ui/src/commonMain/kotlin/app/campfire/series/ui/continueseries/ContinueSeriesUiState.kt`

---

## 44. Eliminate Book Details to Home Screen Back Navigation Lag (v1.2.21)

### Summary
Resolved a noticeable 1–3 second freeze/lag when navigating back from a Book Details screen to the Home screen. By removing Jetpack Compose's shared element transitions (`sharedBounds` on the main page scaffold and book title, and `sharedElement` on the cover image), navigation back to the Home screen now executes instantly via smooth, native slide animations.

### Key Details
1. **Root Cause Analysis**:
   - The Home screen is a large Compose tree comprised of a nested `LazyColumn` containing 8+ horizontal `LazyRow` shelves with dozens of cards, Coil 3 image loaders, and `.animateItem()` modifiers.
   - When entering or exiting `LibraryItemScreen`, Compose's `SharedElementTransitionLayout` attempted to calculate, measure, and coordinate shared bounds across the entire Book Detail hierarchy and the Home feed's lazy rows in parallel.
   - On exit/back-navigation, this coordination blocked the main thread for 1 to 3 seconds while re-measuring off-screen or animating items.
2. **Implementation (`LibraryItemUi.kt`, `CoverImageSlot.kt`, `TitleSlot.kt`)**:
   - Removed `SharedElementTransitionScope` wrapper from `LibraryItemContent` in `LibraryItemUi.kt`.
   - Removed `.sharedBounds(...)` modifier from the root `Scaffold` in `LibraryItemUi.kt`.
   - Removed `SharedElementTransitionScope` and `.sharedElement(...)` from `CoverImageSlot.kt`.
   - Removed `SharedElementTransitionScope` and `.sharedBounds(...)` from `TitleSlot.kt`.
   - Maintained `sharedTransitionKey: String = ""` parameter defaults in `CoverImageSlot` and `TitleSlot` constructors to ensure 100% binary/source compatibility with presenters and tests.
   - Preserved `LibraryItemScreen(libraryItemId, sharedTransitionKey)` so navigation callers remain completely untouched.
   - Transitions back to the Home screen now take < 100ms with zero stutter.

### Modified Files:
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/LibraryItemUi.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/composables/slots/CoverImageSlot.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/composables/slots/TitleSlot.kt`

---

## 45. Audiobookshelf Playback Progress Sync & Server Session Cleanup (v1.2.21)

### Summary
Fixed critical regressions affecting progress tracking, "Continue Listening" visibility on the Audiobookshelf server dashboard, zombie listening sessions on the server, and false-positive chapter metadata error banners.

### Key Details
1. **Re-Enabled Direct Media Progress Sync (`MediaProgressPlaybackSynchronizer.kt`)**:
   - Upstream commit `9c5c51ddd` had retired the direct progress PATCH by passing `skipUpload = true` to `mediaProgressRepository.updateProgress`.
   - For downloaded/local books, the server never opened a streaming `/play` session, relying exclusively on bulk local session batch syncs (`POST /api/session/local-all`).
   - The ABS server enforces a strict client-clock recency guard on `POST /api/session/local-all`, silently ignoring progress if `userProgressForItem.updatedAt > session.updatedAt`. Furthermore, `local-all` requires `timeListening > 5s`.
   - Re-enabled direct progress upload (`skipUpload = false`). When paused or during periodic 15-second intervals, the unconditional `PATCH /api/me/progress/{id}` is transmitted to the server, instantly updating the user's progress and displaying the book under "Continue Listening" on the Audiobookshelf web and dashboard interfaces.
2. **Zombie Server Session Cleanup & Inactive Session End (`RemoteSessionsUpdater.kt`, `Session.kt`, `SqlDelightSessionDataSource.kt`)**:
   - Added `isActive: Boolean` to the domain `Session` model mapped from `DbSession.isActive`.
   - In `RemoteSessionsUpdater.syncServerSession`, updated `isEnded` to `val isEnded = session.isFinished || session.isDeleted || !session.isActive`.
   - When a user stops playback or switches to another book, the previous session becomes inactive (`!session.isActive`). If it has an attached `serverSessionId`, `api.closePlaybackSession` is now reliably dispatched to the server (with any remaining positive `timeListened` delta) and the local session row is deleted from SQLite.
   - If `api.closePlaybackSession` returns HTTP 404 (session already expired on server), the row is also deleted from the local database.
   - This eliminates zombie streaming sessions from lingering in the Audiobookshelf server's active "Listening Sessions" list.
3. **Listening Time Accumulation Interval Alignment (`LocalSessionUpdateSynchronizer.kt`)**:
   - Reduced `MAX_TIME_LISTENING_INTERVAL` from `1.minutes` to `15.seconds` (matching `syncIntervalUnmetered = 15.seconds`).
   - Flush accumulated listening time to the database prior to triggering periodic session syncs. Server sessions now smoothly advance their listening time in real-time instead of appearing stuck at 0 seconds for the first minute.
4. **Chapter Tolerance & False-Positive Banner Fix (`LibraryItemValidatorImpl.kt`)**:
   - Audiobooks frequently have chapter metadata where the final chapter end timestamp slightly exceeds container audio track duration due to AAC framing, metadata rounding, or container padding.
   - Previously, `floor(chapter.end).seconds > totalDuration` caused standard, completely valid audiobooks to display a warning banner: *"This item has misaligned chapter metadata and may not play back as expected"*.
   - Added a standard rounding tolerance (`chapter.start.seconds > totalDuration + 2.seconds || floor(chapter.end).seconds > totalDuration + 5.seconds`), eliminating false-positive warning banners while still catching genuinely corrupt chapter metadata.
5. **Release Logcat Output (`CampfireApplication.kt`)**:
   - Enabled `Heartwood.grow(AndroidBark())` in release builds with `LogRedaction.enabled = !BuildConfig.DEBUG`, allowing diagnostic logging in `adb logcat` during device testing.

### Modified Files:
* `core/src/commonMain/kotlin/app/campfire/core/model/Session.kt`
* `features/sessions/impl/src/commonMain/kotlin/app/campfire/sessions/db/SqlDelightSessionDataSource.kt`
* `features/sessions/impl/src/commonMain/kotlin/app/campfire/sessions/sync/MediaProgressPlaybackSynchronizer.kt`
* `features/sessions/impl/src/commonMain/kotlin/app/campfire/sessions/sync/RemoteSessionsUpdater.kt`
* `features/sessions/impl/src/commonMain/kotlin/app/campfire/sessions/sync/LocalSessionUpdateSynchronizer.kt`
* `features/libraries/impl/src/commonMain/kotlin/app/campfire/libraries/LibraryItemValidatorImpl.kt`
---

## 46. Audiobookshelf Real-Time Open Listening Sessions & Progress Sync Lifecycle (v1.2.21)

### Summary
Resolved Audiobookshelf server integration issues: (1) downloaded playback (e.g. *USS Jefferson*) not appearing under "Open Listening Sessions" on the ABS server dashboard, (2) streaming/local sessions remaining stuck at `0 sec` listened / static timestamp, and (3) lingering zombie sessions failing to close when stopped or when switching books.

### Key Changes
1. **Downloaded Playback Open Listening Sessions (`ServerSessionAttacher.kt`)**:
   - Removed the artificial early-exit `if (session.playMethod == PlayMethod.Local) return` in `ServerSessionAttacher.attachAsync`.
   - Downloaded books played locally now attach a Direct Play server session via `api.startPlaybackSession(..., forceDirectPlay = true)` when online.
   - The Audiobookshelf server administrator dashboard now displays real-time user activity under "Open Listening Sessions" (`Direct Play | Campfire 1.2.21 | Device | X min | Position | Active`), matching the behavior of apps like ShelfPlayer on iOS.
   - When offline, `startPlaybackSession` times out or fails gracefully, ensuring offline playback proceeds uninterrupted and falls back to batch sync when reconnected.
2. **Session Database Update Unblocking (`session.sq`, `SqlDelightSessionDataSource.kt`)**:
   - Removed `AND isActive = 1;` from `updatePlayback`, `updateLastPlayed`, and `addTimeListening` queries in `session.sq`. Because `(userId, libraryItemId)` is the primary key of the `session` table, requiring `isActive = 1` caused all progress, listening time, and timestamp updates to silently drop whenever a session had its `isActive` flag cleared.
   - In `SqlDelightSessionDataSource.createOrStartSession`, fixed `hydrateSession(existingSession.copy(isActive = true))` so reused existing sessions return with `isActive = true` rather than retaining a stale `isActive = false` from prior deactivations.
3. **Session Lifecycle Close & Sync Triggers (`DefaultSessionsRepository.kt`, `LocalSessionUpdateSynchronizer.kt`, `RemoteSessionsUpdater.kt`)**:
   - In `DefaultSessionsRepository`, injected `RemoteSessionsUpdater` and invoked `remoteSessionsUpdater.update(skipInterval = true)` on `createSession`, `stopSession`, and `markFinished`. This ensures that when a user switches books, stops playback, or finishes an audiobook, the old session is closed immediately on the Audiobookshelf server via `/api/session/{id}/close` and deleted from SQLite, eliminating lingering zombie sessions.
   - In `LocalSessionUpdateSynchronizer.onStateChanged`, ensured that pausing or stopping playback triggers `remoteSessionsUpdater.update(skipInterval = true)` immediately.
   - In `LocalSessionUpdateSynchronizer.onOverallTimeChanged`, initialized `lastPlayedTime = now` if it was null, preventing listening time tracking gaps.
   - In `RemoteSessionsUpdater.update`, if a sync job is already in progress and `skipInterval = true`, calls `currentSyncJob.join()` so session closing requests are never dropped.
4. **404 Orphaned Session Hydration Unblocking (`SqlDelightSessionDataSource.kt`)**:
   - Fixed a critical crash where `hydrateSession` called `libraryItemRepository.getLibraryItem(session.libraryItemId)` unconditionally. If an item had been deleted or purged from the library on the server, `getLibraryItem` threw an unhandled `ApiException: API Error [404]`.
   - Because `getSessions(userId)` called `dbSessions.map { hydrateSession(it) }`, any single orphaned row aborted the entire `syncLocalSessionsToServer()` pass, preventing all active sessions from syncing to the ABS server (causing the "0 sec / stuck timestamp" bug).
   - Wrapped item retrieval in `try/catch`: if the item 404s/fails, the orphaned session is cleanly purged from the SQLite database via `db.sessionQueries.delete(...)` and returns `null`. `getSessions` uses `mapNotNull` to gracefully unblock all subsequent active sessions.
5. **Release Logcat & Observability (`base-proguard-rules.pro`, `RemoteSessionsUpdater.kt`)**:
   - Commented out `-assumenosideeffects class android.util.Log { ... }` in `base-proguard-rules.pro`, restoring logcat output in release builds for device diagnostics.
   - Promoted server session sync logs from `dbark` to `ibark` (`Synced Xs (pos=Ys) into server session <id>`) so periodic sync heartbeats are verifiable in live device logcat.

### Modified Files:
* `features/sessions/impl/src/commonMain/kotlin/app/campfire/sessions/sync/ServerSessionAttacher.kt`
* `data/db/core/src/commonMain/sqldelight/app/campfire/data/session.sq`
* `features/sessions/impl/src/commonMain/kotlin/app/campfire/sessions/DefaultSessionsRepository.kt`
* `features/sessions/impl/src/commonMain/kotlin/app/campfire/sessions/db/SqlDelightSessionDataSource.kt`
* `features/sessions/impl/src/commonMain/kotlin/app/campfire/sessions/sync/LocalSessionUpdateSynchronizer.kt`
* `features/sessions/impl/src/commonMain/kotlin/app/campfire/sessions/sync/RemoteSessionsUpdater.kt`
* `app/android/base-proguard-rules.pro`

---

## 47. Cellular Download Check Simplification & False-Positive Warning Resolution (v1.2.21)

### Summary
Resolved an issue where tapping Download on a book while connected to home Wi-Fi triggered a false-positive "Mobile Data Warning" dialog whenever the phone had mobile data enabled.

### Root Cause
During the v1.2.21 feature restore (commit `ecac076c`), `rememberIsCellularOrMetered` was refactored with a live `ConnectivityManager.NetworkCallback` using `cm.registerNetworkCallback(request, callback)` with a general `NET_CAPABILITY_INTERNET` request. On modern Android devices (especially Samsung Galaxy devices), the cellular modem maintains an active background standby connection even while connected to Wi-Fi. Because `registerNetworkCallback` scans all active hardware interfaces on the phone rather than the system's active default network, Android dispatched capabilities for the standby cellular radio, falsely setting `isCellularOrMetered = true`.

### Key Changes
1. **Reverted to Battle-Tested Snapshot Check (`NetworkType.android.kt`)**:
   - Removed the background `NetworkCallback`, `DisposableEffect`, and `registerNetworkCallback` scanner.
   - Restored the rock-solid, zero-callback implementation from v1.2.20 that directly inspects `cm.activeNetwork` and its capabilities when evaluated.
   - Completely eliminates false-positive warnings when connected to home Wi-Fi, produces zero background radio listener overhead, and correctly warns only when the active default connection is cellular or metered.
2. **Added 'Propose, Don't Sneak' Refactoring Invariant (`GEMINI.md`)**:
   - Added Section 5 to `GEMINI.md` mandating strict blast-radius discipline during bug fixes and migrations (no unsolicited architectural refactors or rewriting working code without prior user proposal and approval).

### Modified Files:
* `common/compose/src/androidMain/kotlin/app/campfire/common/compose/network/NetworkType.android.kt`
* `GEMINI.md`

---

## 48. Playback & Navigation Bar Refinements: Fast Forward, Zero Gap & Card Clearance (v1.2.23)

### Summary
Enhanced the collapsed playback bar and bottom navigation system with three core UI/UX refinements:
1. Added a user-configured fast-forward button to the mini playback bar.
2. Eliminated the floating gap under the mini player when scrolling hides the bottom navigation bar.
3. Unified scroll-to-hide behavior across menus and ensured full clearance so bottom cards are never cut off.

### Key Changes
1. **Mini Playback Bar Fast-Forward (`CollapsedPlaybackBar.kt`)**:
   - Added `ForwardIcon` directly adjacent to `RewindIcon` on the mini player: `[Cover] [Title] [Rewind] [Fast Forward] [Play/Pause]`.
   - Tied directly to user-configured skip intervals (30s / 10s / 5s) and emits `PlayerUiEvent.FastForwardClick`.
2. **Docked Mini Player & Normalized Fraction Math (`CampfireNavigationBarState.kt`)**:
   - Replaced raw search bar pixel offsets with a normalized collapse fraction $[0.0, 1.0]$. As scrolling down hides the navigation bar, the mini player smoothly and completely translates to `bottomBarOffset` directly at the bottom of the screen with zero gap.
3. **Uniform Scroll-to-Hide Navigation Across All Menus**:
   - Switched `ContinueSeriesUi` and `DownloadsUi` to `adaptiveEnterAlwaysScrollBehavior` and attached them to `LocalNavigationBarState` (with support added for `TopAppBarScrollBehavior`), unifying scroll-to-hide behavior across Home, Library, Downloads, and Continue Series.
4. **List/Grid Card Clearance at Bottom**:
   - Added `CampfireNavigationBarWindowInsets` to `contentWindowInsets` on `ContinueSeriesUi`, `DownloadsUi`, and `SeriesUi`, ensuring the final book card in lists or grids has complete padding clearance and is never obscured behind bottom chrome.

### Modified Files:
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/playback/collapsed/CollapsedPlaybackBar.kt`
* `ui/navigation/api/src/commonMain/kotlin/app/campfire/ui/navigation/bar/CampfireNavigationBarState.kt`
* `features/series/ui/src/commonMain/kotlin/app/campfire/series/ui/continueseries/ContinueSeriesUi.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/downloads/DownloadsUi.kt`
* `features/series/ui/src/commonMain/kotlin/app/campfire/series/ui/list/SeriesUi.kt`

---

## 49. Instant Background Paging Refresh & Blank Cover Measurement Fix (v1.2.23)

### Summary
Resolved stale data/book counts when opening "Recently Added" and other paged views, and resolved book detail blank cover art display errors.

### Root Cause
1. **Stale 24-Hour Paging Cache Lock**:
   - Upstream commit `93c4027b` introduced a 24-hour cache limit (`MAX_CACHE_TIME_MS = 24L * 60L * 60 * 1000L`).
   - If a page had been viewed within 24 hours, `RemoteMediator.initialize()` returned `SKIP_INITIAL_REFRESH`.
   - When tapping "Recently Added" from Home, Android Paging 3 was prohibited from contacting the network, loading only the stale SQLite database snapshot from hours earlier (e.g. 21,770 books instead of 21,787).
2. **Unconstrained Dimension Collapse on Book Details**:
   - `CoverImageSlot` computed cover size as `minWidth * 0.75f`. Inside unconstrained scrollable layouts, `minWidth` could evaluate to `0.dp`, resulting in a collapsed cover measurement and displaying the fallback placeholder.

### Key Changes
1. **Immediate Background Refresh (`RemoteMediator`)**:
   - Changed `initialize()` across `LibraryItemRemoteMediator`, `SeriesRemoteMediator`, `AuthorsRemoteMediatorFactory`, and `RecentEpisodesRemoteMediator` to return `InitializeAction.LAUNCH_INITIAL_REFRESH` (Google Jetpack Paging standard).
   - Local database cache emits in 0ms for instant UI display, while page 0 refreshes asynchronously in the background (~25 KB payload), immediately reflecting newly added books and accurate total counts without requiring manual pull-to-refresh.
2. **Book Detail Cover Sizing & Click-to-Retry**:
   - Updated `CoverImageSlot` to calculate size from `maxWidth` clamped to at least `CoverImageSize` (256dp).
   - Added error logging via `bark` and enabled click-to-retry (`painter.restart()`) on the placeholder image in `CoverImage.kt`.

### Modified Files:
* `features/libraries/impl/src/commonMain/kotlin/app/campfire/libraries/paging/LibraryItemRemoteMediator.kt`
* `features/series/impl/src/commonMain/kotlin/app/campfire/series/paging/SeriesRemoteMediator.kt`
* `features/author/impl/src/commonMain/kotlin/app/campfire/author/paging/AuthorsRemoteMediatorFactory.kt`
* `features/podcasts/impl/src/commonMain/kotlin/app/campfire/podcasts/paging/RecentEpisodesRemoteMediator.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/detail/composables/slots/CoverImageSlot.kt`
* `common/compose/src/commonMain/kotlin/app/campfire/common/compose/widgets/CoverImage.kt`

---

## 50. Multi-Device Playback Sync False-Positive Suppression & Tolerance Threshold (v1.2.24)

### Summary
Resolved an issue where the mini-player and expanded player intermittently displayed a "Sync available" popup ("Update to mm:ss (-00:02)") that vanished within 2–4 seconds during active playback.

### Root Cause
1. In v1.2.21, real-time Audiobookshelf session tracking and progress reporting were restored (`skipUpload = false`, `MAX_TIME_LISTENING_INTERVAL = 15.seconds`). Every 15 seconds during active playback, the phone sends current progress to the server.
2. The Audiobookshelf server broadcasts `user_item_progress_updated` over WebSockets back to all user clients, including the sending device.
3. In `PlaybackPresenter.kt`, upstream's `derivedStateOf` evaluated `sessionValue.currentTime.inWholeSeconds != mediaProgress.currentTime.seconds.inWholeSeconds` with zero tolerance.
4. Due to network transit time (1–2 seconds round-trip), local playback had continuously advanced 1–2 seconds ahead of the position reported in the echoed packet. This minor 1–2 second difference was falsely flagged as a remote multi-device desync, turning the collapsed playback bar green and flashing the sync prompt until the next local database tick cleared it.

### Key Changes
1. **20-Second Significance Threshold (`PlaybackPresenter.kt`)**:
   - Replaced zero-tolerance inequality check with `delta.absoluteValue >= 20.seconds`. Minor timestamp rounding and network latency differences under 20 seconds are ignored.
2. **Active Playback Echo Suppression (`PlaybackPresenter.kt`)**:
   - Passed `playerState: AudioPlayer.State` to `observeSyncState`. When `playerState == AudioPlayer.State.Playing`, any server progress update lagging behind local playback (`delta < 0`) is suppressed as an own-device echo.
   - Legitimate multi-device progress differences (20+ seconds ahead, or 20+ seconds behind while paused) continue to prompt for sync as intended.
3. **Unit Test Verification (`PlaybackPresenterTest.kt`)**:
   - Added unit test cases verifying that progress differences below 20 seconds produce no sync prompt, that playing state suppresses lagging remote progress, and that differences $\ge$ 20 seconds ahead successfully surface `AvailableSync`.

### Modified Files:
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/playback/PlaybackPresenter.kt`
* `features/sessions/ui/src/commonTest/kotlin/app/campfire/sessions/ui/playback/PlaybackPresenterTest.kt`

---

## 51. Listen Again: Sort by Last Listened (v1.2.24)

### Summary
Added a "Last Listened" sort mode to the Library view and wired the Home screen "Listen Again" shelf header to open the completed books list sorted by last listened date in descending order (most recently listened/finished books first, down to the oldest listened books).

### Key Changes
1. **Sort Mode Definition (`ContentSortMode.kt`)**:
   - Added `ContentSortMode.LastListened("last-listened", "progress", Mode.Numerical)` to core settings.
   - Added `LastListened` to `LibraryItemSortModes.availableModes`.
   - Backward-compatible deserialization maps `"finished-at"` to `LastListened`.
2. **Audiobookshelf Backend Mapping**:
   - Audiobookshelf's server natively parses `progress` in `libraryItemsBookFilters.js` line 291 (`mediaProgresses.updatedAt ${dir} NULLS LAST`).
   - This matches Audiobookshelf's own implementation of the "Listen Again" shelf in `libraryFilters.js` (`sortBy = 'progress'`, `sortDesc = true`), sorting from the most recently listened-to book down to the oldest listened-to book without depending on nullable `finishedAt` fields.
3. **UI Localization & Selection (`filters_ui_strings.xml`, `SortModeBottomSheet.kt`)**:
   - Added string resource `<string name="sort_mode_last_listened">Last Listened</string>`.
   - Added mapping in `SortModeBottomSheet` to display and select "Last Listened".
4. **Home "Listen Again" Shelf Header Navigation (`HomePresenter.kt`)**:
   - Updated the click handler for the "Listen Again" shelf header to navigate to `LibraryScreen` with `filter = Filter.Finished`, `sortMode = ContentSortMode.LastListened`, and `sortDirection = SortDirection.Descending`.
5. **Screen-Scoped Retained State (`LibraryPresenter.kt`)**:
   - Keyed `rememberRetainedSaveable(screen)` to guarantee that screen navigation arguments (`sortMode`, `sortDirection`, `filter`) are reliably applied when transitioning from Home shelf headers.

### Modified Files:
* `core/src/commonMain/kotlin/app/campfire/core/settings/ContentSortMode.kt`
* `features/filters/ui/src/commonMain/composeResources/values/filters_ui_strings.xml`
* `features/filters/ui/src/commonMain/kotlin/app/campfire/filters/ui/sort/SortModeBottomSheet.kt`
* `features/home/ui/src/commonMain/kotlin/app/campfire/home/ui/HomePresenter.kt`
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/paged/LibraryPresenter.kt`

---

## 52. Multi-Tier Resilient Fallback for Cover Thumbnails (v1.2.24)

### Summary
Fixed an issue where book cover thumbnails (e.g. *Blood Is Thicker than Lots of Stuff*) appeared blank or as placeholder icons in library lists, search results, and mini players despite rendering properly on the book detail screen.

### Root Cause
1. In `CoverSizingInterceptor.kt`, image URLs were modified to request server-resized WebP images (e.g. `?width=200&format=webp`).
2. Audiobookshelf's server stores pre-resized image caches (`_200.webp`, `_800.webp`, `_1200.webp`). When sharp resize errors or cache corruption occur on specific dimension variants on the server, the server returns HTTP 500 or 0 bytes for that specific thumbnail size.
3. Detail views succeeded because they requested `width=1200` (which was cached and intact on the server). Because the interceptor had no fallback mechanism, failure of the `width=200` request resulted in an unhandled Coil error, leaving thumbnail covers completely blank.

### Key Changes
1. **Raw Cover URL Helper (`CoverUrls.kt`)**:
   - Implemented `CoverUrls.raw(url)` which strips dimension/format query parameters and appends `raw=1` (Audiobookshelf's raw uncompressed image endpoint).
2. **Multi-Tier Interceptor Fallback (`CoverSizingInterceptor.kt`)**:
   - Sized request (`?width=...&format=webp`) is attempted first.
   - If the sized request fails with an HTTP error status code or empty body:
     - Fallback 1: Retries with the original un-sized URL (`chain.proceed(originalRequest)`).
     - Fallback 2: If the un-sized URL also fails, retries with the raw endpoint (`CoverUrls.raw(url)`).
3. **Unit Tests (`CoverUrlsTest.kt`, `CoverSizingInterceptorTest.kt`)**:
   - Verified that sized, un-sized, and raw fallbacks execute properly in sequential error scenarios.

### Modified Files:
* `core/src/commonMain/kotlin/app/campfire/core/image/CoverUrls.kt`
* `core/src/commonTest/kotlin/app/campfire/core/image/CoverUrlsTest.kt`
* `app/common/src/commonMain/kotlin/app/campfire/common/image/CoverSizingInterceptor.kt`
* `app/common/src/commonTest/kotlin/app/campfire/common/image/CoverSizingInterceptorTest.kt`

---

## 53. Paging Stabilization: Card Reshuffle Removal & Non-Destructive Background Refresh (v1.2.24)

### Summary
Resolved scrolling hitching, card re-shuffling animations, and sudden upward screen snaps when browsing the Library, Recently Added, and other paged screens.

### Root Cause
1. **Unwanted Item Placement Animations**: In Compose `LazyCampfireGrid` and `LazyColumn`, `Modifier.animateItem()` was attached to every item card and list row. Any time paging data updated or adjusted indices, Compose triggered physics spring animations that visibly slid cards across grid slots ("card reshuffle") and dropped frames.
2. **Destructive Background Page Purge**: When entering the screen, `RemoteMediator` began an asynchronous background refresh for Page 0. If the user scrolled down past Page 0 before the network completed, `deleteByInput()` wiped out all subsequent cached pages (Pages 1..N) from SQLite, suddenly shrinking the database list and violently snapping the user's scroll position upwards.

### Key Changes
1. **Remove `Modifier.animateItem()` from Paginated Screens (`LibraryUi.kt`)**:
   - Removed `.animateItem()` from `LibraryItemCard` and `LibraryItemListItem`. Cards render statically and stay anchored in place without sliding or calculating animation physics.
2. **Non-Destructive Background Refresh (`LibraryItemRemoteMediator.kt`)**:
   - Checked if Page 0 returned from network has identical item IDs and total count as currently stored in SQLite: if unchanged (the case 95% of the time), returns early with zero database modifications or invalidations.
   - If Page 0 changed while the user is already scrolled past Page 0 (`anchorPosition >= pageSize`), only replaces Page 0 (`deletePage(page = 0)`), preserving Pages 1..N that the user is currently reading.
   - If the user is at the top of the list, performs a clean `deleteByInput` refresh.

### Modified Files:
* `features/libraries/ui/src/commonMain/kotlin/app/campfire/libraries/ui/paged/LibraryUi.kt`
* `features/libraries/impl/src/commonMain/kotlin/app/campfire/libraries/paging/LibraryItemRemoteMediator.kt`

---

## 54. Home Shelf Auto-Scroll Reset on Content Update (v1.2.25)

### Summary
Resolved an issue where newly added books or updated shelf content on the Home screen ("Recently Added", "Recent Series", "Continue Listening", "Listen Again") did not display at the beginning of the shelf and appeared hidden off-screen to the left until manually swiped.

### Root Cause
1. **Compose LazyRow Key Anchoring**: Each card in a horizontal shelf is keyed by entity ID. When a user was viewing index 0 (Book A), and a new book was prepended at the server (`[New Book, Book A, ...]`), Compose anchored the scroll offset to Book A's key. This shifted `firstVisibleItemIndex` from 0 to 1, pushing `New Book` off-screen to the left.
2. **Shelf-Specific Reset Restriction**: `ShelfListItem.kt` had a scroll reset listener (`scrollToItem(0)`) restricted only to `isDiscover`. Other chronological shelves did not reset their scroll positions when fresh items arrived.

### Key Changes
1. **Multi-Shelf Scroll Reset (`ShelfListItem.kt`)**:
   - Extended `shouldResetOnUpdate` to cover `RecentlyAdded`, `RecentSeries`, `ContinueListening`, and `ListenAgain`.
   - When shelf entities update and the shelf is scrolled (`firstVisibleItemIndex > 0`), automatically scrolls back to index 0 (`listState.scrollToItem(0)`).
   - The newest books and series immediately render at the front (far left) of the shelf upon arrival.

### Modified Files:
* `features/home/ui/src/commonMain/kotlin/app/campfire/home/ui/composables/ShelfListItem.kt`

---

## 55. Voice Pitch Adjustment in Equalizer (v1.2.26)

### Summary
Added a user-facing **Voice Pitch** slider to the Equalizer bottom sheet (`EqualizerBottomSheet.kt`), allowing listeners to alter the narrator's pitch (making their voice deeper or higher) independently of playback speed.

### Key Architecture & Implementation Details
1. **Mathematical Semitone Frequency Factor (`Equalizer.kt`)**:
   - Added `pitchSemitones: Float = 0f` to `EqualizerProfile` bounded within `EqualizerBands.PitchSemitonesRange` (`-4f..4f`).
   - Derived pitch multiplier using the equal-temperament formula:
     $$\text{pitchFactor} = 2.0^{\frac{\text{pitchSemitones}}{12.0}}$$
     yielding $-4\text{ st} \approx 0.7937\times$ (deeper voice), $0\text{ st} = 1.0\times$ (normal voice), and $+4\text{ st} \approx 1.2599\times$ (higher voice).
2. **ExoPlayer PlaybackParameters Integration (`ExoPlayerAudioPlayer.kt`)**:
   - Leveraged Android Media3 ExoPlayer's built-in `SonicAudioProcessor` via `player.setPlaybackParameters(PlaybackParameters(speed, effectivePitch))`.
   - Sonic shifts audio pitch without affecting the duration or playback tempo.
   - Connected `effectivePitch` synchronously across `setPlaybackSpeed()`, `setEqualizer()`, and `prepareSession()`, ensuring vocal pitch persists across playback speed changes, pause/resume, and per-book equalizer overrides.
   - Tied directly to the Equalizer master switch: toggling the Equalizer off instantly reverts pitch factor to $1.0\times$ without destroying the user's configured semitone value.
3. **Discrete 9-Step Snapping Slider UI (`EqualizerBottomSheet.kt`)**:
   - Added a `LabeledSlider` with 7 intermediate tick marks (`steps = 7`), providing 9 discrete semitone stops: `-4, -3, -2, -1, 0, +1, +2, +3, +4`.
   - Visual display shows `"Normal"` at $0\text{ st}$, `"+1" .. "+4"` for higher pitch, and `"-1" .. "-4"` for deeper pitch.
4. **Backward-Compatible Serialization (`EqualizerSettingsImpl.kt`)**:
   - Appended `pitchSemitones` as a 6th field in `EqualizerProfile.serialize()`.
   - `asEqualizerProfile()` accepts both legacy 5-field strings (defaulting `pitchSemitones = 0f`) and 6-field strings, guaranteeing zero migration issues or settings corruption for existing users.

### Modified Files:
* `core/src/commonMain/kotlin/app/campfire/core/audio/Equalizer.kt`
* `features/sessions/ui/src/commonMain/composeResources/values/sessions_ui_strings.xml`
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/sheets/equalizer/EqualizerBottomSheet.kt`
* `features/sessions/ui/src/jvmTest/kotlin/app/campfire/sessions/ui/sheets/equalizer/EqualizerSideSheetRenderTest.kt`
* `features/settings/impl/src/commonMain/kotlin/app/campfire/settings/EqualizerSettingsImpl.kt`
* `features/settings/impl/src/commonTest/kotlin/app/campfire/settings/EqualizerSettingsTest.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/ExoPlayerAudioPlayer.kt`
* `gradle.properties`

---

## 56. Uniform Segmented Scope Selector (Global vs. This Book) in Equalizer & Speed (v1.2.26)

### Summary
Replaced the ambiguous, unlabeled trailing switch in both the Equalizer and Playback Speed bottom sheets with a prominent Material 3 `SingleChoiceSegmentedButtonRow`. This establishes an identical, self-explanatory UI pattern across both screens: `[ 🌐 All Books (Global) | 📖 This Book Only ]`.

### Key Changes
1. **Removed Mystery Trailing Switches**:
   - Stripped the icon-only toggle switch (`CampfireIcons.Rounded.Book` / `Globe`) from the top header bar of `EqualizerBottomSheet.kt` and `PlaybackSpeedBottomSheet.kt`.
   - Titles ("Equalizer" and "Playback speed") now render cleanly centered without confusing adjacent controls.
2. **Prominent Material 3 Segmented Pill Selector**:
   - Added `SingleChoiceSegmentedButtonRow` across the top of both sheets:
     - **`🌐 All Books (Global)`**: Edits or views the app-wide default profile/speed.
     - **`📖 This Book Only`**: Creates, edits, or deletes dedicated per-book overrides.
   - In Equalizer, only one single toggle switch remains on the screen: **"Enable equalizer"** (the master DSP audio effects power switch).
   - In Playback Speed, the scope selector sits cleanly above the preset speed buttons and slider.
3. **Strings**:
   - Added reusable `session_scope_global` ("All Books (Global)") and `session_scope_per_book` ("This Book Only") to `sessions_ui_strings.xml`.

### Modified Files:
* `features/sessions/ui/src/commonMain/composeResources/values/sessions_ui_strings.xml`
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/sheets/equalizer/EqualizerBottomSheet.kt`
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/sheets/speed/PlaybackSpeedBottomSheet.kt`
* `gradle.properties`

---

## 57. Granular Playback Skip Forward & Backward Duration Sliders (v1.2.27)

### Summary
Replaced the rigid 3-value tap-to-cycle skip settings (`5s / 10s / 30s`) in Playback Settings with interactive `DurationSliderSetting` sliders for both Forward and Backward skip times, giving users full granular control across 5-second increments from 5s to 60s.

### Key Changes
1. **Interactive Duration Sliders**:
   - Replaced `TimeJumpSetting` with `DurationSliderSetting` in `PlaybackPane.kt` for forward skip and backward skip times.
   - Configured with `valueRange = 5.seconds..60.seconds` and `stepSeconds = 5`, providing 12 discrete, granular steps: `5s, 10s, 15s, 20s, 25s, 30s, 35s, 40s, 45s, 50s, 55s, 60s`.
   - Displays real-time exact second labels (`"${it.inWholeSeconds}s"`).
2. **System & Playback Engine Compatibility**:
   - Seamlessly works with existing audio player engines (`ExoPlayerAudioPlayer`, `DesktopAudioPlayer`, `IosAudioPlayer`) which already accept arbitrary millisecond skip durations.
   - Fully compatible with Android Auto and media notifications via AndroidX Media3 (official 15-second icons are natively mapped in `MediaSessionCallback.kt`, with smooth fallback to standard system icons for other values).
   - Preserved `QuickSkipJumps` for `trackResetThreshold` so reset threshold functionality remains completely intact.

3. **Consistent Circular Skip Arrow Icons & 15s Icon Badges**:
   - Replaced the mismatched double-chevron fast-forward arrow in `Forward.kt` with a true clockwise circular arrow loop that identically mirrors `Replay.kt`.
   - Added dedicated `Forward15` and `Replay15` vector ImageVectors so 15-second skip times render with clear "15" digit badges matching 5s, 10s, and 30s.
   - All non-standard duration selections now render consistent, matching circular loop arrow icons for both rewind and forward buttons across the entire app.

### Modified Files:
* `common/compose/src/commonMain/kotlin/app/campfire/common/compose/icons/rounded/Forward.kt`
* `common/compose/src/commonMain/kotlin/app/campfire/common/compose/icons/rounded/Forward15.kt`
* `common/compose/src/commonMain/kotlin/app/campfire/common/compose/icons/rounded/Replay15.kt`
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/composables/TimeSkipIcons.kt`
* `features/settings/ui/src/commonMain/kotlin/app/campfire/ui/settings/panes/PlaybackPane.kt`
* `CHANGELOG.md`
* `gradle.properties`

---

## 58. Wired Headset 3.5mm Multi-Click Detection & Navigation Drawer String Fix (v1.2.28)

### Problem Description
1. Users using wired earbuds/headphones with a 3.5mm jack (or USB-C to 3.5mm analog adapters) reported that triple-pressing the inline button did not skip backward, and both double and triple button presses would only skip forward.
   - **Root Cause**: On 3.5mm headsets, the single physical button shorting the mic line to ground emits `KeyEvent.KEYCODE_HEADSETHOOK`. In AndroidX Media3 (`MediaSessionImpl`), the framework eagerly intercepts double-clicks and immediately dispatches `onSkipToNext()`, while completely lacking any triple-click logic. When the 3rd click arrives, the double-click has already executed forward skip and the 3rd click is treated as a separate stray click.
2. In the navigation drawer, the "What's New" menu item was rendered with an escaped backslash as `"What\'s New"`.
   - **Root Cause**: Jetpack Compose Multiplatform string resource generator does not treat `\'` as an escape sequence, rendering the backslash literally.

### Solution & Key Implementation Details
1. **Wired Headset Multi-Click State Machine (`MediaSessionCallback.kt`)**:
   - Intercepted `KeyEvent.KEYCODE_HEADSETHOOK` inside `onMediaButtonEvent()`.
   - Built a high-precision multi-click detection state machine:
     - **Single Click**: Toggles Play/Pause (with cold-start session resumption support).
     - **Double Click**: Dispatches Skip Forward (either custom time-seek via `player.seekForward()` or chapter advance via `player.skipToNext()` based on `remoteNextPrevSkipsChapters`).
     - **Triple Click**: Dispatches Skip Backward immediately on the 3rd release without delay (either custom time-seek via `player.seekBackward()` or chapter return via `player.skipToPrevious()` based on `remoteNextPrevSkipsChapters`).
     - **Long-Press Passthrough**: Uses `ViewConfiguration.getLongPressTimeout()` and `repeatCount` detection to cleanly pass long-presses to Google Assistant.
2. **String Resource Fix (`ui_drawer_strings.xml`, `audioplayer_ui_strings.xml`)**:
   - Replaced `What\'s New` with `What's New` and `Couldn\'t connect` with `Couldn't connect`.

### Modified Files:
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/MediaSessionCallback.kt`
* `ui/navigation/ui/src/commonMain/composeResources/values/ui_drawer_strings.xml`
* `infra/audioplayer/public-ui/src/commonMain/composeResources/values/audioplayer_ui_strings.xml`
* `CHANGELOG.md`
* `gradle.properties`

---

## 59. Dual Book-Time Readouts & Action Row Alignment Fix (v1.2.29)

### Problem Description
1. In the expanded audiobook player, the whole-book progress indicator previously only showed the remaining time centered beneath the progress bar, omitting the elapsed listening time.
2. In the player bottom action row, the playback speed action button (`PlaybackSpeedAction.kt`) and end-of-chapter timer (`RunningTimerAction.kt`) were implemented as hardcoded 96dp wide boxes with unconstrained 18sp extra bold text. When multiple player tools were present, the ~48dp action slot caused the speed text (`"1.25x"`) to wrap into two lines, pushing `"1.25"` upwards and clipping `"x"` at the bottom container edge.

### Solution & Key Implementation Details
1. **Dual Book-Time Readouts (`BookTimeProgressIndicator.kt`)**:
   - Replaced centered single-text readout with a full-width row (`BookTimeReadouts`) containing:
     - Elapsed listening time formatted on the far left (e.g. `16s`, `1h 45m`).
     - Real-time speed-adjusted remaining countdown on the far right (e.g. `7h 3m 58s left`).
   - Seamlessly integrates with animated acceleration indicator when playback speed > 1.0x.
2. **Unified Standard `IconButton` Integration (`PlaybackSpeedAction.kt`, `RunningTimerAction.kt`)**:
   - Replaced custom fixed-width box layouts with standard `IconButtonTooltip` and `IconButton` containers.
   - Formatted speed and end-of-chapter timer text with `fontSize = 16.sp`, `fontWeight = FontWeight.Bold`, `maxLines = 1`, `softWrap = false`, and `overflow = TextOverflow.Visible`.
   - Guaranteed identical vertical alignment, touch targets, and baseline centering across all player action buttons.

### Modified Files:
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/playback/expanded/composables/BookTimeProgressIndicator.kt`
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/composables/PlaybackSpeedAction.kt`
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/composables/RunningTimerAction.kt`
* `CHANGELOG.md`
* `CUSTOM_MODIFICATIONS.md`
* `gradle.properties`

---

## 60. Google Assistant & Gemini Hands-Free Voice Controls & Enlarged Book-Time Readouts (v1.2.30)

### Problem Description
1. Users holding the phone power button (launching Google Gemini / Assistant) or saying *"Hey Google"* previously could not control Campfire hands-free (play, pause, next chapter, rewind, or open and play an audiobook).
2. The whole-book elapsed and remaining duration labels beneath the expanded player progress bar (`BookTimeProgressIndicator.kt`) were rendered at 11sp (`MaterialTheme.typography.labelSmall`), which was slightly small to read at a glance or while in motion.

### Solution & Key Implementation Details
1. **Official Android App Actions Capabilities (`shortcuts.xml`)**:
   - Defined capabilities for `actions.intent.PLAY_MEDIA`, `actions.intent.RESUME_MEDIA`, `actions.intent.PAUSE_MEDIA`, and `actions.intent.STOP_MEDIA` mapping to `android.media.action.MEDIA_PLAY_FROM_SEARCH`.
   - Registered `shortcuts.xml` in `app/android/src/main/AndroidManifest.xml` via `<meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />`.
2. **System Voice Intent Handling (`MainActivity.kt` & `AudioPlayerService`)**:
   - Registered `android.media.action.MEDIA_PLAY_FROM_SEARCH` intent filters in both `app/android/src/main/AndroidManifest.xml` and `infra/audioplayer/impl/src/androidMain/AndroidManifest.xml`.
   - In `MainActivity.kt`, added `handleVoiceIntent()` to connect asynchronously via Media3's `MediaController` and dispatch playback, search queries, or pause toggles directly.
   - Upgraded `media3.session` dependency in `app/android/build.gradle.kts` from `debugImplementation` to `implementation` for release build support.
3. **Voice Search & Playback Session Resolution (`MediaSessionCallback.kt`)**:
   - In `onSetMediaItems`, added support for empty media items (voice resume signals) and search queries (`search:<query>` or `item.requestMetadata.searchQuery`).
   - Resolves search queries directly against `mediaTree.search(query)` and starts immediate playback on the top match, or seamlessly falls back to resuming the active session.
4. **20% Enlarged Whole-Book Time Readouts (`BookTimeProgressIndicator.kt`)**:
   - Increased elapsed and remaining text sizing by 20% from 11sp to 13.2sp (`lineHeight = 18.sp`).
   - Scaled speed-accelerated remaining text from 12sp to 14sp bold/italic, perfectly complementing the 16dp double-arrow indicator.

### Modified Files:
* `app/android/build.gradle.kts`
* `app/android/src/main/AndroidManifest.xml`
* `app/android/src/main/res/xml/shortcuts.xml`
* `app/android/src/main/java/app/campfire/android/MainActivity.kt`
* `infra/audioplayer/impl/src/androidMain/AndroidManifest.xml`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/MediaSessionCallback.kt`
* `features/sessions/ui/src/commonMain/kotlin/app/campfire/sessions/ui/playback/expanded/composables/BookTimeProgressIndicator.kt`
* `CHANGELOG.md`
* `CUSTOM_MODIFICATIONS.md`
* `gradle.properties`

---

## 61. Android Auto & Notification Dual Progress Hierarchy (v1.2.31)

### Problem Description
1. In Android Auto and the phone lock screen media notification, drivers previously had to view the chapter title with only a single remaining countdown on Line 1 (e.g. "Chapter 4 - 3h 45m left"), with the author or book title occupying secondary lines.
2. In Android Auto split-screen (Coolwalk dashboard with Google Maps/Waze), Line 3 is completely suppressed by Android Automotive OS to fit transport controls, meaning any metadata placed on Line 3 vanishes during navigation.
3. Drivers prioritize seeing their total listening progress ("4h 15m played • 3h 45m left") prominently without distraction or having it truncated by long chapter/book titles.

### Solution & Key Implementation Details
1. **Reorganized Metadata Hierarchy for Android Auto & System Notifications**:
   - **Line 1 (title)**: Dual listening progress ("4h 15m played • 3h 45m left"). Rendered in large bold text on car head units and phone lock screens, providing high-contrast, instant at-a-glance visibility.
   - **Line 2 (artist / subtitle)**: Combined chapter and book title ("Chapter 4 - The Way of Kings"). Rendered in regular (non-bold) medium font, fitting ~40% more characters to comfortably display both chapter and book without truncation.
   - **Line 3 (albumTitle)**: Author name ("Brandon Sanderson"). Visible in full-screen mode and safely deprioritized during split-screen navigation.
2. **Speed-Adjusted Real-Time Updates**:
   - `ExoPlayerAudioPlayer.kt` dynamically computes both `elapsedBookTimeFormatted()` and speed-adjusted `remainingBookTimeFormatted()`.
   - On every progress tick / minute boundary, `updatePlaylistMetadata()` updates the active media item metadata to advance the displayed minutes.
3. **Coarse Window Projection Integration (ChapterWindowForwardingPlayer.kt)**:
   - Enhanced `Host` interface with `elapsedFormatted(): String`.
   - Updated `chapterPlaylist` to project the dual progress line and chapter/book title across all chapter items, caching on minute changes.
4. **Multi-Track & Podcast Unification (MediaItemBuilder.kt)**:
   - Unified formatting through `formatDualProgressTitle()` and `formatChapterWithBookTitle()`.
   - Applied symmetrically across HLS streams, podcast episodes, chaptered audiobooks, and multi-track audiobooks.

### Modified Files:
* `infra/audioplayer/impl/src/commonMain/kotlin/app/campfire/audioplayer/impl/mediaitem/MediaItemBuilder.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/MediaItemBuilder.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/forwarding/ChapterWindowForwardingPlayer.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/ExoPlayerAudioPlayer.kt`
* `CHANGELOG.md`
* `CUSTOM_MODIFICATIONS.md`
* `gradle.properties`

---

## 62. Android Auto Single-File Chaptered Book Title Restoration (v1.2.32)

### Problem Description
1. In Android Auto and system media notifications for single-file audiobooks with embedded chapter markers (e.g. Audible M4B/MP3s), Line 2 displayed the author name (`029 - Dennis E. Taylor`) instead of the book title (`029 - The Singularity Trap`).
2. This occurred because `ChapterWindowForwardingPlayer` extracted `bookTitle` from `baseItem.mediaMetadata.albumTitle`. After the v1.2.31 metadata reorganization moved the author name to Line 3 (`albumTitle`), `baseItem.mediaMetadata.albumTitle` was populated with the author name, causing `ChapterWindowForwardingPlayer` to mistakenly treat the author as the book title.
3. Minute-by-minute dynamic metadata updates in `ExoPlayerAudioPlayer.updatePlaylistMetadata()` also lacked direct access to the raw chapter title from the timeline, risking string corruption.

### Solution & Key Implementation Details
1. **Direct Session & Model Access via Host Interface (`ChapterWindowForwardingPlayer.kt`)**:
   - Added `bookTitle(): String` and `authorName(): String` to the `ChapterWindowForwardingPlayer.Host` interface.
   - Implemented in `ExoPlayerAudioPlayer.kt` to query `preparedSession?.libraryItem?.media?.metadata?.title` and `authorName` directly from the active session model, eliminating reliance on platform metadata slots.
2. **Clean Domain Semantics vs. Platform Slot Projection (`MediaItemBuilder.kt`)**:
   - Restored pure domain semantics in `MediaItem.Metadata` (`commonMain`): `albumTitle` represents the book title, `artist` represents the author, and `subtitle` represents the chapter title.
   - Isolated platform slot mapping inside `asPlatformMediaItem(context)` (`androidMain`):
     - **Line 1 (`setTitle`)**: Dual Progress countdown (`3h 43m played • 6h 7m left`).
     - **Line 2 (`setArtist` & `setSubtitle`)**: Chapter and Book title (`029 - The Singularity Trap`).
     - **Line 3 (`setAlbumTitle` & `setAlbumArtist`)**: Author name (`Dennis E. Taylor`).
3. **Timeline-Driven Dynamic Metadata Updates (`ExoPlayerAudioPlayer.kt`)**:
   - `updatePlaylistMetadata()` now resolves the unformatted chapter title from `session.episode?.title ?: chapterTimeline?.chapterAt(overallTime.value)?.title`, ensuring real-time progress ticks keep the chapter and book title in sync.
4. **Defensive Title Deduplication (`MediaItemBuilder.formatChapterWithBookTitle`)**:
   - Added checks to prevent duplicate book titles if the chapter metadata already has the book title embedded (e.g. avoiding `Chapter 1 - Title - Title`).

### Modified Files:
* `infra/audioplayer/impl/src/commonMain/kotlin/app/campfire/audioplayer/impl/mediaitem/MediaItemBuilder.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/MediaItemBuilder.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/forwarding/ChapterWindowForwardingPlayer.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/ExoPlayerAudioPlayer.kt`
* `CHANGELOG.md`
* `CUSTOM_MODIFICATIONS.md`
* `gradle.properties`

---

## 63. Wired Headset 30-Second Skip Interception & Sleep Timer Persistence (v1.2.33)

### Problem Description
1. **Wired Headset / USB-C Inline Remote Chapter Skip Bug**:
   - A user running a Samsung Galaxy Note 10 on Android 12 (One UI 4.1) using a wired USB-C headset reported that skipping with the wired headset skipped audiobook chapters instead of jumping forward/backward by 30 seconds, even though Bluetooth earbuds worked perfectly and the user explicitly configured `remoteNextPrevSkipsChapters = false`.
   - **Root Cause**: In AndroidX Media3, hardware `ACTION_MEDIA_BUTTON` broadcast events from wired 3.5mm headsets, USB-C headphones, and USB DAC dongles are handled by `MediaSessionService.onStartCommand()` and routed into `MediaSessionImpl` via Media3's internal notification controller (`session.isMediaNotificationController(controllerInfo) == true`). In `MediaSessionCallback.kt`, checking `!isNotification` falsely identified all wired headset button events as the system notification tray, bypassing custom time-seek interception for `KEYCODE_HEADSETHOOK`, `KEYCODE_MEDIA_NEXT`, and `KEYCODE_MEDIA_PREVIOUS` and falling through to Media3's default behavior (`seekToNextMediaItem()`).
2. **Sleep Timer Disappearing / Disabled After Playback Pauses**:
   - Users noticed that after setting a sleep timer while paused (or pausing playback with a running sleep timer) and unpausing several minutes later, the sleep timer was disabled or completely gone.
   - **Root Cause**: `CoroutineSleepTimerManager` previously maintained all active timer state solely in memory. When playback is paused for several minutes, Android demotes the foreground playback service to a cached background service and terminates the process during deep sleep. Upon unpausing, playback resumed cleanly, but `CoroutineSleepTimerManager` was re-instantiated with all in-memory timer fields set to `null`.

### Solution & Key Implementation Details
1. **Wired Headset & Media Notification Controller Decoupling (`MediaSessionCallback.kt`)**:
   - Removed the overly broad `!isNotification` check from `KeyEvent.KEYCODE_HEADSETHOOK`, `KeyEvent.KEYCODE_MEDIA_NEXT`, and `KeyEvent.KEYCODE_MEDIA_PREVIOUS`.
   - Android system notification buttons utilize Media3 `PlayerCommand`s (`Player.COMMAND_SEEK_TO_NEXT`, `Player.COMMAND_SEEK_FORWARD`, etc.) rather than raw hardware `KeyEvent`s, making the `!isNotification` check on `KeyEvent`s unnecessary and harmful to wired headsets.
   - Retained the `!isAuto` guard so that dedicated in-car steering wheel and console buttons in Android Auto continue to function as intended.
   - Wired headset multi-clicks (single click: play/pause; double click: skip forward; triple click: skip backward) and OEM-translated next/prev hardware buttons now consistently honor `component.playbackSettings.remoteNextPrevSkipsChapters` by invoking `player.seekForward()` and `player.seekBackward()`.
2. **Sleep Timer State Persistence & Rehydration (`SleepSettings.kt`, `SleepSettingsImpl.kt`, `CoroutineSleepTimerManager.kt`)**:
   - Added `PersistedSleepTimer(isEndOfChapter, totalDurationMillis, remainingMillis, isAutoSleepTimer)` to `SleepSettings`.
   - Implemented persistent storage in `SleepSettingsImpl` backed by `stringOrNullSetting(KEY_ACTIVE_SLEEP_TIMER)`.
   - Updated `CoroutineSleepTimerManager`:
     - **Save on Set**: In `setTimer(timer)`, writes `activeSleepTimer` to settings immediately.
     - **Save on Pause**: In `pauseCountdown(timer)`, computes and persists the exact frozen `remainingMillis`.
     - **Save on Resume**: In `resumeCountdown(timer)`, records the active timer and countdown resume reference.
     - **Clear on Stop/Finish**: In `clearTimerInternal()`, sets `activeSleepTimer = null`.
     - **Restore on Re-Instantiation**: In `init`, invokes `restorePersistedTimer()` to inspect `sleepSettings.activeSleepTimer`. If a valid timer exists, rehydrates `playbackTimer`, sets `initialRemainingMillis`, starts state observation, and immediately publishes a frozen `RunningTimer` if paused.
3. **Comprehensive Unit Testing (`CoroutineSleepTimerManagerTest.kt`)**:
   - Added test `persisted sleep timer is restored on manager initialization` verifying frozen remaining duration on pause and resumption countdown to completion.
   - Added test `persisted end of chapter timer is restored on manager initialization` verifying boundary pause and clean timer removal.

### Modified Files:
* `features/settings/api/src/commonMain/kotlin/app/campfire/settings/api/SleepSettings.kt`
* `features/settings/impl/src/commonMain/kotlin/app/campfire/settings/SleepSettingsImpl.kt`
* `features/settings/test/src/commonMain/kotlin/app/campfire/settings/test/FakeSleepSettings.kt`
* `infra/audioplayer/impl/src/commonMain/kotlin/app/campfire/audioplayer/impl/sleep/CoroutineSleepTimerManager.kt`
* `infra/audioplayer/impl/src/androidMain/kotlin/app/campfire/audioplayer/impl/MediaSessionCallback.kt`
* `infra/audioplayer/impl/src/jvmTest/kotlin/app/campfire/audioplayer/impl/sleep/CoroutineSleepTimerManagerTest.kt`
* `CHANGELOG.md`
* `CUSTOM_MODIFICATIONS.md`
* `gradle.properties`
