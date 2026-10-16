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

### Other Notes & Contributions

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
