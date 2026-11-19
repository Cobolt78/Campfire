// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.bookmarks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.Bookmark as BookmarkObj
import app.campfire.analytics.events.Created
import app.campfire.analytics.events.Deleted
import app.campfire.analytics.events.PlaybackActionEvent
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Bookmark
import app.campfire.common.compose.icons.rounded.BookmarkAdd
import app.campfire.common.compose.icons.rounded.BookmarkStar
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.DeleteForever
import app.campfire.common.compose.icons.rounded.DeleteSweep
import app.campfire.common.compose.icons.rounded.Edit
import app.campfire.common.compose.icons.rounded.Forward5
import app.campfire.common.compose.icons.rounded.Replay
import app.campfire.common.compose.icons.rounded.Schedule
import app.campfire.common.compose.widgets.FilledTonalIconButton
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.SizedIcon
import app.campfire.common.compose.widgets.circularReveal
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.onError
import app.campfire.core.coroutines.onLoaded
import app.campfire.core.coroutines.onLoading
import app.campfire.core.di.UserScope
import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import app.campfire.user.api.BookmarkRepository
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_delete_all_bookmarks
import campfire.features.sessions.ui.generated.resources.action_delete_bookmark
import campfire.features.sessions.ui.generated.resources.action_edit_bookmark
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_action_create
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_empty_message
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_error_message
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_title
import campfire.features.sessions.ui.generated.resources.bookmark_default_title
import campfire.features.sessions.ui.generated.resources.bookmark_delete_action_cancel
import campfire.features.sessions.ui.generated.resources.bookmark_delete_action_delete
import campfire.features.sessions.ui.generated.resources.bookmark_delete_all_action_confirm
import campfire.features.sessions.ui.generated.resources.bookmark_delete_all_dialog_message
import campfire.features.sessions.ui.generated.resources.bookmark_delete_all_dialog_title
import campfire.features.sessions.ui.generated.resources.bookmark_delete_title
import campfire.features.sessions.ui.generated.resources.bookmark_dialog_label_notes
import campfire.features.sessions.ui.generated.resources.bookmark_edit_dialog_action_save
import campfire.features.sessions.ui.generated.resources.bookmark_edit_dialog_title
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_action_cancel
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_action_create
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_label_title
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_title
import campfire.features.sessions.ui.generated.resources.cd_forward_time
import campfire.features.sessions.ui.generated.resources.cd_rewind_time
import com.slack.circuit.overlay.OverlayHost
import dev.zacsweers.metro.ContributesTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

sealed interface BookmarkResult {
  data object None : BookmarkResult
  data class Selected(val bookmark: Bookmark) : BookmarkResult
}

@ContributesTo(UserScope::class)
interface BookmarksBottomSheetComponent {
  val audioPlayerHolder: AudioPlayerHolder
  val bookmarkRepository: BookmarkRepository
}

