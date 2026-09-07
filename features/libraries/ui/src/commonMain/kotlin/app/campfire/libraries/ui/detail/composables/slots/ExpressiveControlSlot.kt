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
import app.campfire.common.compose.permission.PermissionState
import app.campfire.common.compose.permission.rememberPostNotificationPermissionState
import app.campfire.common.compose.widgets.dialog.ConfirmDownloadDialog
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
  @get:VisibleForTesting val confirmActionsSetting: Boolean = true,
  @get:VisibleForTesting val warnOnCellularDownloadSetting: Boolean = true,
  @get:VisibleForTesting val canStreamHls: Boolean = false,
  @get:VisibleForTesting val willStreamHls: Boolean = false,
) : ContentSlot {

  override val id: String = "expressive_control_bar"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    var showConfirmDownloadDialog by remember { mutableStateOf(false) }
    var doNotShowDownloadConfirmationAgain by remember { mutableStateOf(false) }
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

    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var showConfirmDiscardDialog by remember { mutableStateOf(false) }
    var showConfirmMarkFinishedDialog by remember { mutableStateOf(false) }
    var showCellularWarningDialog by remember { mutableStateOf(false) }

    val isCellularOrMetered = app.campfire.common.compose.network.rememberIsCellularOrMetered()

    ExpressiveControlBar(
      isQueued = isQueued,
      hasSession = hasSession,
      isCurrentSession = isCurrentSession,
      isEbookOnly = libraryItem.isEbookOnly,
      canStreamHls = canStreamHls,
      willStreamHls = willStreamHls,
      offlineDownload = offlineDownload,
      totalSizeInBytes = libraryItem.media.sizeInBytes,
      mediaProgress = mediaProgress,
      onPlayClick = { method ->
        eventSink(LibraryItemUiEvent.PlayClick(method))
      },
      onDownloadClick = {
        if (warnOnCellularDownloadSetting && isCellularOrMetered) {
          showCellularWarningDialog = true
        } else if (showConfirmDownloadDialogSetting) {
          showConfirmDownloadDialog = true
        } else {
          eventSink(LibraryItemUiEvent.DownloadClick())
        }
      },
      onMarkFinished = {
        if (confirmActionsSetting && mediaProgress != null && mediaProgress.progress > 0f) {
          showConfirmMarkFinishedDialog = true
        } else {
          eventSink(LibraryItemUiEvent.MarkFinished(libraryItem))
        }
      },
      onMarkNotFinished = {
        eventSink(LibraryItemUiEvent.MarkNotFinished(libraryItem))
      },
      onDiscardProgress = {
        if (confirmActionsSetting) {
          showConfirmDiscardDialog = true
        } else {
          eventSink(LibraryItemUiEvent.DiscardProgress(libraryItem))
        }
      },
      onStopDownloadClick = {
        eventSink(LibraryItemUiEvent.StopDownloadClick)
      },
      onDeleteDownloadClick = {
        if (confirmActionsSetting) {
          showConfirmDeleteDialog = true
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

    if (showCellularWarningDialog) {
      app.campfire.common.compose.widgets.dialog.ConfirmActionDialog(
        title = "Mobile Data Warning",
        message = "You are currently on a mobile or metered connection. Downloading may result in extra data charges.",
        confirmButtonText = "Download anyway",
        onConfirm = {
          showCellularWarningDialog = false
          if (showConfirmDownloadDialogSetting) {
            showConfirmDownloadDialog = true
          } else {
            eventSink(LibraryItemUiEvent.DownloadClick())
          }
        },
        onDismissRequest = { showCellularWarningDialog = false },
      )
    }

    if (showConfirmDeleteDialog) {
      app.campfire.common.compose.widgets.dialog.ConfirmActionDialog(
        title = "Delete Download",
        message = "Are you sure you want to delete this download from your device?",
        confirmButtonText = "Delete",
        onConfirm = {
          showConfirmDeleteDialog = false
          eventSink(LibraryItemUiEvent.RemoveDownloadClick)
        },
        onDismissRequest = { showConfirmDeleteDialog = false },
      )
    }

    if (showConfirmDiscardDialog) {
      app.campfire.common.compose.widgets.dialog.ConfirmActionDialog(
        title = "Discard Progress",
        message = "Are you sure you want to discard your listening progress for this item?",
        confirmButtonText = "Discard",
        onConfirm = {
          showConfirmDiscardDialog = false
          eventSink(LibraryItemUiEvent.DiscardProgress(libraryItem))
        },
        onDismissRequest = { showConfirmDiscardDialog = false },
      )
    }

    if (showConfirmMarkFinishedDialog) {
      app.campfire.common.compose.widgets.dialog.ConfirmActionDialog(
        title = "Mark as Finished",
        message = "You have not completed listening to this item yet. Are you sure you want to mark it as finished?",
        confirmButtonText = "Mark finished",
        onConfirm = {
          showConfirmMarkFinishedDialog = false
          eventSink(LibraryItemUiEvent.MarkFinished(libraryItem))
        },
        onDismissRequest = { showConfirmMarkFinishedDialog = false },
      )
    }
  }
}
