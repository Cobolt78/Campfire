# Antigravity Rules for Campfire (Cobolt78/Campfire)

## Workflow & GitHub Integration Invariants

1. **Automatic Git Tracking & Commits**:
   - Every time code changes or fixes are implemented and verified, automatically stage and commit them to the local Git repository on branch `v1.1.0-custom`.
   - Automatically push commits to GitHub: `git push origin v1.1.0-custom` (`https://github.com/Cobolt78/Campfire.git`).
   - Automatically update `CUSTOM_MODIFICATIONS.md` and regenerate `custom_changes.patch` using `git diff > custom_changes.patch` so that patch files are always 100% in sync with zero drift.

2. **Releases & Binary Distribution**:
   - When compiling a release, build the release APK(s) using `./gradlew.bat :app:android:assembleFossRelease` (and/or `:app:android:assembleStandardRelease` if requested).
   - Name the release APKs using the versioned format: `android-foss-release_<version>.apk` and `android-standard-release_<version>.apk` (e.g. `android-standard-release_1.1.0.apk`).
   - Publish or update the release on GitHub via GitHub CLI (`gh release upload <tag> <file> --clobber --repo Cobolt78/Campfire`). If replacing an existing release asset with a different filename, delete the stale asset first.
   - If the user's phone (`RFCX816WP0Z`) is connected via USB, automatically install the updated APK via `adb install -r`.

3. **Repository Integrity**:
   - Do NOT delete `CHANGELOG.md` (it is required at build time by `GenerateChangelogTask` to generate the in-app "What's New" dialog).
   - Maintain a clean baseline against upstream `https://github.com/r0adkll/Campfire.git`.

4. **Mandatory Version Upgrade & Feature Retention Protocol**:
   - **Never Silently Drop a Feature**: When migrating to a new upstream release (e.g., v1.1.1, v1.2.0, v2.0+), NEVER assume an upstream change supersedes, replaces, or conflicts with any custom modification without performing an explicit audit and confirming with the user.
   - **Mandatory Section-by-Section Audit**: Every single numbered section in `CUSTOM_MODIFICATIONS.md` (Sections 1 through 11+) must be checked off in an explicit audit list before any upgrade is finalized:
     1. The code for the feature must be verified as actively present and compiling in the new codebase.
     2. If upstream modified a subsystem touching our features (such as Android Auto, player architecture, sleep timer, or home shelves), verify that the *exact* custom user experience (e.g., speed-adjusted total countdown, volume fade curves) is preserved. If not identical, adapt the custom feature to sit on top of upstream's new architecture.
     3. If an upstream feature genuinely makes a custom modification redundant, or if an architectural conflict arises, STOP and explicitly present the options to the user in chat. Never unilaterally omit a feature.
   - **Patch & Inventory Verification**:
     - Cross-check the modified file list in the new version against `CUSTOM_MODIFICATIONS.md` (Section 11: Modified Files Inventory) and the previous version's `custom_changes.patch`.
     - If any file or feature that previously had custom code is untouched in the new version, treat it as an immediate regression alert that must be resolved before cutting a release.
   - **Lockstep Synchronization**:
     - Keep `CUSTOM_MODIFICATIONS.md` and `custom_changes.patch` in 100% sync with the actual repository code at all times.
