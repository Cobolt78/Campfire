// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.detail.composables

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
import app.campfire.core.model.Playlist
import campfire.features.playlists.ui.generated.resources.Res
import campfire.features.playlists.ui.generated.resources.dialog_action_cancel
import campfire.features.playlists.ui.generated.resources.dialog_action_download
import campfire.features.playlists.ui.generated.resources.dialog_action_ok
import campfire.features.playlists.ui.generated.resources.dialog_download_playlist_all_downloaded_message
import campfire.features.playlists.ui.generated.resources.dialog_download_playlist_all_downloaded_title
import campfire.features.playlists.ui.generated.resources.dialog_download_playlist_all_finished_prefix
import campfire.features.playlists.ui.generated.resources.dialog_download_playlist_completed_items_count
import campfire.features.playlists.ui.generated.resources.dialog_download_playlist_items_count
import campfire.features.playlists.ui.generated.resources.dialog_download_playlist_title
import campfire.features.playlists.ui.generated.resources.dialog_download_playlist_unlistened_items_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

internal val Playlist.Item.Expanded.sizeInBytes: Long
  get() = episode?.sizeInBytes?.takeIf { it > 0L } ?: libraryItem.media.sizeInBytes

@Composable
internal fun ConfirmDownloadPlaylistDialog(
  unlistenedItems: List<Playlist.Item.Expanded>,
  completedItems: List<Playlist.Item.Expanded>,
  onConfirm: (List<Playlist.Item.Expanded>) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val allAlreadyDownloaded = unlistenedItems.isEmpty() && completedItems.isEmpty()

  if (allAlreadyDownloaded) {
    AlertDialog(
      modifier = modifier,
      onDismissRequest = onDismissRequest,
      title = { Text(stringResource(Res.string.dialog_download_playlist_all_downloaded_title)) },
      text = { Text(stringResource(Res.string.dialog_download_playlist_all_downloaded_message)) },
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
    targetItems.sumOf { it.sizeInBytes }
  }

  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(Res.string.dialog_download_playlist_title)) },
    text = {
      Column {
        if (unlistenedItems.isNotEmpty()) {
          val countText = if (includeCompleted) {
            pluralStringResource(
              Res.plurals.dialog_download_playlist_items_count,
              targetItems.size,
              targetItems.size,
            )
          } else {
            pluralStringResource(
              Res.plurals.dialog_download_playlist_unlistened_items_count,
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
                Res.plurals.dialog_download_playlist_completed_items_count,
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
          val completedText = pluralStringResource(
            Res.plurals.dialog_download_playlist_completed_items_count,
            completedItems.size,
            completedItems.size,
          )
          Text(
            buildAnnotatedString {
              append(stringResource(Res.string.dialog_download_playlist_all_finished_prefix))
              append(" ")
              withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(completedText)
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
        Text(
          "${stringResource(Res.string.dialog_action_download)} (${targetItems.size})",
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(Res.string.dialog_action_cancel))
      }
    },
  )
}
