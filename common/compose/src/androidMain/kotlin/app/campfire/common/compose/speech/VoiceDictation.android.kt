// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.speech

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberVoiceDictationLauncher(
  onResult: (String) -> Unit,
): VoiceDictationLauncher {
  val context = LocalContext.current
  val currentOnResult by rememberUpdatedState(onResult)

  val isSupported = remember(context) {
    val probe = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    probe.resolveActivity(context.packageManager) != null
  }

  val activityLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val spoken = result.data
        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        ?.firstOrNull()
        ?.trim()
      if (!spoken.isNullOrEmpty()) {
        currentOnResult(spoken)
      }
    }
  }

  return remember(isSupported, activityLauncher) {
    object : VoiceDictationLauncher {
      override val isSupported: Boolean = isSupported

      override fun launch(prompt: String) {
        if (!isSupported) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        try {
          activityLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
          // Ignore if no speech recognizer activity can handle the request at launch time
        }
      }
    }
  }
}
