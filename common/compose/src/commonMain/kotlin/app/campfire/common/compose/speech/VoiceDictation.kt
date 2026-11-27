// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.speech

import androidx.compose.runtime.Composable

/**
 * Platform launcher for speech-to-text dictation (Android system speech recognizer on Android,
 * unsupported no-op on other targets).
 */
interface VoiceDictationLauncher {
  val isSupported: Boolean
  fun launch(prompt: String)
}

@Composable
expect fun rememberVoiceDictationLauncher(
  onResult: (String) -> Unit,
): VoiceDictationLauncher

/**
 * Merges a newly dictated phrase into [existing] text, capitalizing the first character of the
 * dictated phrase when starting fresh or after sentence-ending punctuation / a newline.
 */
fun appendDictatedText(
  existing: String,
  dictated: String,
): String {
  val cleaned = dictated.trim()
  if (cleaned.isEmpty()) return existing

  val trimmedEnd = existing.trimEnd()
  val shouldCapitalize = trimmedEnd.isEmpty() ||
    existing.endsWith("\n") ||
    trimmedEnd.last() in charArrayOf('.', '!', '?')
  val formatted = if (shouldCapitalize) {
    cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
  } else {
    cleaned
  }

  return when {
    trimmedEnd.isEmpty() -> formatted
    existing.endsWith("\n") -> "$existing$formatted"
    else -> "$trimmedEnd $formatted"
  }
}
