# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

## [1.2.39]

### Changed

- **Full-Width Stacked Layout for Headset & Remote Controls Settings**:
  - Redesigned the action selectors in the "Headset & Remote Controls" settings pane from cramped horizontal rows into clean, full-width vertical blocks with headlines and descriptions placed directly above each selector.
  - Eliminated awkward single-letter word wrapping (such as orphan "s" in "Quadruple-Press") and compressed 3–4 line descriptions into natural 1–2 line paragraphs spanning the full screen width.
  - Added subtle horizontal divider lines between setting blocks to establish clear visual boundaries, ensuring users can instantly tell which description belongs to each button.
  - Expanded selector chips into full-width Material 3 picker fields, giving long action labels (e.g. "Skip to previous chapter") ample room without truncation.

## [1.2.38]

### Changed

- **Restore 1-Hour Cooldown for Home Feed Pull-to-Refresh**:
  - Restored the 1-hour background cooldown throttle (`force = false`) on home feed pull-to-refresh gestures (`HomeUiEvent.Refresh` and `HomeUiEvent.RefreshDiscoveries`), preventing excessive GitHub API requests during frequent library refreshes.
  - The dedicated "Check for updates" navigation drawer button continues to bypass the cooldown (`force = true`), providing immediate on-demand checking whenever desired.

## [1.2.37]

### Added

- **Interactive In-Drawer App Update Card**:
  - Replaced transient, unclickable toast notifications in the navigation drawer with an expandable, interactive update card rendered directly beneath the "Check for updates" button.
  - Tapping the update card immediately opens the release notes bottom sheet with full changelogs and one-tap installation.
  - Added a dismiss ('X') button directly on the card to collapse it when desired.
- **Inline Status Feedback for Update Checks**:
  - Checking for updates now displays a clean, temporary inline status banner directly under the "Check for updates" button ("Campfire is up to date" or network error) that automatically fades away after 4 seconds.
- **Update Badge on Drawer Navigation**:
  - Added an update badge indicator to the "Check for updates" navigation item whenever an update is available.

### Changed

- **Forced Update Checks on User Pull-to-Refresh**:
  - Pulling down to refresh on the Home screen (`HomeUi`) now runs an immediate forced update check against GitHub Releases (`force = true`), bypassing the 1-hour background cooldown throttle.
- **Automatic Un-dismissal on Manual Update Checks**:
  - Manually checking for updates (either by swiping down on Home or tapping "Check for updates" in the drawer) now clears prior update dismissals, ensuring newly available releases are immediately surfaced on both the Home screen and the navigation drawer.

## [1.2.36]

### Added

- **Dedicated "Headset & Remote Controls" Settings Category**:
  - Moved remote button and gesture preferences out of the crowded Playback settings into a dedicated, organized "Headset & Remote Controls" settings pane.
  - Distinct sections for **Wired Headset (Single Button)** and **Bluetooth & Dedicated Buttons** with clear, intuitive guidance.

- **Configurable Multi-Press Actions for Single-Button Wired Headsets**:
  - Added individual action selectors for **Double-Press**, **Triple-Press**, and **Quadruple-Press** of the physical inline play button.
  - Customizable actions include: *Skip forward (time)*, *Skip backward (time)*, *Skip to next chapter*, *Skip to previous chapter*, *Add bookmark*, or *Do nothing*.
  - Instant remote bookmark creation saves a bookmark at the exact playback position with toast feedback without having to take the phone out of your pocket or unlock the screen.

- **Configurable "Next Track" & "Previous Track" Command Mapping**:
  - Independent action mapping for incoming **Next Track** and **Previous Track** commands from Bluetooth earbuds, headphones, and car steering wheel controls.
  - Supports all actions (*Skip forward*, *Skip backward*, *Skip next/previous chapter*, *Add bookmark*, *Do nothing*), decoupling Campfire's actions from headphone companion app gesture customizations.

### Fixed

- **Wired Headset Multi-Press Accumulation & Chapter Skip Hijack**:
  - Fixed a race condition in `MediaSessionCallback` where rapid multi-clicks on a single inline headset hook button did not cancel pending click timers on `ACTION_DOWN`, causing double and triple presses to misfire or execute as unwanted chapter skips.
  - Key events for wired single-button multi-presses and Bluetooth next/previous commands are now fully consumed and dispatched directly to the configured action before Media3's internal controller proxy can force chapter skipping.

## [1.2.35]

### Fixed

- **Fix App Crash on Launch from Persisted Sleep Timer (Class Initialization Order)**:
  - Fixed a critical startup crash (`NullPointerException` on `player.state.value` during `AudioPlayerService.onCreate()`) that caused the app to crash repeatedly on open if the user previously had an active sleep timer.
  - Deferred instantiation of `SleepTimerManager` in `ExoPlayerAudioPlayer` until after `state` and all player StateFlows are fully initialized.
  - Added robust exception handling around `restorePersistedTimer()` and `startTimer()` in `CoroutineSleepTimerManager` so any corrupted or unexpected timer state is safely caught and cleared rather than causing a crash loop on app launch.

## [1.2.34]

### Added

- **Multi-Trigger GitHub App Update Checking with 1-Hour Disk Cooldown**:
  - **Pull-to-Refresh Auto-Check**: Pulling down to refresh on the Home screen (or tapping the Discoveries shelf refresh) automatically checks GitHub Releases for new Campfire updates in a parallel, non-blocking background task.
  - **App Foregrounding (`onResume`) Auto-Check**: Resuming or opening Campfire automatically performs a silent background update check without interrupting playback or startup.
  - **Unified 1-Hour Disk Cooldown**: All background checks share an hourly timestamp persisted to disk (`AppStateSettings.lastAppUpdateCheckTimestamp`), preventing repeated GitHub API network queries during frequent shelf refreshes or app switching while surviving app restarts and process kills.
  - **Manual "Check for Updates" Drawer Button**: Added a dedicated action item in the main navigation drawer between "Settings" and "What's New". Tapping it bypasses the 1-hour cooldown to force an immediate GitHub check with an interactive loading spinner, provides immediate Toast feedback ("Campfire is up to date", "Update available: v...", or connection status), resets the cooldown timer, and instantly presents the update banner if a new version is found.

## [1.2.33]

### Fixed

