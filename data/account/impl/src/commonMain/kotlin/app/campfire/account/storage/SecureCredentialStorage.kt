// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.storage

import app.campfire.account.api.UserCredentials
import app.campfire.account.settings.TokenSettings
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.model.UserId
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.toSuspendSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class SecureCredentialStorage(
  @TokenSettings private val tokenSettings: Settings,
  private val dispatcherProvider: DispatcherProvider,
) : CredentialStorage {

  @OptIn(ExperimentalSettingsApi::class)
  private val settings by lazy {
    tokenSettings.toSuspendSettings(dispatcherProvider.io)
  }

  override suspend fun get(userId: UserId): UserCredentials? {
    val username = settings.getStringOrNull(usernameKey(userId)) ?: return null
    val password = settings.getStringOrNull(passwordKey(userId)) ?: return null
    return UserCredentials(username = username, password = password)
  }

  override suspend fun put(userId: UserId, credentials: UserCredentials) {
    settings.putString(usernameKey(userId), credentials.username)
    settings.putString(passwordKey(userId), credentials.password)
  }

  override suspend fun remove(userId: UserId) {
    settings.remove(usernameKey(userId))
    settings.remove(passwordKey(userId))
  }

  private fun usernameKey(userId: String): String = "cred_username_$userId"
  private fun passwordKey(userId: String): String = "cred_password_$userId"
}