suspend fun OverlayHost.showBookmarksBottomSheet(libraryItemId: LibraryItemId): BookmarkResult {
  return show(
    AdaptiveSheetOverlay<LibraryItemId, BookmarkResult>(
      model = libraryItemId,
      onDismiss = { BookmarkResult.None },
      skipPartiallyExpanded = true,
    ) { id, overlayNavigator ->
      Impression {
        ScreenViewEvent("Bookmarks", ScreenType.Overlay)
      }

      BookmarksBottomSheet(
        libraryItemId = id,
        onBookmarkClick = { bookmark ->
          overlayNavigator.finish(BookmarkResult.Selected(bookmark))
        },
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun BookmarksBottomSheet(
  libraryItemId: LibraryItemId,
  onBookmarkClick: (Bookmark) -> Unit,
  modifier: Modifier = Modifier,
  component: BookmarksBottomSheetComponent = rememberComponent(),
) {
  val scope = rememberCoroutineScope()

  val currentTime by remember {
    component.audioPlayerHolder.currentPlayer.filterNotNull().flatMapLatest { it.overallTime }
  }.collectAsState(0.seconds)

  val loadState by remember {
    component.bookmarkRepository.observeBookmarks(libraryItemId)
      .map { LoadState.Loaded(it) }
      .catch<LoadState<out List<Bookmark>>> { emit(LoadState.Error) }
  }.collectAsState(LoadState.Loading)

  val loadedBookmarks = (loadState as? LoadState.Loaded<List<Bookmark>>)?.data.orEmpty()

  var showCreateDialog by remember { mutableStateOf<Duration?>(null) }
  var editingBookmark by remember { mutableStateOf<Bookmark?>(null) }
  var showDeleteAllDialog by remember { mutableStateOf(false) }

  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.bookmark_bottomsheet_title)) },
    trailingContent = {
      AnimatedVisibility(
        visible = loadedBookmarks.isNotEmpty(),
        modifier = Modifier.align(Alignment.CenterEnd),
      ) {
        val deleteAllLabel = stringResource(Res.string.action_delete_all_bookmarks)
        IconButtonTooltip(text = deleteAllLabel) {
          IconButton(
            onClick = { showDeleteAllDialog = true },
          ) {
            Icon(
              CampfireIcons.Rounded.DeleteSweep,
              contentDescription = deleteAllLabel,
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    },
  ) {
    Box {
      LazyColumn {
        loadState
          .onLoaded { bookmarks ->
            items(
              items = bookmarks,
              key = { it.time.inWholeSeconds },
            ) { bookmark ->
              BookmarkListItem(
                bookmark = bookmark,
                onClick = {
                  onBookmarkClick(bookmark)
                },
                onEditClick = {
                  editingBookmark = bookmark
                },
                onDeleteClick = {
                  scope.launch {
                    Analytics.send(PlaybackActionEvent(BookmarkObj, Deleted))
                    component.bookmarkRepository
                      .removeBookmark(bookmark.libraryItemId, bookmark.time)
                  }
                },
                modifier = Modifier.animateItem(),
              )
            }

            if (bookmarks.isEmpty()) {
              item {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = stringResource(Res.string.bookmark_bottomsheet_empty_message),
                    style = MaterialTheme.typography.bodyLarge,
                  )
                }
              }
            }
          }
          .onLoading {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(300.dp),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator()
              }
            }
          }
          .onError {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 24.dp)
                  .height(200.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = stringResource(Res.string.bookmark_bottomsheet_error_message),
                  textAlign = TextAlign.Center,
                )
              }
            }
          }

        item(key = "create_new_bookmark") {
          CreateNewListItem(
            currentTime = currentTime,
            onClick = {
              showCreateDialog = currentTime
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
      }
    }
  }

  if (showDeleteAllDialog && loadedBookmarks.isNotEmpty()) {
    val bookmarksToDelete = loadedBookmarks.toList()
    AlertDialog(
      onDismissRequest = { showDeleteAllDialog = false },
      icon = {
        Icon(
          CampfireIcons.Rounded.DeleteSweep,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
        )
      },
      title = {
        Text(text = stringResource(Res.string.bookmark_delete_all_dialog_title))
      },
      text = {
        Text(
          text = stringResource(
            Res.string.bookmark_delete_all_dialog_message,
            bookmarksToDelete.size,
          ),
        )
      },
      dismissButton = {
        TextButton(
          onClick = { showDeleteAllDialog = false },
        ) {
          Text(stringResource(Res.string.bookmark_delete_action_cancel))
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteAllDialog = false
            bookmarksToDelete.forEach { bookmark ->
              scope.launch {
                Analytics.send(PlaybackActionEvent(BookmarkObj, Deleted))
                component.bookmarkRepository
                  .removeBookmark(bookmark.libraryItemId, bookmark.time)
              }
            }
          },
          colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
          ),
        ) {
          Text(stringResource(Res.string.bookmark_delete_all_action_confirm))
        }
      },
    )
  }

  val editorTargetTime = showCreateDialog ?: editingBookmark?.time
  if (editorTargetTime != null) {
    val existing = editingBookmark
    val (initialTitle, initialNotes) = remember(existing) {
      if (existing != null) decodeBookmarkTitleAndNotes(existing.title) else "" to ""
    }
    BookmarkEditorDialog(
      currentTime = editorTargetTime,
      isEditing = existing != null,
      initialTitle = initialTitle,
      initialNotes = initialNotes,
      onDismiss = {
        showCreateDialog = null
        editingBookmark = null
      },
      onSave = { encodedTitle, timestamp ->
        scope.launch {
          if (existing != null) {
            if (timestamp.inWholeSeconds != existing.time.inWholeSeconds) {
              component.bookmarkRepository
                .removeBookmark(libraryItemId, existing.time)
            }
          } else {
            Analytics.send(PlaybackActionEvent(BookmarkObj, Created))
          }
          component.bookmarkRepository
            .createBookmark(
              libraryItemId = libraryItemId,
              timestamp = timestamp,
              title = encodedTitle,
            )
        }
        showCreateDialog = null
        editingBookmark = null
      },
    )
  }
}

