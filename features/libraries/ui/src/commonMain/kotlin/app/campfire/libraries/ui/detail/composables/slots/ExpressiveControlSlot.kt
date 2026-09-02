// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.common.compose.network.rememberIsCellularOrMetered
import app.campfire.common.compose.permission.PermissionState
import app.campfire.common.compose.permission.rememberPostNotificationPermissionState
import app.campfire.common.compose.widgets.dialog.ConfirmActionDialog
import app.campfire.common.compose.widgets.dialog.ConfirmDownloadDialog
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.ExpressiveControlBar
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.playlists.api.dialog.PlaylistDialogResult
import kotlinx.coroutines.launch

class ExpressiveControlSlot(
  private val libraryItem: LibraryItem,
  private val offlineDownload: OfflineDownload?,
  private val mediaProgress: MediaProgress?,
  private val isQueued: Boolean,
  private val hasSession: Boolean,
  private val isCurrentSession: Boolean,
  private val addToPlaylistDialog: AddToPlaylistDialog,
  @get:VisibleForTesting val showConfirmDownloadDialogSetting: Boolean,
  val confirmActionsSetting: Boolean = true,
  val warnOnCellularDownloadSetting: Boolean = true,
) : ContentSlot {

  override val id: String = "expressive_control_bar"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    var showConfirmDownloadDialog by remember { mutableStateOf(false) }
    var showCellularDownloadConfirmation by remember { mutableStateOf(false) }
    var showDeleteDownloadConfirmation by remember { mutableStateOf(false) }
    var showDiscardProgressConfirmation by remember { mutableStateOf(false) }
    var showMarkFinishedConfirmation by remember { mutableStateOf(false) }
    var doNotShowDownloadConfirmationAgain by remember { mutableStateOf(false) }

    val isCellular = rememberIsCellularOrMetered()

    val postNotificationPermissionState = rememberPostNotificationPermissionState {
      if (it) {
        eventSink(LibraryItemUiEvent.DownloadClick(doNotShowDownloadConfirmationAgain))
        showConfirmDownloadDialog = false
      }
    }

    val scope = rememberCoroutineScope()
    val snackBarHost = LocalSnackBarHost.current
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    if (showAddToPlaylistDialog) {
      addToPlaylistDialog.Content(
        libraryItemId = libraryItem.id,
        itemTitle = libraryItem.media.metadata.title.orEmpty(),
        onDismiss = { dialogResult ->
          if (dialogResult !is PlaylistDialogResult.None) {
            scope.launch {
              val result = snackBarHost.showSnackbar(
                message = "Added to playlist",
                actionLabel = "Open",
              )

              if (result == SnackbarResult.ActionPerformed) {
                val playlistId = when (dialogResult) {
                  is PlaylistDialogResult.Existing -> dialogResult.playlistId
                  is PlaylistDialogResult.New -> dialogResult.playlistId
                }
                eventSink(LibraryItemUiEvent.OpenPlaylist(playlistId, dialogResult is PlaylistDialogResult.New))
              }
            }
          }
          showAddToPlaylistDialog = false
        },
        modifier = Modifier,
      )
    }

    val hasProgress = mediaProgress != null &&
      mediaProgress.progress > 0f &&
      !mediaProgress.isFinished

    ExpressiveControlBar(
      isQueued = isQueued,
      hasSession = hasSession,
      isCurrentSession = isCurrentSession,
      isEbookOnly = libraryItem.isEbookOnly,
      offlineDownload = offlineDownload,
      totalSizeInBytes = libraryItem.media.sizeInBytes,
      mediaProgress = mediaProgress,
      onPlayClick = {
        eventSink(LibraryItemUiEvent.PlayClick)
      },
      onDownloadClick = {
        if (warnOnCellularDownloadSetting && isCellular) {
          showCellularDownloadConfirmation = true
        } else if (showConfirmDownloadDialogSetting) {
          showConfirmDownloadDialog = true
        } else {
          eventSink(LibraryItemUiEvent.DownloadClick())
        }
      },
      onMarkFinished = {
        if (confirmActionsSetting && hasProgress) {
          showMarkFinishedConfirmation = true
        } else {
          eventSink(LibraryItemUiEvent.MarkFinished(libraryItem))
        }
      },
      onMarkNotFinished = {
        eventSink(LibraryItemUiEvent.MarkNotFinished(libraryItem))
      },
      onDiscardProgress = {
        if (confirmActionsSetting) {
          showDiscardProgressConfirmation = true
        } else {
          eventSink(LibraryItemUiEvent.DiscardProgress(libraryItem))
        }
      },
      onStopDownloadClick = {
        eventSink(LibraryItemUiEvent.StopDownloadClick)
      },
      onDeleteDownloadClick = {
        if (confirmActionsSetting) {
          showDeleteDownloadConfirmation = true
        } else {
          eventSink(LibraryItemUiEvent.RemoveDownloadClick)
        }
      },
      onAddToQueueClick = {
        if (isQueued) {
          Analytics.send(ActionEvent("remove_from_queue", Click))
          eventSink(LibraryItemUiEvent.RemoveFromQueue)
        } else {
          Analytics.send(ActionEvent("add_to_queue", Click))
          eventSink(LibraryItemUiEvent.AddToQueue)
        }
      },
      onAddToPlaylistClick = {
        showAddToPlaylistDialog = true
      },
      modifier = modifier.padding(horizontal = 16.dp),
    )

    if (showCellularDownloadConfirmation) {
      val itemTitle = libraryItem.media.metadata.title.orEmpty()
      val sizeText = libraryItem.media.sizeInBytes.takeIf { it > 0 }?.asReadableBytes() ?: ""
      val message = if (sizeText.isNotEmpty()) {
        "You are connected to mobile data. Downloading \"$itemTitle\" ($sizeText) will use your cellular data plan."
      } else {
        "You are connected to mobile data. Downloading \"$itemTitle\" will use your cellular data plan."
      }
      ConfirmActionDialog(
        title = "Download Over Cellular?",
        message = message,
        confirmButtonText = "Download",
        onConfirm = {
          showCellularDownloadConfirmation = false
          if (showConfirmDownloadDialogSetting) {
            showConfirmDownloadDialog = true
          } else {
            eventSink(LibraryItemUiEvent.DownloadClick())
          }
        },
        onDismissRequest = { showCellularDownloadConfirmation = false },
      )
    }

    if (showDeleteDownloadConfirmation) {
      val itemTitle = libraryItem.media.metadata.title.orEmpty()
      val sizeText = libraryItem.media.sizeInBytes.takeIf { it > 0 }?.asReadableBytes() ?: ""
      val message = if (sizeText.isNotEmpty()) {
        "Are you sure you want to delete the local download of \"$itemTitle\"? This will free up $sizeText on your device."
      } else {
        "Are you sure you want to delete the local download of \"$itemTitle\"?"
      }
      ConfirmActionDialog(
        title = "Delete Download?",
        message = message,
        confirmButtonText = "Delete",
        onConfirm = {
          eventSink(LibraryItemUiEvent.RemoveDownloadClick)
          showDeleteDownloadConfirmation = false
        },
        onDismissRequest = { showDeleteDownloadConfirmation = false },
      )
    }

    if (showDiscardProgressConfirmation) {
      val itemTitle = libraryItem.media.metadata.title.orEmpty()
      ConfirmActionDialog(
        title = "Discard Progress?",
        message = "Are you sure you want to reset your listening progress for \"$itemTitle\"? Your saved position will be lost.",
        confirmButtonText = "Discard",
        onConfirm = {
          eventSink(LibraryItemUiEvent.DiscardProgress(libraryItem))
          showDiscardProgressConfirmation = false
        },
        onDismissRequest = { showDiscardProgressConfirmation = false },
      )
    }

    if (showMarkFinishedConfirmation) {
      val itemTitle = libraryItem.media.metadata.title.orEmpty()
      ConfirmActionDialog(
        title = "Mark as Finished?",
        message = "Are you sure you want to mark \"$itemTitle\" as finished? This will set your progress to 100%.",
        confirmButtonText = "Mark Finished",
        onConfirm = {
          eventSink(LibraryItemUiEvent.MarkFinished(libraryItem))
          showMarkFinishedConfirmation = false
        },
        onDismissRequest = { showMarkFinishedConfirmation = false },
      )
    }

    if (showConfirmDownloadDialog) {
      ConfirmDownloadDialog(
        item = libraryItem,
        onConfirm = { doNotShowAgain ->
          if (postNotificationPermissionState is PermissionState.Granted) {
            eventSink(LibraryItemUiEvent.DownloadClick(doNotShowAgain))
            showConfirmDownloadDialog = false
          } else {
            doNotShowDownloadConfirmationAgain = doNotShowAgain
            postNotificationPermissionState.launchPermissionRequest()
          }
        },
        onDismissRequest = { showConfirmDownloadDialog = false },
      )
    }
  }
}
