# Antigravity Rules for Campfire (Cobolt78/Campfire)

## Workflow & GitHub Integration Invariants

1. **Automatic Git Tracking & Commits**:
   - Every time code changes or fixes are implemented and verified, automatically stage and commit them to the local Git repository on the active custom branch (e.g. `v1.2.21-custom`).
   - Automatically push commits to GitHub: `git push origin <active-branch>` (`https://github.com/Cobolt78/Campfire.git`).
   - Automatically update `CUSTOM_MODIFICATIONS.md` and regenerate `custom_changes.patch` using `git diff <upstream-merge-base> ':!custom_changes.patch' > custom_changes.patch` so that patch files are always 100% in sync with zero drift.

2. **Releases & Binary Distribution**:
   - When compiling a release, build the release APKs using `./gradlew.bat :app:android:assembleFossRelease` and `./gradlew.bat :app:android:assembleStandardRelease`.
   - **EXACTLY TWO APK ASSETS INVARIANT**:
     - Every GitHub Release MUST contain strictly **ONLY TWO APK files**:
       1. `campfire-foss-release.apk`
       2. `campfire-standard-release.apk`
     - Plus `custom_changes.patch` (diff against upstream).
     - **NEVER** upload multiple duplicates, version-numbered aliases (e.g. `android-foss-release_1.2.21.apk`), or redundant copies under different names. Exactly two APK files total per release.
   - Publish or update the release on GitHub via GitHub CLI (`gh release create <tag> ... --repo Cobolt78/Campfire` or `gh release upload <tag> ... --clobber --repo Cobolt78/Campfire`).
   - If an existing release contains duplicate or stale assets, immediately remove them with `gh release delete-asset <tag> <asset> --repo Cobolt78/Campfire -y` so only the two APKs and the patch remain.
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

5. **Refactoring Discipline & 'Propose, Don't Sneak' Invariant**:
   - **Strict Blast-Radius Discipline**:
     - When fixing a specific bug or porting an existing feature, touch ONLY what is strictly required to solve that problem.
     - NEVER rewrite working code, replace proven simple checks with speculative reactive listeners, or make unsolicited architectural changes on a whim.
     - If code worked reliably in previous versions, port it identically without unrequested "enhancements".
   - **Freedom to Improve (With Prior User Approval)**:
     - If a cleaner, simpler, more performant, or more robust approach is identified, PROPOSE it to the user explicitly in chat first.
     - Explain what the improvement is, why it is better, and what the potential trade-offs or risks are.
     - Wait for the user's explicit approval before implementing any architectural or logic refactor. Never sneak refactors into unrelated bug fix commits.
   - **Button-Action Check Principle**:
     - For one-shot user actions (like pressing "Download" or opening a menu), evaluate system state (such as network connectivity) directly at the moment of the action rather than running persistent background callbacks or listeners that risk desynchronization or background radio conflicts.