@Composable
private fun BookmarkListItem(
  bookmark: Bookmark,
  onClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isConfirmDeleteVisible by remember { mutableStateOf(false) }
  Box(
    modifier = modifier,
  ) {
    BookmarkListItemContent(
      bookmark = bookmark,
      onClick = onClick,
      onEditClick = onEditClick,
      onDeleteClick = {
        isConfirmDeleteVisible = true
      },
      modifier = Modifier.zIndex(
        if (isConfirmDeleteVisible) 0f else 1f,
      ),
    )

    Row(
      modifier = Modifier.zIndex(
        if (isConfirmDeleteVisible) 1f else 0f,
      ).circularReveal(
        isVisible = isConfirmDeleteVisible,
        revealFrom = Offset(1f, 0.5f),
      ).background(MaterialTheme.colorScheme.error).matchParentSize().padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(Res.string.bookmark_delete_title),
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.onError,
      )
      TextButton(
        onClick = { isConfirmDeleteVisible = false },
        colors = ButtonDefaults.textButtonColors(
          contentColor = MaterialTheme.colorScheme.onError,
        ),
      ) {
        Text(stringResource(Res.string.bookmark_delete_action_cancel))
      }
      FilledTonalIconButton(
        onClick = {
          onDeleteClick()
          isConfirmDeleteVisible = false
        },
        icon = {
          SizedIcon(
            CampfireIcons.Rounded.DeleteForever,
            contentDescription = null,
          )
        },
        label = { Text(stringResource(Res.string.bookmark_delete_action_delete)) },
      )
    }
  }
}

