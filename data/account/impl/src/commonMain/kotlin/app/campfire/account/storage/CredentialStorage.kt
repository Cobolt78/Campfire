// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.storage

import app.campfire.account.api.UserCredentials
import app.campfire.core.model.UserId

interface CredentialStorage {
  suspend fun get(userId: UserId): UserCredentials?
  suspend fun put(userId: UserId, credentials: UserCredentials)
  suspend fun remove(userId: UserId)
}