- **Wired Headset Multi-Click & Next/Previous 30-Second Skip Interception**:
  - Fixed an issue where wired 3.5mm headsets, USB-C earphones (e.g. Samsung AKG Type-C on Galaxy Note 10), and external USB-C DAC dongles unexpectedly skipped book chapters instead of jumping forward/backward by 30 seconds.
  - Media3 routes hardware `ACTION_MEDIA_BUTTON` broadcast events from wired headsets through its internal notification controller proxy (`session.isMediaNotificationController() == true`). Removed overly restrictive `!isNotification` checks from `KEYCODE_HEADSETHOOK`, `KEYCODE_MEDIA_NEXT`, and `KEYCODE_MEDIA_PREVIOUS` handlers in `MediaSessionCallback.kt`.
  - Wired headset inline multi-clicks (double click to jump forward, triple click to jump backward) and OEM-synthesized next/previous key events now consistently obey the user's remote skip setting (`remoteNextPrevSkipsChapters`).
- **Sleep Timer Persistence Across Pauses & Background Service Lifecycle**:
  - Fixed an issue where the sleep timer was unexpectedly disabled or disappeared if playback was paused for several minutes before resuming.
  - When playback is paused in deep sleep, Android demotes and terminates idle background media services. In-memory timer countdowns were previously discarded upon service recreation.
  - `CoroutineSleepTimerManager` now persists the active timer type, total duration, and exact frozen remaining countdown to disk (`SleepSettings.activeSleepTimer`) whenever a timer is set, paused, or resumed, and cleanly restores it on service restart.

## [1.2.32]

### Fixed

- **Android Auto Single-File Chaptered Book Title Restoration**:
  - Fixed an issue where single-file audiobooks with embedded chapters (e.g. Audible M4B/MP3s) displayed the author name on Line 2 (e.g. `029 - Dennis E. Taylor`) instead of the book title (`029 - The Singularity Trap`).
  - `ChapterWindowForwardingPlayer` now resolves the true book title and author name directly from the active `Session` and library metadata via the `Host` interface rather than deriving them from platform media item slots.
  - Restored clean domain semantics in `MediaItem.Metadata` across `commonMain` while cleanly handling platform slot mapping in `asPlatformMediaItem` (`androidMain`).
  - Updated `updatePlaylistMetadata()` to retrieve the raw chapter title from `chapterTimeline` so dynamic updates on minute boundaries preserve the chapter and book title.
  - Added defensive deduplication to `formatChapterWithBookTitle()` so chapter titles already containing the book title are never redundantly repeated.

## [1.2.31]

### Added

- **Android Auto & System Notification Dual Progress Layout**:
  - Reorganized Android Auto and system media notification metadata hierarchy:
    - **Line 1 (Headline, Bold)**: Dual listening progress (e.g. 4h 15m played • 3h 45m left, speed-adjusted).
    - **Line 2 (Regular Font)**: Chapter and book title (e.g. Chapter 4 - The Way of Kings).
    - **Line 3 (Full-Screen)**: Author name (e.g. Brandon Sanderson).
  - Guarantees 100% visibility of listening progress and chapter/book context across all Android Auto screen modes (including split-screen Coolwalk navigation alongside Google Maps) and the Android lock screen notification.

## [1.2.30]

### Added

- **Google Assistant & Gemini Hands-Free Voice Controls**:
  - Registered Android App Actions capabilities (`shortcuts.xml`) for `PLAY_MEDIA`, `RESUME_MEDIA`, `PAUSE_MEDIA`, and `STOP_MEDIA`.
  - Added intent-filter for `android.media.action.MEDIA_PLAY_FROM_SEARCH` across `MainActivity` and `AudioPlayerService`.
  - Enables hands-free voice commands via *"Hey Google"* or long-pressing the power button to launch Gemini (e.g. *"Play Campfire"*, *"Pause"*, *"Next chapter"*, *"Rewind 30 seconds"*, *"Resume my audiobook"*).
  - Enhanced `MediaSessionCallback` to resolve voice search queries against the media tree and seamlessly resume active playback sessions.

### Changed

- **Enlarged Whole-Book Elapsed & Remaining Readouts (`BookTimeProgressIndicator.kt`)**:
  - Increased font size by 20% from 11sp (`labelSmall`) to 13.2sp (and 14sp when speed-accelerated) with 18sp line height for enhanced readability without disturbing the expanded player layout.

## [1.2.29]

### Added

- **Player Book Time Dual Readout (`BookTimeProgressIndicator.kt`)**:
  - Shows elapsed listening time on the far left (e.g., `1h 45m`) and remaining duration on the far right (e.g., `7h 3m 58s left`) across the full width of the whole-book progress indicator.
  - Automatically accounts for speed-adjusted countdown when playback speed is accelerated.

### Fixed

- **Action Row Button Alignment & Sizing (`PlaybackSpeedAction.kt`, `RunningTimerAction.kt`)**:
  - Replaced fixed-width box constraints with standard `IconButton` integration to guarantee uniform vertical alignment and centering across all player tools.
  - Formatted playback speed indicator (`1.25x`) and End-of-Chapter countdown to bold `16.sp` on a single line, eliminating text wrapping, container clipping, and vertical displacement.

## [1.2.28]

### Added

- **Wired Headset & Earbud Multi-Click Controls (`MediaSessionCallback.kt`)**:
  - Implemented a dedicated multi-tap detector for `KEYCODE_HEADSETHOOK` on 3.5mm wired headsets and USB-C audio adapters.
  - Full single, double, and triple-click recognition:
    - **Single Click**: Toggles Play/Pause (with cold-start session resumption support).
    - **Double Click**: Skips forward (time-seek or chapter skip depending on user settings).
    - **Triple Click**: Skips backward (time-seek or chapter skip depending on user settings).
  - Eliminates the AndroidX Media3 bug where triple-click was prematurely dispatched as a double-click forward skip.
  - Automatically preserves long-press gestures for Google Assistant.

### Fixed

- **Navigation Drawer "What's New" Typo (`ui_drawer_strings.xml`, `audioplayer_ui_strings.xml`)**:
  - Removed unnecessary backslash escape sequences in Compose Multiplatform string resources that caused the menu item to literally render as `"What\'s New"`.

## [1.2.27]

### Added

