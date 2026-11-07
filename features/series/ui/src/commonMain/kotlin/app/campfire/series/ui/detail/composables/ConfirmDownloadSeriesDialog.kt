// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.LibraryItem
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.dialog_action_cancel
import campfire.features.series.ui.generated.resources.dialog_action_download
import campfire.features.series.ui.generated.resources.dialog_action_ok
import campfire.features.series.ui.generated.resources.dialog_download_series_all_downloaded_message
import campfire.features.series.ui.generated.resources.dialog_download_series_all_downloaded_title
import campfire.features.series.ui.generated.resources.dialog_download_series_all_finished_prefix
import campfire.features.series.ui.generated.resources.dialog_download_series_books_count
import campfire.features.series.ui.generated.resources.dialog_download_series_completed_books_count
import campfire.features.series.ui.generated.resources.dialog_download_series_title
import campfire.features.series.ui.generated.resources.dialog_download_series_unlistened_books_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConfirmDownloadSeriesDialog(
  unlistenedItems: List<LibraryItem>,
  completedItems: List<LibraryItem>,
  onConfirm: (List<LibraryItem>) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val allAlreadyDownloaded = unlistenedItems.isEmpty() && completedItems.isEmpty()

  if (allAlreadyDownloaded) {
    AlertDialog(
      modifier = modifier,
      onDismissRequest = onDismissRequest,
      title = { Text(stringResource(Res.string.dialog_download_series_all_downloaded_title)) },
      text = { Text(stringResource(Res.string.dialog_download_series_all_downloaded_message)) },
      confirmButton = {
        TextButton(onClick = onDismissRequest) {
          Text(stringResource(Res.string.dialog_action_ok))
        }
      },
    )
    return
  }

  var includeCompleted by remember { mutableStateOf(false) }

  val targetItems = remember(includeCompleted, unlistenedItems, completedItems) {
    if (unlistenedItems.isEmpty()) {
      completedItems
    } else if (includeCompleted) {
      unlistenedItems + completedItems
    } else {
      unlistenedItems
    }
  }

  val totalBytes = remember(targetItems) {
    targetItems.sumOf { it.media.sizeInBytes }
  }

  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(Res.string.dialog_download_series_title)) },
    text = {
      Column {
        if (unlistenedItems.isNotEmpty()) {
          val countText = if (includeCompleted) {
            pluralStringResource(
              Res.plurals.dialog_download_series_books_count,
              targetItems.size,
              targetItems.size,
            )
          } else {
            pluralStringResource(
              Res.plurals.dialog_download_series_unlistened_books_count,
              unlistenedItems.size,
              unlistenedItems.size,
            )
          }
          Text(
            buildAnnotatedString {
              append("Download ")
              withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(countText)
              }
              append(" (~${totalBytes.asReadableBytes()})?")
            },
          )

          if (completedItems.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { includeCompleted = !includeCompleted }
                .padding(vertical = 4.dp),
            ) {
              val completedLabel = pluralStringResource(
                Res.plurals.dialog_download_series_completed_books_count,
                completedItems.size,
                completedItems.size,
              )
              Text(
                text = "Include $completedLabel",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
              )
              Checkbox(
                checked = includeCompleted,
                onCheckedChange = { includeCompleted = it },
              )
            }
          }
        } else {
          val countText = pluralStringResource(
            Res.plurals.dialog_download_series_books_count,
            completedItems.size,
            completedItems.size,
          )
          Text(
            buildAnnotatedString {
              append(stringResource(Res.string.dialog_download_series_all_finished_prefix))
              withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(countText)
              }
              append(" (~${totalBytes.asReadableBytes()})?")
            },
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirm(targetItems) },
      ) {
        Text("${stringResource(Res.string.dialog_action_download)} (${targetItems.size})")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(Res.string.dialog_action_cancel))
      }
    },
  )
}
