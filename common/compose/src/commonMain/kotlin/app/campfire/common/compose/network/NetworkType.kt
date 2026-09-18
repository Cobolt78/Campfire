// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.network

import androidx.compose.runtime.Composable

/**
 * Returns true if the device is currently connected via cellular or a metered connection.
 * Updates reactively when connectivity changes while the composable is in the composition.
 */
@Composable
expect fun rememberIsCellularOrMetered(): Boolean