- **Granular Skip Forward & Backward Duration Sliders (`PlaybackPane.kt`)**:
  - Replaced the rigid 3-value tap-to-cycle skip settings (`5s / 10s / 30s`) in Playback Settings with interactive `DurationSliderSetting` sliders for both Forward and Backward skip times.
  - Granular 5-second steps spanning from 5s to 60s (`5s, 10s, 15s, 20s, 25s, 30s, 35s, 40s, 45s, 50s, 55s, 60s`) with real-time exact second readouts.
  - Seamlessly integrates with existing playback engines, notification shade, and Android Auto.
- **Dedicated 15s Skip Badges (`Forward15.kt`, `Replay15.kt`, `TimeSkipIcons.kt`)**:
  - Added dedicated `Forward15` and `Replay15` vector ImageVectors so the popular 15-second skip duration renders with clear "15" digit badges matching 5s, 10s, and 30s.

### Fixed

- **Consistent Circular Skip Arrow Icons (`Forward.kt`, `TimeSkipIcons.kt`)**:
  - Replaced the mismatched double-chevron fast-forward arrow in `Forward.kt` with a true clockwise circular arrow loop that identically mirrors `Replay.kt`.
  - All non-badged duration selections now render consistent, matching circular loop arrow icons for both rewind and forward buttons across the entire app.

## [1.2.26]

### Added

- **Voice Pitch Adjustment (`Equalizer.kt`, `ExoPlayerAudioPlayer.kt`, `EqualizerBottomSheet.kt`)**:
  - Added a dedicated "Voice Pitch" slider to the Equalizer bottom sheet with 9 discrete semitone stops (-4 to +4 semitones with "Normal" at 0).
  - Uses equal-temperament frequency calculation ($2.0^{\frac{\text{semitones}}{12.0}}$) coupled to Media3 ExoPlayer's native Sonic AudioProcessor, shifting narrator voice pitch higher or deeper in real time without altering playback speed or tempo.
  - Linked to the Equalizer master switch and preserved across speed adjustments, session starts, and per-book overrides.
  - Full backward-compatible persistence for both legacy 5-field profiles and new 6-field profiles in `EqualizerSettingsImpl.kt`.
- **Uniform Segmented Scope Selector (Global vs. This Book) (`EqualizerBottomSheet.kt`, `PlaybackSpeedBottomSheet.kt`)**:
  - Replaced the ambiguous, unlabeled trailing switch in both the Equalizer and Playback Speed bottom sheets with a prominent Material 3 `SingleChoiceSegmentedButtonRow`: `[ 🌐 All Books (Global) | 📖 This Book Only ]`.
  - Clearly distinguishes between app-wide global default settings and book-specific overrides.
  - In Equalizer, leaves only one single toggle switch on the screen: "Enable equalizer" (the master DSP power switch).

## [1.2.25]

### Added

- **Listen Again: Sort by Last Listened (`ContentSortMode.kt`, `HomePresenter.kt`, `SortModeBottomSheet.kt`, `LibraryPresenter.kt`)**:
  - Added "Last Listened" sort mode, mapping natively to Audiobookshelf's backend `progress` sort key (`mediaProgresses.updatedAt DESC`).
  - Tapping the "Listen Again" shelf header on the home screen now opens the completed books list sorted from last listened to oldest listened to, matching the exact order on the home screen shelf.
  - Keyed `rememberRetainedSaveable(screen)` in `LibraryPresenter.kt` to guarantee screen-specified sort modes and filters are adopted upon navigation.

### Fixed

- **Paging Stabilization & Card Reshuffle Removal (`LibraryUi.kt`)**:
  - Removed `Modifier.animateItem()` from library grid cards and list rows to prevent card sliding animations and micro-stutter during scrolling.
- **Non-Destructive Background Refresh (`LibraryItemRemoteMediator.kt`)**:
  - Check if Page 0 is unchanged on background refresh to avoid clearing SQLite cache and prevent invalidation churn.
  - If Page 0 changed while scrolled down, update only Page 0 instead of wiping subsequent pages from under the user, preventing sudden upward screen snaps.
- **Home Shelf Auto-Scroll on Update (`ShelfListItem.kt`)**:
  - Automatically resets horizontal scroll position to index 0 when new items arrive on "Recently Added", "Recent Series", "Continue Listening", and "Listen Again", so new books appear immediately at the start instead of being hidden off-screen to the left.
- **Multi-Device Sync False-Positive Suppression & 20s Tolerance Threshold (`PlaybackPresenter.kt`)**:
  - Implemented a 20-second significance threshold for server playback synchronization prompts (`delta.absoluteValue >= 20.seconds`), eliminating the 2–4 second flashing "Sync available" popups caused by minor timestamp rounding and network latency.
  - Added active playback echo suppression: when the player is actively playing, server progress updates lagging behind local playback (`delta < 0`) are recognized as server echoes of our own periodic syncs and suppressed. Genuine multi-device progress differences (20+ seconds ahead or behind while paused) continue to prompt as expected.
- **Blank Thumbnail Cover Art Multi-Tier Fallback (`CoverSizingInterceptor.kt`, `CoverUrls.kt`)**:
  - Added a resilient multi-tier fallback pipeline for book and podcast cover thumbnails. If the Audiobookshelf server's sized image rendering (`_200.webp` or `_400.webp`) fails with an HTTP error or zero bytes, the Coil interceptor automatically falls back to the un-sized original image URL, and if that also fails, falls back to the raw uncompressed cover endpoint (`raw=1`). Prevents books with corrupted server-side thumbnail caches from appearing blank in search, library lists, and the mini-player.
- **Live Server Sorting Preservation**:
  - Maintained live, immediate background refresh on screen entry without artificial delay windows, guaranteeing live server date-added sorting remains 100% accurate.

## [1.2.23]

### Added

- **Fast-Forward Button on Collapsed Mini-Player**:
  - Added a dedicated fast-forward button directly to the right of the rewind button on the collapsed playback bar (`[Rewind] [Fast Forward] [Play/Pause]`), using the user-configured forward skip duration.

### Changed

- **Docked Mini-Player & Floating Gap Elimination**:
  - Normalized scroll collapse fractions in `CampfireNavigationBarState` so the collapsed playback bar smoothly glides all the way down to the bottom of the screen with zero gap when the navigation bar hides.
