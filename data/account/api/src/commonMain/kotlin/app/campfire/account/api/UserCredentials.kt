// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.api

/**
 * Credentials securely stored in hardware-backed storage for silent background auto-healing
 * when access and refresh tokens expire or are revoked.
 */
data class UserCredentials(
  val username: String,
  val password: String,
)
