// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.speech

import androidx.compose.runtime.Composable

private object UnsupportedVoiceDictationLauncher : VoiceDictationLauncher {
  override val isSupported: Boolean = false
  override fun launch(prompt: String) = Unit
}

@Composable
actual fun rememberVoiceDictationLauncher(
  onResult: (String) -> Unit,
): VoiceDictationLauncher = UnsupportedVoiceDictationLauncher