- **Uniform Scroll-to-Hide Navigation Across All Menus**:
  - Switched "Continue Series" and "Downloads" to `adaptiveEnterAlwaysScrollBehavior` and attached them to `LocalNavigationBarState`, making the bottom navigation bar hide on scroll down and reappear on scroll up uniformly across all menus.
- **Instant Background Library Refresh & Up-to-Date Item Counts**:
  - Replaced stale 24-hour cache locks with immediate background refresh in `LibraryItemRemoteMediator`, `SeriesRemoteMediator`, `AuthorsRemoteMediatorFactory`, and `RecentEpisodesRemoteMediator`. Opening "Recently Added", "Recent Series", "Newest Authors", or the Library now displays cached items instantly (0ms) while asynchronously fetching fresh data from Audiobookshelf, updating the item count and displaying newly added books immediately.

### Fixed

- **Cut-Off Cards at List and Grid Bottoms**:
  - Added `CampfireNavigationBarWindowInsets` to `contentWindowInsets` on `ContinueSeriesUi`, `DownloadsUi`, and `SeriesUi`, ensuring the final card in the list or grid is 100% visible and not obstructed behind the bottom navigation or playback bar.
- **Blank Book Cover Art & Click-to-Retry**:
  - Fixed an unconstrained `minWidth` measurement collapse in `CoverImageSlot` that could zero out book cover dimensions, sizing with `maxWidth` clamped to `CoverImageSize`. Added error logging and click-to-retry (`painter.restart()`) on placeholder covers.

## [1.2.22]

### Added

- **Audiobookshelf Real-Time Open Listening Sessions**:
  - Downloaded books played locally on the device now attach a Direct Play server session when online, displaying real-time listening activity under "Open Listening Sessions" on the Audiobookshelf server dashboard.
- **Continue Series Downloaded Indicator**:
  - Added the offline downloaded icon to books in both list and grid cards within the "Continue Series" view, matching the home screen card indicator.

### Changed

- **Eliminated Book Details Back-Navigation Lag**:
  - Removed heavy shared-element bounds transitions on book cover and title when navigating back from Book Details to the Home screen, providing instant, hitch-free navigation.
- **Cellular Download Safeguard Fix**:
  - Reverted cellular download checks to evaluate the active default network, eliminating false-positive "Mobile Data Warning" popups when connected to home Wi-Fi.

### Fixed

- **Audiobookshelf Playback Progress & Session Close Lifecycle**:
  - Restored real-time progress and listening time reporting to the Audiobookshelf server every 15 seconds.
  - Fixed an unhandled HTTP 404 crash during database session hydration on purged/removed server items that previously aborted remote session synchronization.
  - Closed active server sessions immediately on pause, stop, book switch, and completion to prevent lingering zombie sessions on the dashboard.
- **Tolerant Chapter Metadata Validation**:
  - Added container tolerance (+2s start, +5s end) to chapter validation, eliminating false-positive "misaligned chapter metadata" warning banners on standard audiobooks with minor container padding differences.

## [1.2.21]

### Added

- **Server-Synced Audible Series Tracking & Completion Filters**:
  - Seamlessly integrates with Audiobookshelf server Audible series provider endpoints (`/api/libraries/:id/upcoming-releases`, `/api/libraries/:id/missing-books`, `/api/series/:id/provider-entries`).
  - Added Series **Completion** filters (`Has Missing Books`, `Complete`, `Has Upcoming Releases`) and sort options (`Missing Books Count`, `Upcoming Release Date`).
  - Upcoming releases screen syncs directly from Audiobookshelf server with 100% backwards compatibility and instant fallback to local 28-day scanning when connected to vanilla upstream servers.
  - Added top bar status subtitle indicator showing "Synced from server" or "Scanned on device".
- **Upcoming Releases Optimizations**:
  - Implemented 28-day freshness window and delta series scanning to reduce Audible query overhead and save battery.
- **Core Stability & Restored Features**:
  - Restored destructive action confirmations (delete downloaded episode, mark completed).
  - Restored headset and remote media button controls (single click play/pause, double click skip next, triple click skip previous).
  - Restored cellular download warnings and data safeguard dialogs.
  - Restored "What's New" entry in navigation drawer.
  - Fixed account re-authentication and session restore fallback.

## [1.2.20]

### Added

- **Ported Custom Enhancements to Upstream 1.2.1 Base**:
  - Re-architected custom extensions to native Metro DI (`dev.zacsweers.metro`) across all feature modules.
  - Dedicated **Continue Series** and **Offline Downloads** screens with full sorting and filtering.
  - Interactive shelf headings (`›`) on Home feed with fast routing to full screens.
  - In-app **GitHub Updates** (`Cobolt78/Campfire`) with live download progress in drawer and direct APK installation handoff.
  - Dynamic **Speed-Adjusted Book Countdown** on Android Auto (`MediaItemBuilder` & `ChapterWindowForwardingPlayer`).
  - Audiobookshelf-style **Bottom Progress Bars** (6dp, green/orange) across Library, Search, Author, Collection, and Series views.
  - Series book number badges (`#N`) rendered in the top-left corner before the finished checkmark (`[#1] [✓]`).
  - Modernized offline indicator to an industry-standard download tray arrow (`CampfireIcons.Rounded.Download`) tinted green (`#4CAF50`).
  - Media file size in MB displayed on Book & Podcast detail screens.
  - App version displayed on bottom row of navigation drawer.
  - Configurable action confirmations and warn-on-cellular downloads safeguards.
  - Reset sleep timer on pause setting and shake-to-reset haptic confirmation.

### Changed

- **UI & Badge Harmonization**:
  - Refined `#N` series badge to a compact rounded pill tag (`RoundedCornerShape(4.dp)`) with tight padding and snug `5.dp` corner offset to prevent obscuring book cover art.
  - Calibrated badge typography to **11sp bold** on standard cards and **10sp bold** on compact grids and list rows.
  - Arranged `#N` series badge and finished checkmark in TopStart with `#N` positioned before the checkmark (`[#1] [✓]`).
  - Book Detail series section positioned above Community Ratings & Reviews for faster navigation.
  - Adopted upstream 1.2.1 native decibel volume fading (`VolumeFadeController`).
  - Home feed Discoveries refresh bypasses server cache with millisecond timestamp parameters.

### Fixed

