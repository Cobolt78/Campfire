// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.AppStateSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestAppStateSettings(
  private val testScope: CoroutineScope = TestScope(UnconfinedTestDispatcher()),
) : TestSettings(), AppStateSettings {

  override var hasShownWidgetPinning: Boolean by boolean()
  override fun observeHasShownWidgetPinning(): StateFlow<Boolean> =
    observeBoolean(::hasShownWidgetPinning)
      .stateIn(testScope, SharingStarted.Lazily, hasShownWidgetPinning)

  override var lastSeenVersion: String? by stringOrNull()
  override fun observeLastSeenVersion(): StateFlow<String?> =
    observeStringOrNull(::lastSeenVersion)
      .stateIn(testScope, SharingStarted.Lazily, lastSeenVersion)

  override var appUpdateSignInDismissed: Boolean by boolean()
  override fun observeAppUpdateSignInDismissed(): StateFlow<Boolean> =
    observeBoolean(::appUpdateSignInDismissed)
      .stateIn(testScope, SharingStarted.Lazily, appUpdateSignInDismissed)

  override var appUpdateDismissedVersionCode: Long by long()
  override fun observeAppUpdateDismissedVersionCode(): StateFlow<Long> =
    observeLong(::appUpdateDismissedVersionCode)
      .stateIn(testScope, SharingStarted.Lazily, appUpdateDismissedVersionCode)

  override var lastAppUpdateCheckTimestamp: Long by long()
  override fun observeLastAppUpdateCheckTimestamp(): StateFlow<Long> =
    observeLong(::lastAppUpdateCheckTimestamp)
      .stateIn(testScope, SharingStarted.Lazily, lastAppUpdateCheckTimestamp)

  override var cachedUpdateVersionCode: Long by long()
  override fun observeCachedUpdateVersionCode(): StateFlow<Long> =
    observeLong(::cachedUpdateVersionCode)
      .stateIn(testScope, SharingStarted.Lazily, cachedUpdateVersionCode)

  override var cachedUpdateVersionName: String? by stringOrNull()
  override fun observeCachedUpdateVersionName(): StateFlow<String?> =
    observeStringOrNull(::cachedUpdateVersionName)
      .stateIn(testScope, SharingStarted.Lazily, cachedUpdateVersionName)

  override var cachedUpdateDownloadUrl: String? by stringOrNull()
  override fun observeCachedUpdateDownloadUrl(): StateFlow<String?> =
    observeStringOrNull(::cachedUpdateDownloadUrl)
      .stateIn(testScope, SharingStarted.Lazily, cachedUpdateDownloadUrl)

  override var cachedUpdateReleaseNotes: String? by stringOrNull()
  override fun observeCachedUpdateReleaseNotes(): StateFlow<String?> =
    observeStringOrNull(::cachedUpdateReleaseNotes)
      .stateIn(testScope, SharingStarted.Lazily, cachedUpdateReleaseNotes)
}
