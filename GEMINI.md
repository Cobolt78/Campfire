# Antigravity Rules for Campfire (Cobolt78/Campfire)

## Workflow & GitHub Integration Invariants

1. **Automatic Git Tracking & Commits**:
   - Every time code changes or fixes are implemented and verified, automatically stage and commit them to the local Git repository on branch `v1.05-custom`.
   - Automatically push commits to GitHub: `git push origin v1.05-custom` (`https://github.com/Cobolt78/Campfire.git`).
   - Automatically update `CUSTOM_MODIFICATIONS.md` and regenerate `custom_changes.patch` using `git diff > custom_changes.patch` so that patch files are always 100% in sync with zero drift.

2. **Releases & Binary Distribution**:
   - When compiling a release, build the release APK(s) using `./gradlew.bat :app:android:assembleFossRelease` (and/or `:app:android:assembleStandardRelease` if requested).
   - Publish or update the release on GitHub via GitHub CLI (`gh release upload ... --repo Cobolt78/Campfire`) so that the user can download the APKs directly from `https://github.com/Cobolt78/Campfire/releases`.
   - If the user's phone (`RFCX816WP0Z`) is connected via USB, automatically install the updated APK via `adb install -r`.

3. **Repository Integrity**:
   - Do NOT delete `CHANGELOG.md` (it is required at build time by `GenerateChangelogTask` to generate the in-app "What's New" dialog).
   - Maintain a clean baseline against upstream `https://github.com/r0adkll/Campfire.git`.