- **Stability & Performance**:
  - Fixed podcast libraries failing to load in Home, Shows, and Latest tabs when a podcast contains a blank or non-ISO release date (e.g. from RSS feed or OPML import).
  - Handled missing `google-services.json` gracefully on custom release builds to eliminate startup initialization crashes.
  - Throttled SQLite progress writes during active playback (15s progress, 5s session time) to eliminate background coroutine CPU pinning and battery drain.
  - Replaced N+1 series queries with batch query `selectForMultipleSeries` offloaded to `databaseRead` dispatcher, preventing UI freezes and ANRs.
  - Fixed Android Auto next/previous track controls to always skip chapters while preserving dedicated 30s jump buttons.
  - Calibrated shake-to-reset thresholds and sensor delay.
  - Widened equalizer bottom sheet label column to 90.dp to prevent word wrapping.

## [1.2.1]

### Added

- [Desktop] Install and update on macOS with Homebrew: `brew install r0adkll/tap/campfire`
- Campfire doesn't try to reach a server on your home network over mobile data, and retries less and less often while your server can't be reached (Settings → Connection)
- [Android] Download on Wi-Fi only: downloads wait for Wi-Fi and pause on mobile data (Settings → Downloads)
- Edit the custom headers sent to your server, such as a reverse-proxy or access-gateway token, in Settings → Connection without signing out; live updates now send them too
- [Android] Settings → Connection shows when Android's local network permission is blocking your home server, with a shortcut to allow it
- [Android] Your settings, custom themes and campsites come back when you reinstall or move to a new phone; pick a campsite on the login screen to sign back in
- [Android] Save your password to your password manager when you sign in, and sign back in with one tap when it's needed again (Android 14 and later)
- [Android] Stay signed in when you reinstall Campfire or move to a new phone (Google Play version; turn off in Settings → Account)
- Mark a whole series as finished, or as not finished to start it over, from the menu on the series screen
- Mark a whole playlist as finished or not finished from its ⋮ menu, and see each book's series and number in the playlist

### Changed

- Listening progress recorded while your server was unreachable uploads as soon as it can be reached again, instead of waiting for your next play or pause
- Reworked the full-screen player layout so the cover art stays as large as the screen allows, with more compact transport controls on shorter screens
- Chapters, bookmarks, speed and the other player controls now open in a panel beside the player when your phone is sideways or your foldable is half-open, instead of a bottom sheet with room for barely two rows
- Chapter and book titles too long to fit in the player now scroll, fading out at the edges, instead of being cut off (turn off with Scroll long titles in Settings → Playback)
- [Android] Sign-ins and Hardcover tokens are now encrypted with a key kept in your device's keystore, replacing the deprecated encrypted storage; you stay signed in through the update
- Each account keeps its own library sorting and layout
- [Android] Slightly faster startup when signed in
- Books on the series screen show their number in the series and release year, matching the books missing from your library

### Fixed

- [Android] Skipping back being recorded in playback history with the skip-forward amount
- [Desktop] Campfire no longer contacts Google, GitHub, or Bing to check whether you're online
- Battery drain and slow screens while your server is unreachable, such as when you're away from your home network
- The player squashing into the left of the screen when a phone is turned sideways, instead of laying out with the cover beside the controls
- Books that belong to more than one series being ordered by their number in a different series
- Marking a book you've started listening to as finished not showing it as finished until Campfire restarts
- [Android] The screen shifting up and leaving a band of empty space above the keyboard when typing with a phone turned sideways
- [Android] New skip forward and back times only taking effect after restarting the app
- Adding an account for a server you're already signed in to replacing the existing account; Campfire now asks you to sign out of it first

## [1.2.0]

### Added

- [Desktop] Now Playing on macOS shows the current chapter and cover art, and media keys, AirPods taps, and Control Center controls work
- [Desktop] Plays through a built-in audio engine, so VLC no longer needs to be installed
- [Desktop] Ton of Desktop centered UI improvements
- [Desktop] Send playback to a specific audio output device instead of always following the system default (macOS and Windows)
- [Desktop] Resizable and compact mini-player that stays always on-top
- [Android] Foldable phones opened halfway on a table split the screen while something is playing: the app above the hinge, the full player below it
- Pull down on the Home, Playlists and Collections screens to refresh them

### Changed

- [Desktop] Window can no longer be resized below a phone-sized layout
- [Desktop] Playback bar is shorter and shows progress through the whole book, with chapter and bookmark markers you can hover and click
- [Desktop] Playback bar docks along the bottom of any tablet-width or wider window instead of only ultra-wide ones
- [Android, Desktop] Search results on tablet and desktop layouts close once you pick one
- [Desktop] Built-in themes render on neutral cool-grey surfaces with a bolder version of the theme's colour as the accent, instead of tinting every surface
- Download options now respect server-side permission for the user
- Improved Pull-to-refresh indicator and animation

### Fixed

- Titles removed from the server lingering in the app, including their downloads, or showing up twice after being re-added
- [Desktop] App freezing while opening a chapter, bookmark, or track, and while starting playback
- Expanded search results on wide layouts spilling past the search bar's edge instead of lining up under it
- Navigation drawer always highlighting Home instead of the section being viewed
- Item detail showing a stray download button while the item is playing or already downloading, and a mismatched play button shape while a download is in progress
- Cover art on the playing screen flickering and reloading while dragging the player up or down
- Book covers flashing a loading spinner when opening an item from home, a library, or a series
- [Android] Cast device picker opening away from button in foldable/tablet layouts
- [Android] App crashing when a Cast device appears on the network
- App crashing when a request finishes after logging out or while signing in
- [Android] Briefly going silent between chapters of single-file audiobooks encoded with xHE-AAC
- Sleep timer's Time and End of Chapter buttons having mismatched sizes when a label wraps onto two lines

### Other Notes & Contributions

- [Android] Beta builds are published to Firebase App Distribution again with every release, reaching testers without waiting on Google Play review

## [1.1.0]

### Added

