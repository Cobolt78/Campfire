// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.dialog_download_action_dismiss
import org.jetbrains.compose.resources.stringResource

/**
 * A simple, reusable confirmation dialog for potentially destructive actions.
 * The cancel button label comes from string resources so it is localisable.
 */
@Composable
fun ConfirmActionDialog(
  title: String,
  message: String,
  confirmButtonText: String,
  onConfirm: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  dismissButtonText: String = stringResource(Res.string.dialog_download_action_dismiss),
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
      )
    },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(confirmButtonText)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(dismissButtonText)
      }
    },
  )
}