@Composable
private fun BookmarkListItemContent(
  bookmark: Bookmark,
  onClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val (displayTitle, displayNotes) = remember(bookmark.title) {
    decodeBookmarkTitleAndNotes(bookmark.title)
  }
  ListItem(
    modifier = modifier.combinedClickable(
      onClick = onClick,
      onLongClick = onEditClick,
    ),
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    leadingContent = {
      Icon(
        CampfireIcons.Rounded.Bookmark,
        contentDescription = null,
      )
    },
    headlineContent = { Text(displayTitle) },
    supportingContent = {
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (displayNotes.isNotEmpty()) {
          Text(
            text = displayNotes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            CampfireIcons.Rounded.Schedule,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(4.dp))
          Text(bookmark.time.readoutFormat())
        }
      }
    },
    trailingContent = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val editLabel = stringResource(Res.string.action_edit_bookmark)
        IconButtonTooltip(text = editLabel) {
          IconButton(
            onClick = onEditClick,
          ) {
            Icon(
              CampfireIcons.Rounded.Edit,
              contentDescription = editLabel,
            )
          }
        }
        val deleteLabel = stringResource(Res.string.action_delete_bookmark)
        IconButtonTooltip(text = deleteLabel) {
          IconButton(
            onClick = onDeleteClick,
          ) {
            Icon(
              CampfireIcons.Rounded.Delete,
              contentDescription = deleteLabel,
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    },
  )
}

@Composable
private fun CreateNewListItem(
  currentTime: Duration,
  onClick: (Duration) -> Unit,
  modifier: Modifier = Modifier,
) {
  Button(
    modifier = modifier.fillMaxWidth(),
    onClick = { onClick(currentTime) },
    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
  ) {
    Icon(
      CampfireIcons.Rounded.BookmarkAdd,
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(stringResource(Res.string.bookmark_bottomsheet_action_create, currentTime.readoutFormat()))
  }
}

@Composable
private fun BookmarkEditorDialog(
  currentTime: Duration,
  isEditing: Boolean,
  initialTitle: String,
  initialNotes: String,
  onDismiss: () -> Unit,
  onSave: (encodedTitle: String, timestamp: Duration) -> Unit,
  modifier: Modifier = Modifier,
) {
  var bookmarkTime by remember(currentTime) { mutableStateOf(currentTime) }
  val defaultTitle = stringResource(Res.string.bookmark_default_title, bookmarkTime.readoutFormat())
  var titleValue by remember(initialTitle) { mutableStateOf(TextFieldValue(initialTitle)) }
  var notesValue by remember(initialNotes) { mutableStateOf(TextFieldValue(initialNotes)) }

  BasicAlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    content = {
      Surface(
        modifier = Modifier.wrapContentWidth().wrapContentHeight(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            CampfireIcons.Rounded.BookmarkStar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
          )

          Spacer(Modifier.height(16.dp))

          Text(
            text = stringResource(
              if (isEditing) {
                Res.string.bookmark_edit_dialog_title
              } else {
                Res.string.bookmark_new_dialog_title
              },
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Spacer(Modifier.height(16.dp))

          Row(
            modifier = Modifier
              .wrapContentSize()
              .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(50),
              ),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val rewindLabel = stringResource(Res.string.cd_rewind_time, "1")
            IconButtonTooltip(text = rewindLabel) {
              IconButton(
                onClick = {
                  bookmarkTime = (bookmarkTime - 1.seconds).coerceAtLeast(Duration.ZERO)
                },
                colors = IconButtonDefaults.iconButtonColors(
                  contentColor = MaterialTheme.colorScheme.primary,
                ),
              ) {
                Icon(
                  CampfireIcons.Rounded.Replay,
                  contentDescription = rewindLabel,
                )
              }
            }

            Text(
              text = bookmarkTime.readoutFormat(),
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 8.dp),
            )

            val forwardLabel = stringResource(Res.string.cd_forward_time, "5")
            IconButtonTooltip(text = forwardLabel) {
              IconButton(
                onClick = {
                  bookmarkTime += 5.seconds
                },
                colors = IconButtonDefaults.iconButtonColors(
                  contentColor = MaterialTheme.colorScheme.primary,
                ),
              ) {
                Icon(
                  CampfireIcons.Rounded.Forward5,
                  contentDescription = forwardLabel,
                )
              }
            }
          }

          Spacer(Modifier.height(16.dp))

          OutlinedTextField(
            value = titleValue,
            onValueChange = { newValue ->
              titleValue = newValue
            },
            label = { Text(stringResource(Res.string.bookmark_new_dialog_label_title)) },
            placeholder = { Text(defaultTitle) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          Spacer(Modifier.height(12.dp))

          OutlinedTextField(
            value = notesValue,
            onValueChange = { newValue ->
              notesValue = newValue
            },
            label = { Text(stringResource(Res.string.bookmark_dialog_label_notes)) },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
          )

          Spacer(Modifier.height(16.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            modifier = Modifier.fillMaxWidth(),
          ) {
            TextButton(
              onClick = onDismiss,
            ) {
              Text(stringResource(Res.string.bookmark_new_dialog_action_cancel))
            }
            TextButton(
              onClick = {
                onSave(
                  encodeBookmarkTitleAndNotes(
                    titleInput = titleValue.text,
                    notesInput = notesValue.text,
                    defaultTitle = defaultTitle,
                  ),
                  bookmarkTime,
                )
              },
            ) {
              Text(
                stringResource(
                  if (isEditing) {
                    Res.string.bookmark_edit_dialog_action_save
                  } else {
                    Res.string.bookmark_new_dialog_action_create
                  },
                ),
              )
            }
          }
        }
      }
    },
  )
}

internal fun decodeBookmarkTitleAndNotes(rawTitle: String): Pair<String, String> {
  val normalized = rawTitle.replace("\r\n", "\n")
  val newlineIndex = normalized.indexOf('\n')
  return if (newlineIndex == -1) {
    normalized to ""
  } else {
    normalized.substring(0, newlineIndex) to normalized.substring(newlineIndex + 1)
  }
}

internal fun encodeBookmarkTitleAndNotes(
  titleInput: String,
  notesInput: String,
  defaultTitle: String,
): String {
  val cleanTitle = titleInput.replace('\n', ' ').replace('\r', ' ').trim()
    .ifBlank { defaultTitle }
  val cleanNotes = notesInput.replace("\r\n", "\n").trim()
  return if (cleanNotes.isEmpty()) {
    cleanTitle
  } else {
    "$cleanTitle\n$cleanNotes"
  }
}