- Playback settings for how often listening progress syncs to the server, with separate intervals for Wi-Fi and mobile data
- HLS streaming on Android, with a Streaming method setting (Auto, Direct play, or Prefer HLS) for smoother seeking in large single-file audiobooks
- Playback speed improvements: a per-book speed that doesn't change the global speed, and customizable quick speed options (0.5x to 3x) in Settings → Playback
- Sound equalizer on the playing screen (Android and desktop) with presets, 10-band adjustment, loudness and bass boost, and per-book profiles
- Ratings & Reviews on the book detail page from Audible and Open Library, or from a linked Hardcover account, with a source switcher per book and a Book Info page in Settings to manage sources
- Upcoming page and home shelf with a release timeline of announced books from your series, and series pages now show released books missing from your library, both checked against Audible
- Statistics page: a daily listening heatmap for the past year, plus an Activity section with listening streaks, finished counts, and daily averages
- Sleep timer fade-out duration can be set in Settings → Sleep (off to one minute), and the fade now follows a smoother logarithmic curve

### Changed

- Automatic rewind can now be set anywhere from 0s to 2m, and the ignore-pauses window now goes up to 30s
- Refreshed icons throughout the app with the updated Material Symbols style
- Statistics load instantly from a short-lived cache, with a refresh button in the top bar for the latest numbers

### Fixed

- Chromecast: audiobooks now play, devices appear reliably (including after rotating the screen), the end-of-chapter sleep timer stops at chapter boundaries, and playback no longer stops after about an hour on Audiobookshelf 2.22.0 or newer
- Listening progress and finished indicators missing from book cards on series, author, collection, and search screens
- Android Auto and the media notification now show the book title with the chapter instead of only the chapter and author
- Recent listening bar chart on the Statistics page scaling against all-time highs instead of the days shown
- Playback timing and progress showing blank after reopening the app until playback started
- App widgets breaking due to large image size
- Sleep timer countdown scrim not covering the whole cover art on the playing screen

### Other Notes & Contributions

- The cast device list offers to request local network access on Android 17+ when it may be needed to find devices

## [1.0.5]

### Other Notes & Contributions

- FOSS (F-Droid) builds are now byte-for-byte reproducible

## [1.0.4]

### Fixed

- What's New screen could occasionally ship without its changelog content

### Other Notes & Contributions

- Made the FOSS (F-Droid) build reproducible while keeping the baseline profile for startup performance

## [1.0.3]

### Fixed

- Mini player cover art no longer stays blank while a book is playing

### Other Notes & Contributions

- Releases now publish to Google Play automatically: release candidates to open testing, final versions as a staged production rollout

- `scripts/release` is now a guided interactive flow that keeps Gradle output in a log file and helps resolve blockers such as an over-limit store changelog

## [1.0.2]

### Added

- Tap the speed value in the playback speed sheet to type an exact playback speed

### Changed

- Cover art is now requested at the size it is displayed, so full-screen player and detail artwork is sharp instead of upscaled

### Fixed

- App freezing on the search empty state when system animations are disabled (e.g. the "Remove animations" accessibility setting)
- Series with decimal sequence numbers (1.1, 1.2, …) now sort in the correct order instead of jumping to the end
- Covers updated on the server now refresh in the app instead of showing the old image until the cache expires
- Download and file sizes showing two decimal places (e.g. 1.00 MB) instead of one

### Other Notes & Contributions

- Added `scripts/release` to cut releases in one step; the Release workflow now bumps `campfire.version` automatically after a release
- Alpha build release notes now describe the PR that produced the build instead of the cumulative Unreleased changelog
- Baseline profiles are generated once per release (on emulator.wtf in CI, or locally via `scripts/release`) instead of once per flavor on a hosted-runner emulator

## [1.0.1]

### Fixed

- Crash when the server is unreachable or an item was deleted while opening Settings or resuming playback
- Crash when resuming downloads on app launch on some Android 12+ devices
- Crash when opening Android Auto settings on devices without Android Auto installed
- Playback resumption from Bluetooth / media buttons killing the app when the server is offline
- Playback speeds between x.01 and x.09 displaying incorrectly (e.g. 1.03x showing as 1.3x)

### Other Notes & Contributions

- Fixed the Google Play release bundle build failing on a lint check

## [1.0.0]

### Added

- Google Play builds can now update the app in-place when a new version is available on the Play Store

### Changed

- The bluetooth next/previous settings default is now to fast-forward / rewind

### Fixed

- Books belonging to multiple series now show all of their series on the book detail screen
- App freezing completely when connecting to Android Auto
- Logging in with an account that has no library access now explains the problem instead of incorrectly reporting bad credentials

### Other Notes & Contributions

- The on-device AI theme builder is now an optional component, laying the groundwork for fully FOSS builds for F-Droid
- Added a new FOSS build variant with no Google or analytics components, in preparation for F-Droid and IzzyOnDroid distribution
- Google Cast support is now an optional component excluded from the FOSS build variant
- Release version numbers are now derived from the release tag so builds are reproducible
- Refreshed the in-app open source license list, which now accurately reflects each build variant's dependencies

## [1.0.0-rc3]

### Fixed

- Crash when the server sends large or malformed realtime sync updates, such as progress updates for accounts with a long listening history

## [1.0.0-rc2]

### Fixed

- Crash on app launch when opening the app while logged out or when your account needs to be re-authenticated

## [1.0.0-rc1]

### Added

- List/grid display toggle for the Series, Collections, and Playlists screens
- Optional overall book progress indicator with time remaining in the player, toggleable in Playback settings
- Setting to disable the wavy playback seek bar animation
- Quick-insert bar above the keyboard when typing the server address at login, with one-tap snippets for https://, http://, local network prefixes, and the default Audiobookshelf port
- Server addresses no longer require typing http(s):// — the login screen detects the right scheme automatically and fills it in
- eBook-only library items now show a format badge on their covers and their play, download, and queue actions are disabled since reading eBooks isn't supported yet
- Android Auto tabs now adapt to the active library: podcast libraries get a Shows tab for browsing every podcast and its episodes, hide the book-only Series, Authors, and Collections tabs, and the tab list refreshes live when switching libraries or changing Android Auto settings

### Changed

- Collection, series, and playlist cards now show an item-count badge in the corner of the cover
- Server addresses are now redacted from diagnostic logs and crash reports to better protect your privacy
- The tent picker on the login screen now offers the app's default themes, previews your choice live, and applies it as your app theme after signing in

### Deprecated

### Removed

- The per-server tent color: the tent picker in Account settings is gone and campsite icons now share one look — theming is handled entirely by the theme picker

### Fixed

