Rebased onto official **Campfire v1.2.0** while preserving, refining, and extending all custom enhancements. 

*Note: Upstream 1.2.0 natively adopted our custom Pull-to-Refresh, Search Cache Invalidation, Sleep Timer Fade-Out curves, and Shake-to-Reset sensitivity! We have seamlessly transitioned to the official implementations for these features while maintaining the rest of our custom suite.*

#### 🎧 Dynamic Continue Listening & Offline Downloads
- **Dynamic Last-Played Sorting**: The Continue Listening shelf now automatically sorts in realtime by most recently played (lastUpdate descending). Whichever book you're currently listening to or just paused smoothly glides straight to the #&#8203;1 spot at the front of the shelf using Compose item animations.
- **Downloaded Books Integration**: In-progress downloaded books now seamlessly appear on the Continue Listening shelf, even when listening completely offline or before server synchronization.
- **Finished Book Pruning**: Completed books automatically drop off the shelf as soon as they are finished.

#### 🚗 Android Auto & Media Controls
- **Dynamic Speed-Adjusted Total Book Countdown**: In Android Auto and system media notifications, the total chapter time next to the chapter name is replaced with the live speed-adjusted total book remaining countdown (e.g., Chapter 134 - 18h 45m left). Successfully adapted this injection to work alongside the new upstream 1.2.0 gapless playback and isSingleItemQueue architecture changes.

#### 🎨 Progress Bar & Visual Refinements
- **High-Contrast Dynamic Progress Bars**: Continued use of the neutral, semi-transparent dark scrim (50% translucent black) track for progress bars across all shelves, lists, search, and collections, ensuring the dynamic Material 3 theme color stands out sharply against any cover artwork.

#### ⚡ Sleep Timer & Core Improvements
- **Reset Timer on Pause (User Configurable)**: Retained the option in **Settings → Sleep**. When enabled, pausing audio (via earbuds, lockscreen, notification, or in-app button) automatically resets the sleep timer back to its full original duration, ready for when playback resumes.
- **Home Downloads Shelf (Synthetic Shelf Architecture)**: Successfully merged the synthetic Downloads shelf into the new upstream 1.2.0 HomeUiState rendering architecture, preserving zero-database-churn observation.
- **Network Observers**: Preserved the live ConnectivityManager.NetworkCallback dynamic mobile data detection.

#### 🛡️ Stability & Open-Source Build Fixes
- **Firebase Initialization Crash Guard**: Successfully carried over the Firebase Crashlytics initialization guards to gracefully allow standard release compilation without Google's private credentials.

---
**Assets Included:**
- cobolt-campfire-standard-release_1.2.0.apk: Full release with Google Cast and AI theme generation support.
- cobolt-campfire-foss-release_1.2.0.apk: 100% FOSS release for F-Droid / tracker-free installations.
