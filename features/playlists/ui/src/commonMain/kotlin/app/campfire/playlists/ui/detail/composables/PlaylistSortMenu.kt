// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.detail.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.SortAsc
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.model.Playlist
import campfire.features.playlists.ui.generated.resources.Res
import campfire.features.playlists.ui.generated.resources.action_sort_playlist
import campfire.features.playlists.ui.generated.resources.dialog_action_cancel
import campfire.features.playlists.ui.generated.resources.dialog_action_sort
import campfire.features.playlists.ui.generated.resources.dialog_sort_playlist_message
import campfire.features.playlists.ui.generated.resources.dialog_sort_playlist_title
import org.jetbrains.compose.resources.stringResource

enum class PlaylistSortOption(val label: String) {
  TitleAsc("Title (A–Z)"),
  TitleDesc("Title (Z–A)"),
  AuthorAsc("Author (A–Z)"),
  AuthorDesc("Author (Z–A)"),
  DurationAsc("Length (Shortest first)"),
  DurationDesc("Length (Longest first)"),
  DateAddedDesc("Date Added (Newest first)"),
  DateAddedAsc("Date Added (Oldest first)"),
  PublishYearDesc("Release Date (Newest first)"),
  PublishYearAsc("Release Date (Oldest first)"),
  ;

  val comparator: Comparator<Playlist.Item.Expanded>
    get() = when (this) {
      TitleAsc -> Comparator { a, b ->
        val titleA = a.episode?.title ?: a.libraryItem.media.metadata.title.orEmpty()
        val titleB = b.episode?.title ?: b.libraryItem.media.metadata.title.orEmpty()
        titleA.compareTo(titleB, ignoreCase = true)
      }
      TitleDesc -> Comparator { a, b ->
        val titleA = a.episode?.title ?: a.libraryItem.media.metadata.title.orEmpty()
        val titleB = b.episode?.title ?: b.libraryItem.media.metadata.title.orEmpty()
        titleB.compareTo(titleA, ignoreCase = true)
      }
      AuthorAsc -> Comparator { a, b ->
        val authorA = a.libraryItem.media.metadata.authorName.orEmpty()
        val authorB = b.libraryItem.media.metadata.authorName.orEmpty()
        val cmp = authorA.compareTo(authorB, ignoreCase = true)
        if (cmp != 0) cmp else {
          val titleA = a.episode?.title ?: a.libraryItem.media.metadata.title.orEmpty()
          val titleB = b.episode?.title ?: b.libraryItem.media.metadata.title.orEmpty()
          titleA.compareTo(titleB, ignoreCase = true)
        }
      }
      AuthorDesc -> Comparator { a, b ->
        val authorA = a.libraryItem.media.metadata.authorName.orEmpty()
        val authorB = b.libraryItem.media.metadata.authorName.orEmpty()
        val cmp = authorB.compareTo(authorA, ignoreCase = true)
        if (cmp != 0) cmp else {
          val titleA = a.episode?.title ?: a.libraryItem.media.metadata.title.orEmpty()
          val titleB = b.episode?.title ?: b.libraryItem.media.metadata.title.orEmpty()
          titleA.compareTo(titleB, ignoreCase = true)
        }
      }
      DurationAsc -> Comparator { a, b ->
        val durA = a.episode?.durationInMillis ?: a.libraryItem.media.durationInMillis
        val durB = b.episode?.durationInMillis ?: b.libraryItem.media.durationInMillis
        durA.compareTo(durB)
      }
      DurationDesc -> Comparator { a, b ->
        val durA = a.episode?.durationInMillis ?: a.libraryItem.media.durationInMillis
        val durB = b.episode?.durationInMillis ?: b.libraryItem.media.durationInMillis
        durB.compareTo(durA)
      }
      DateAddedDesc -> Comparator { a, b ->
        val addA = a.libraryItem.addedAtMillis
        val addB = b.libraryItem.addedAtMillis
        addB.compareTo(addA)
      }
      DateAddedAsc -> Comparator { a, b ->
        val addA = a.libraryItem.addedAtMillis
        val addB = b.libraryItem.addedAtMillis
        addA.compareTo(addB)
      }
      PublishYearDesc -> Comparator { a, b ->
        val yearA = a.libraryItem.media.metadata.publishedYear?.toIntOrNull() ?: 0
        val yearB = b.libraryItem.media.metadata.publishedYear?.toIntOrNull() ?: 0
        yearB.compareTo(yearA)
      }
      PublishYearAsc -> Comparator { a, b ->
        val yearA = a.libraryItem.media.metadata.publishedYear?.toIntOrNull() ?: 0
        val yearB = b.libraryItem.media.metadata.publishedYear?.toIntOrNull() ?: 0
        yearA.compareTo(yearB)
      }
    }
}

@Composable
internal fun PlaylistSortMenu(
  onSortSelected: (PlaylistSortOption) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier) {
    val sortLabel = stringResource(Res.string.action_sort_playlist)
    IconButtonTooltip(text = sortLabel) {
      IconButton(onClick = { expanded = true }) {
        Icon(
          CampfireIcons.Rounded.SortAsc,
          contentDescription = sortLabel,
        )
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      shape = MaterialTheme.shapes.medium,
    ) {
      PlaylistSortOption.entries.forEach { option ->
        DropdownMenuItem(
          text = { Text(option.label) },
          onClick = {
            expanded = false
            onSortSelected(option)
          },
        )
      }
    }
  }
}

@Composable
internal fun ConfirmSortPlaylistDialog(
  sortOption: PlaylistSortOption,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.dialog_sort_playlist_title)) },
    text = {
      Text(stringResource(Res.string.dialog_sort_playlist_message, sortOption.label))
    },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(Res.string.dialog_action_sort))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.dialog_action_cancel))
      }
    },
  )
}