- Offline playback of downloaded audiobooks failing when the server is unreachable
- Crash when the server sends a real-time library update containing unusual line-break characters (seen with some podcast descriptions)
- Crash on launch when more than one account on the device had listening progress for the same book
- Server addresses that resolve to a local IP through custom DNS being rejected on Android 16+ (the local network permission is now requested for them)
- Campfire logo flame flickering during the Welcome to Login screen transition
- Real-time sync socket not connecting after logging in until the app was restarted
- Podcasts in Android Auto can now be browsed into their episode list, and episode entries show the episode's title, artwork, and duration and play the correct episode instead of failing to start
- Android Auto category tabs hanging indefinitely instead of showing an empty list when the library has no playlists, series, authors, or downloads
- Android Auto browse and search lists now respect the head unit's requested page size instead of returning every item at once, fixing truncated lists in large libraries

### Other Notes & Contributions

## [0.13.1-beta]

### Added

- Auto-rewind configurable durations after paused

### Fixed

- Missing runtime permission on Android 17 when connecting to local network addresses

## [0.13.0-beta]

### Added

- **Podcasts**
  - Add new podcasts
  - Find and add new episodes
  - Server download queue
  - Latest episodes screen
- **Socket Connection**
  - Now supports the socket.io connection to the server
  - Near-immediate syncing of item, series, playlists, collection, author changes from the server or other devices
  - Improved progress syncing
  - Live podcast episode download queue / notifications
- New AI theme generator using local-only Gemini Nano via Halogen

### Changed

- Change the theme mode or open the theme picker directly from the Drawer

### Fixed

- Phone Landscape now has a friendlier UI/UX

## [0.12.0-beta]

### Added

- Playback History
- Multi-device progress synchronization
- Setting to disable marquee scrolling on library item cards
- Android Auto settings pane: toggle, reorder, and choose list/grid layout per top-level category
- 37 new theme icons (camping/nature/travel) selectable when building a custom theme

### Changed

- Already listened to chapters on items in-progress are collapsed by default
- Overhauled playback homescreen widget UI
- Theme color extraction from cover art now waits for a brief impression before dispatching, avoiding wasted work when scrolling past items
- Time remaining on collapsed bar + item detail now reflect the current playback speed of a session
- Improved home feed scrolling with mixed scrolling orientations

### Fixed

- Cast / Output device list not always showing available / current device(s)
- Fixed media progress not syncing during playback
- Fixed playback sessions dying due to token expiration
- Device information reported as 'Unknown' for sessions on the server
- Cover art not always rendering in Android Auto

### Other Notes & Contributions

## [0.11.0-beta]

### Added

- What's New / Changelog screen and widget
- Playback Queue
- Playlists
- Setting to switch next/prev actions from remotes (i.e. Bluetooth) to fast forward / rewind
- Support the [playback resumption][media3-playback-resumption] API

### Changed

- Revamped the sleep timer bottom sheet UI in the player view
- Warnings when an item has misaligned chapter information that needs to be corrected on the server
- Rewind action will jump to previous chapter with the rewind duration from the end instead of just to the start of the item
- Default playback speed options changed to 1x, 1.1x, 1.25x, 1.5x, 2x. Slider range remains 0.5x to 2x.

### Fixed

- Bottom navigation bar now transitions smoothly between shared screen transitions
- Series on the item detail page not appearing until loading them in the series screen
- App crashing on system reboot due to offline download resumption
- Mark Finished / Discard Progress would pause playback of current item
- Playback issues when chapter information is mis-aligned with the audio tracks
- Bluetooth metadata not showing the title of the book

## [0.10.0-beta] - 2026-02-01

### Added

- Paging Support! Library items, series, and authors are all now paginated for large library support
- Item progress now displays in contexts other than the Home feed

### Changed

- "Play/Continue listening" button on item details is now disabled if it is the current play session

### Fixed

- Crash when opening series detail page
- Death loop when using multiple accounts and all refresh tokens expire
- Collection book ordering not being consistent with web/server
- Fast Forward / Rewind not working on Android Auto
- Collections not being editable if you were a "root" user
- Playback view now maintains expansion state on configuration changes

## [0.9.0-beta] - 2026-01-19

### Added

- OIDC Authentication
- Offline downloads accessible from side drawer
- Denser grid option for the Library screen

### Changed

- Switched underlying authentication to use bearer auth for improved security.
- Streaming / Image requests now use HTTP headers for authorization.
- Moved palette picker on item detail from cover image to top bar
- Improved navigation shared element animations

### Fixed

- Analytics consent screen no longer re-appears after accepting it
- Predictive back navigation causing screens to infinitely load
- Analytics consent screen re-appearing after accepting it
- Search overlay popping up when adding bookmarks
- Bug where home feed would be empty when using multiple accounts/libraries
- Library filter(s) now persist between screens and configuration changes

## [0.8.0-beta] - 2025-12-16

### Added

- New dynamic content theming based on an item's thumbnail.
- Ability to change the seed color for per-item theming
- Settings to enable/disable dynamic content based theming

### Changed

- Material 3 Expressive UI overhaul
- Rewrote item detail screen for performance
- Item detail screen UI polishing.
- Moved 'tent' choice/theming to 'Appearance' settings tab
- Moved 'Collections' to the left-drawer from bottom nav bar
- Editing collections now only shows for server admins

### Removed

- Top bar library switcher. This is now located only in the left-hand drawer.

### Fixed

- Incorrect stat % trend rendering when negative from the previous week
- Series not always appearing on item details.
- Inconsistent series book ordering
- Series disk cache getting deleted when leaving series screen
- Crash due to null 'duration' value in MediaProgress
- Crash due to float 'time' in Bookmark responses
- Crash due to missing or misaligned chapters during playback / ui
- Crash due to no user stat information
- Home feed not syncing / displaying correctly

## [0.7.2-beta] - 2025-11-06

### Fixed

- Fixed metadata parsing bug causing some items and collections to not load in the UI

## [0.7.1-beta] - 2025-11-06

### Fixed

- Gracefully handle Cast/PlayService integrations on devices that don't have Google/Play Services

## [0.7.0-beta] - 2025-11-06

### Added

- Support for Google Cast
- Manage offline downloads in settings screen

### Changed

- Refactored how the Home screen observes its data
- Release notes display in in-app update card
- In-app update now shows progress
- "Mark as (Not) Finished" is always visible

### Fixed

- Opening/Closing foldables no properly manages detail screens.
- Screen jank when app widget is present and playing a book
- How the homescreen loads its data leading to blank / flickering items

## [0.6.2-beta] - 2025-10-31

### Fixed

- Fixed model parsing in Home feed and collection views
- Fixed crashing when pressing system back button

## [0.6.1-beta] - 2025-10-30

### Fixed

- Fixed issue with how data was being parsed on the home feed

## [0.6.0-beta] - 2025-10-30

### Added

- Usage analytics via MixPanel (Android Only)
- Reporting consent screen to opt-out of developer and usage reporting
- More debugging to help diagnose infinite loading screens

### Changed

- Disabled Firebase in debug builds
- Capitalize "Chapter" in all "End of Chapter" text

### Fixed

- Improved UI performance during playback
- Playback speed now displays down to the hundredth place
- Improved sleep timer display of long chapters
- Chapter time in the playback UI scales with playback speed
- Series books are now more consistently sorted in the correct order
- Issue where Play/Pause would become disabled when session is loaded
- In-app updating via Firebase App Distribution for beta and alpha builds

### Other Notes & Contributions

- Reached database stability. From this release onwards re-installs won't be required.

## [0.5.0-alpha] - 2025-10-12

### Added

- Library item filtering
- Save playback speed across listening sessions
- Toggle between time in book and chapter length in Now Playing chapter list
- Item detail author/narrator now clickable
- Shared animation transitions throughout entire app
- Current library picker added to drawer screen

### Changed

- Item Detail UI Adjusted to be more streamlined for downloads
- Improved empty and default states in Author UI
- Author placeholder image is now gender neutral

### Fixed

- Navigating from search for Narrators, Genres, and Tags now works
- Logging out & Changing accounts
- Status bar now respects app theme

## [0.4.0-alpha] - 2025-07-07

### Added

- Android Auto Support
- Toggle on item detail to switch between time in book, and chapter length

### Changed

- When playback speed is not 1x, display the actual speed in the actions bar
- Dynamic timer icon when timer is enabled in playback UI
- Time remaining in the playback UI now adjusts based on playback speed
- Improved the homescreen widget appearance and actions
- Widget can now expand vertically showing the list of chapters for the current item.

### Fixed

- HTML is now rendered properly on the item detail page description
- Fixed sleep timer fading-to-pause when set as "End of Chapter"
- Fixed how PlayMethod not reporting offline play correctly

## [0.3.0-alpha] - 2025-07-02

### Added

- Library switcher UI for changing your selected library.
- Offline status indicators on all surfaces that show library items.
- Android home screen widget to control playback

### Fixed

- Fix issue with non-ssl traffic for self-hosted servers not behind https

## [0.2.1-alpha] - 2025-06-22

### Added

- Download confirmation dialog
- Download settings panel
- Android permissions check for download notification
-
### Fixed

- Fixed [#237](https://github.com/r0adkll/Campfire/issues/237) whens syncing media progress after login

## [0.2.0-alpha] - 2025-06-21

### Added

- Ability to add books to collections, or create new collections
- Ability to remove books from collections, and delete collections
- Offline download support for Android

### Fixed

- Loading indicators not showing on series/collections screens for initial load
- Crash on the 'Statistics' page when previous week didn't have any data

## [0.1.0-alpha] - 2025-06-15

### Added

- Initial Alpha Release.

[1.2.1]: https://github.com/r0adkll/Campfire/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/r0adkll/Campfire/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/r0adkll/Campfire/compare/1.0.5...1.1.0
[1.0.5]: https://github.com/r0adkll/Campfire/compare/1.0.4...1.0.5
[1.0.4]: https://github.com/r0adkll/Campfire/compare/1.0.3...1.0.4
[1.0.3]: https://github.com/r0adkll/Campfire/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/r0adkll/Campfire/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/r0adkll/Campfire/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/r0adkll/Campfire/compare/v1.0.0-rc3...v1.0.0
[1.0.0-rc3]: https://github.com/r0adkll/Campfire/compare/v1.0.0-rc2...v1.0.0-rc3
[1.0.0-rc2]: https://github.com/r0adkll/Campfire/compare/v1.0.0-rc1...v1.0.0-rc2
[1.0.0-rc1]: https://github.com/r0adkll/Campfire/compare/v0.13.1-beta...v1.0.0-rc1
[0.13.1-beta]: https://github.com/r0adkll/Campfire/compare/v0.12.0-beta...v0.13.1-beta
[0.13.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.12.0-beta...v0.13.0-beta
[0.12.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.11.0-beta...v0.12.0-beta
[0.11.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.10.0-beta...v0.11.0-beta
[0.10.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.9.0-beta...v0.10.0-beta
[0.9.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.8.0-beta...v0.9.0-beta
[0.8.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.7.2-beta...v0.8.0-beta
[0.7.2-beta]: https://github.com/r0adkll/Campfire/compare/v0.7.1-beta...v0.7.2-beta
[0.7.1-beta]: https://github.com/r0adkll/Campfire/compare/v0.7.0-beta...v0.7.1-beta
[0.7.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.6.2-beta...v0.7.0-beta
[0.6.2-beta]: https://github.com/r0adkll/Campfire/compare/v0.6.1-beta...v0.6.2-beta
[0.6.1-beta]: https://github.com/r0adkll/Campfire/compare/v0.6.0-alpha...v0.6.1-beta
[0.6.0-beta]: https://github.com/r0adkll/Campfire/compare/v0.5.0-alpha...v0.6.0-beta
[0.5.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.4.0-alpha...v0.5.0-alpha
[0.4.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.3.0-alpha...v0.4.0-alpha
[0.3.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.2.1-alpha...v0.3.0-alpha
[0.2.1-alpha]: https://github.com/r0adkll/Campfire/compare/v0.1.0-alpha...v0.2.1-alpha
[0.2.0-alpha]: https://github.com/r0adkll/Campfire/compare/v0.1.0-alpha...v0.2.0-alpha
[0.1.0-alpha]: https://github.com/r0adkll/Campfire/releases/tag/v0.1.0

[media3-playback-resumption]: https://developer.android.com/media/media3/session/background-playback?utm_source=android-studio-app&utm_medium=app#resumption
