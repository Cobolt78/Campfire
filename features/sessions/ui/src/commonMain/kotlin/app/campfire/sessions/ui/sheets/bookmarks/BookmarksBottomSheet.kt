// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.bookmarks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.Bookmark as BookmarkObj
import app.campfire.analytics.events.Created
import app.campfire.analytics.events.Deleted
import app.campfire.analytics.events.PlaybackActionEvent
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Bookmark
import app.campfire.common.compose.icons.rounded.BookmarkAdd
import app.campfire.common.compose.icons.rounded.BookmarkStar
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.DeleteForever
import app.campfire.common.compose.icons.rounded.DeleteSweep
import app.campfire.common.compose.icons.rounded.Edit
import app.campfire.common.compose.icons.rounded.Forward30
import app.campfire.common.compose.icons.rounded.History
import app.campfire.common.compose.icons.rounded.Mic
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.Replay30
import app.campfire.common.compose.icons.rounded.Schedule
import app.campfire.common.compose.speech.appendDictatedText
import app.campfire.common.compose.speech.rememberVoiceDictationLauncher
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
import app.campfire.settings.api.PlaybackSettings
import app.campfire.user.api.BookmarkRepository
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_delete_all_bookmarks
import campfire.features.sessions.ui.generated.resources.action_delete_bookmark
import campfire.features.sessions.ui.generated.resources.action_dictate_bookmark_notes
import campfire.features.sessions.ui.generated.resources.action_dictate_bookmark_title
import campfire.features.sessions.ui.generated.resources.action_edit_bookmark
import campfire.features.sessions.ui.generated.resources.action_play_pause
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_action_create
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_empty_message
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_error_message
import campfire.features.sessions.ui.generated.resources.bookmark_bottomsheet_title
import campfire.features.sessions.ui.generated.resources.bookmark_continue_listening_at
import campfire.features.sessions.ui.generated.resources.bookmark_default_title
import campfire.features.sessions.ui.generated.resources.bookmark_delete_action_cancel
import campfire.features.sessions.ui.generated.resources.bookmark_delete_action_delete
import campfire.features.sessions.ui.generated.resources.bookmark_delete_all_action_confirm
import campfire.features.sessions.ui.generated.resources.bookmark_delete_all_dialog_message
import campfire.features.sessions.ui.generated.resources.bookmark_delete_all_dialog_title
import campfire.features.sessions.ui.generated.resources.bookmark_delete_title
import campfire.features.sessions.ui.generated.resources.bookmark_dialog_label_notes
import campfire.features.sessions.ui.generated.resources.bookmark_dictation_prompt_notes
import campfire.features.sessions.ui.generated.resources.bookmark_dictation_prompt_title
import campfire.features.sessions.ui.generated.resources.bookmark_edit_dialog_action_save
import campfire.features.sessions.ui.generated.resources.bookmark_edit_dialog_title
import campfire.features.sessions.ui.generated.resources.bookmark_editor_timestamp_label
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_action_cancel
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_action_create
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_label_title
import campfire.features.sessions.ui.generated.resources.bookmark_new_dialog_title
import campfire.features.sessions.ui.generated.resources.bookmark_notes_show_less
import campfire.features.sessions.ui.generated.resources.bookmark_notes_show_more
import campfire.features.sessions.ui.generated.resources.bookmark_return_banner_dismiss
import campfire.features.sessions.ui.generated.resources.cd_forward_time
import campfire.features.sessions.ui.generated.resources.cd_rewind_time
import com.slack.circuit.overlay.OverlayHost
import dev.zacsweers.metro.ContributesTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

sealed interface BookmarkResult {
  data object None : BookmarkResult
  data class Selected(val bookmark: Bookmark) : BookmarkResult
}

@ContributesTo(UserScope::class)
interface BookmarksBottomSheetComponent {
  val audioPlayerHolder: AudioPlayerHolder
  val bookmarkRepository: BookmarkRepository
  val playbackSettings: PlaybackSettings
  val bookmarkReturnPositionStore: BookmarkReturnPositionStore
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
        onDismissSheet = {
          overlayNavigator.finish(BookmarkResult.None)
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
  onDismissSheet: () -> Unit,
  modifier: Modifier = Modifier,
  component: BookmarksBottomSheetComponent = rememberComponent(),
) {
  val scope = rememberCoroutineScope()

  val currentPlayer by component.audioPlayerHolder.currentPlayer.collectAsState()

  val currentTime by remember {
    component.audioPlayerHolder.currentPlayer.filterNotNull().flatMapLatest { it.overallTime }
  }.collectAsState(0.seconds)

  val playerState by remember {
    component.audioPlayerHolder.currentPlayer.filterNotNull().flatMapLatest { it.state }
  }.collectAsState(AudioPlayer.State.Disabled)

  val returnPosition by remember(libraryItemId) {
    component.bookmarkReturnPositionStore.observe(libraryItemId)
  }.collectAsState(null)

  val useCompactLayout by remember {
    component.playbackSettings.observeBookmarkEditorAlternateView()
  }.collectAsState()

  val loadState by remember(libraryItemId) {
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
    Column(
      modifier = Modifier.fillMaxWidth(),
    ) {
      val visibleReturnPosition = returnPosition
      AnimatedVisibility(visible = visibleReturnPosition != null) {
        visibleReturnPosition?.let { targetTime ->
          ReturnToPositionBanner(
            returnTime = targetTime,
            onContinueClick = {
              currentPlayer?.seekTo(targetTime, play = true)
              component.bookmarkReturnPositionStore.clear(libraryItemId)
              onDismissSheet()
            },
            onDismissClick = {
              component.bookmarkReturnPositionStore.clear(libraryItemId)
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
      }

      LazyColumn(
        modifier = Modifier.weight(1f, fill = false),
      ) {
        loadState
          .onLoaded { bookmarks ->
            items(
              items = bookmarks,
              key = { it.time.inWholeSeconds },
            ) { bookmark ->
              BookmarkListItem(
                bookmark = bookmark,
                onClick = {
                  component.bookmarkReturnPositionStore.recordJumpFrom(
                    libraryItemId = libraryItemId,
                    fromTime = currentTime,
                    toTime = bookmark.time,
                  )
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
      }
      HorizontalDivider()
      CreateNewListItem(
        currentTime = currentTime,
        onClick = {
          showCreateDialog = currentTime
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      )
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
            scope.launch {
              withContext(NonCancellable) {
                bookmarksToDelete.forEach { bookmark ->
                  Analytics.send(PlaybackActionEvent(BookmarkObj, Deleted))
                  component.bookmarkRepository
                    .removeBookmark(bookmark.libraryItemId, bookmark.time)
                }
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
      liveTime = currentTime,
      maxDuration = currentPlayer?.preparedSession?.duration?.takeIf { it > Duration.ZERO },
      hasPlayer = currentPlayer != null,
      isPlaying = playerState == AudioPlayer.State.Playing,
      isEditing = existing != null,
      useCompactLayout = useCompactLayout,
      returnPosition = returnPosition,
      initialTitle = initialTitle,
      initialNotes = initialNotes,
      chapterTitleResolver = { ts -> currentPlayer?.preparedSession?.chapterAt(ts)?.title },
      onPause = { currentPlayer?.pause() },
      onSeek = { timestamp, play -> currentPlayer?.seekTo(timestamp, play) },
      onRecordReturnPosition = { fromTime, toTime ->
        component.bookmarkReturnPositionStore.recordJumpFrom(
          libraryItemId = libraryItemId,
          fromTime = fromTime,
          toTime = toTime,
        )
      },
      onReturnToPositionInDialog = { pos ->
        currentPlayer?.seekTo(pos, play = false)
        component.bookmarkReturnPositionStore.clear(libraryItemId)
      },
      onDismiss = {
        showCreateDialog = null
        editingBookmark = null
      },
      onSave = { encodedTitle, timestamp ->
        scope.launch {
          withContext(NonCancellable) {
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
          var isNotesExpanded by remember(bookmark.time, displayNotes) { mutableStateOf(false) }
          var canExpandNotes by remember(bookmark.time, displayNotes) { mutableStateOf(false) }

          Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = displayNotes,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = if (isNotesExpanded) Int.MAX_VALUE else 6,
              overflow = TextOverflow.Ellipsis,
              onTextLayout = { result ->
                if (!isNotesExpanded) {
                  canExpandNotes = result.hasVisualOverflow
                }
              },
            )
            if (canExpandNotes || isNotesExpanded) {
              Text(
                text = stringResource(
                  if (isNotesExpanded) {
                    Res.string.bookmark_notes_show_less
                  } else {
                    Res.string.bookmark_notes_show_more
                  },
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                  .clickable { isNotesExpanded = !isNotesExpanded }
                  .padding(vertical = 2.dp),
              )
            }
          }
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
private fun ReturnToPositionBanner(
  returnTime: Duration,
  onContinueClick: () -> Unit,
  onDismissClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier
          .weight(1f)
          .clickable(onClick = onContinueClick)
          .padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = CampfireIcons.Rounded.History,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
          text = stringResource(Res.string.bookmark_continue_listening_at, returnTime.readoutFormat()),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
      val dismissLabel = stringResource(Res.string.bookmark_return_banner_dismiss)
      IconButton(
        onClick = onDismissClick,
      ) {
        Icon(
          imageVector = CampfireIcons.Rounded.Close,
          contentDescription = dismissLabel,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun BookmarkEditorDialog(
  currentTime: Duration,
  liveTime: Duration,
  maxDuration: Duration?,
  hasPlayer: Boolean,
  isPlaying: Boolean,
  isEditing: Boolean,
  useCompactLayout: Boolean,
  returnPosition: Duration?,
  initialTitle: String,
  initialNotes: String,
  chapterTitleResolver: (Duration) -> String?,
  onPause: () -> Unit,
  onSeek: (timestamp: Duration, play: Boolean) -> Unit,
  onRecordReturnPosition: (fromTime: Duration, toTime: Duration) -> Unit,
  onReturnToPositionInDialog: (Duration) -> Unit,
  onDismiss: () -> Unit,
  onSave: (encodedTitle: String, timestamp: Duration) -> Unit,
  modifier: Modifier = Modifier,
) {
  var bookmarkTime by remember(currentTime) { mutableStateOf(currentTime) }
  var hasPreviewedInEdit by remember(currentTime, isEditing) { mutableStateOf(false) }
  var pendingSeekTarget by remember { mutableStateOf<Duration?>(null) }
  var pendingSeekMark by remember { mutableStateOf<TimeMark?>(null) }
  val isTrackingLiveAudio = !isEditing || hasPreviewedInEdit

  LaunchedEffect(Unit) {
    if (isPlaying) {
      onPause()
    }
  }

  LaunchedEffect(liveTime, isPlaying, isTrackingLiveAudio) {
    if (!isPlaying || !isTrackingLiveAudio) return@LaunchedEffect
    val target = pendingSeekTarget
    if (target != null) {
      val settled = (liveTime - target).absoluteValue <= 2.seconds
      val timedOut = (pendingSeekMark?.elapsedNow() ?: Duration.ZERO) >= 1500.milliseconds
      if (!settled && !timedOut) return@LaunchedEffect
      pendingSeekTarget = null
      pendingSeekMark = null
    }
    bookmarkTime = liveTime
  }

  fun nudgeBy(delta: Duration) {
    val raw = (bookmarkTime + delta).coerceAtLeast(Duration.ZERO)
    val clamped = maxDuration?.let { raw.coerceAtMost(it) } ?: raw
    bookmarkTime = clamped
    if (hasPlayer && isTrackingLiveAudio) {
      pendingSeekTarget = clamped
      pendingSeekMark = TimeSource.Monotonic.markNow()
      onSeek(clamped, isPlaying)
    }
  }

  fun togglePlayPause() {
    if (!hasPlayer) return
    if (isPlaying && isTrackingLiveAudio) {
      onPause()
    } else {
      if (isEditing && !hasPreviewedInEdit) {
        onRecordReturnPosition(liveTime, bookmarkTime)
        hasPreviewedInEdit = true
      }
      pendingSeekTarget = bookmarkTime
      pendingSeekMark = TimeSource.Monotonic.markNow()
      onSeek(bookmarkTime, true)
    }
  }

  val chapterTitle = remember(bookmarkTime, chapterTitleResolver) { chapterTitleResolver(bookmarkTime) }
  val fallbackTitle = stringResource(Res.string.bookmark_default_title, bookmarkTime.readoutFormat())
  val defaultTitle = remember(chapterTitle, fallbackTitle) {
    chapterTitle?.replace('\n', ' ')?.replace('\r', ' ')?.trim()?.ifBlank { null } ?: fallbackTitle
  }
  var titleValue by remember(initialTitle) { mutableStateOf(TextFieldValue(initialTitle)) }
  var notesValue by remember(initialNotes) { mutableStateOf(TextFieldValue(initialNotes)) }

  val titleDictation = rememberVoiceDictationLauncher { spoken ->
    val merged = appendDictatedText(titleValue.text, spoken)
      .replace('\n', ' ')
      .replace('\r', ' ')
      .trim()
    titleValue = TextFieldValue(merged, TextRange(merged.length))
  }
  val notesDictation = rememberVoiceDictationLauncher { spoken ->
    val merged = appendDictatedText(notesValue.text, spoken)
    notesValue = TextFieldValue(merged, TextRange(merged.length))
  }

  BasicAlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    content = {
      Surface(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
      ) {
        Column(
          modifier = Modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
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

          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = stringResource(Res.string.bookmark_editor_timestamp_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              Spacer(Modifier.height(8.dp))

              val showPauseIcon = isPlaying && isTrackingLiveAudio
              val playPauseLabel = stringResource(Res.string.action_play_pause)
              val playPauseButtonSize = if (useCompactLayout) 42.dp else 48.dp

              if (useCompactLayout) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                  IconButtonTooltip(text = playPauseLabel) {
                    FilledIconButton(
                      onClick = ::togglePlayPause,
                      enabled = hasPlayer,
                      shape = CircleShape,
                      modifier = Modifier.size(playPauseButtonSize),
                    ) {
                      Icon(
                        imageVector = if (showPauseIcon) {
                          CampfireIcons.Rounded.Pause
                        } else {
                          CampfireIcons.Rounded.PlayArrow
                        },
                        contentDescription = playPauseLabel,
                      )
                    }
                  }

                  Text(
                    text = bookmarkTime.readoutFormat(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              } else {
                Text(
                  text = bookmarkTime.readoutFormat(),
                  style = MaterialTheme.typography.headlineSmall,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  softWrap = false,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                  val rewind30Label = stringResource(Res.string.cd_rewind_time, "30")
                  IconButtonTooltip(text = rewind30Label) {
                    IconButton(
                      onClick = { nudgeBy((-30).seconds) },
                    ) {
                      Icon(
                        imageVector = CampfireIcons.Rounded.Replay30,
                        contentDescription = rewind30Label,
                      )
                    }
                  }

                  IconButtonTooltip(text = playPauseLabel) {
                    FilledIconButton(
                      onClick = ::togglePlayPause,
                      enabled = hasPlayer,
                      shape = CircleShape,
                      modifier = Modifier.size(playPauseButtonSize),
                    ) {
                      Icon(
                        imageVector = if (showPauseIcon) {
                          CampfireIcons.Rounded.Pause
                        } else {
                          CampfireIcons.Rounded.PlayArrow
                        },
                        contentDescription = playPauseLabel,
                      )
                    }
                  }

                  val forward30Label = stringResource(Res.string.cd_forward_time, "30")
                  IconButtonTooltip(text = forward30Label) {
                    IconButton(
                      onClick = { nudgeBy(30.seconds) },
                    ) {
                      Icon(
                        imageVector = CampfireIcons.Rounded.Forward30,
                        contentDescription = forward30Label,
                      )
                    }
                  }
                }
              }

              Spacer(Modifier.height(10.dp))

              BookmarkFineNudgeRow(
                onNudge = ::nudgeBy,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          AnimatedVisibility(
            visible = isEditing && hasPreviewedInEdit && returnPosition != null,
          ) {
            returnPosition?.let { targetTime ->
              FilledTonalButton(
                onClick = {
                  hasPreviewedInEdit = false
                  pendingSeekTarget = null
                  pendingSeekMark = null
                  onReturnToPositionInDialog(targetTime)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 10.dp),
              ) {
                Icon(
                  imageVector = CampfireIcons.Rounded.History,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = stringResource(
                    Res.string.bookmark_continue_listening_at,
                    targetTime.readoutFormat(),
                  ),
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          Spacer(Modifier.height(16.dp))

          val dictateTitleLabel = stringResource(Res.string.action_dictate_bookmark_title)
          val dictateTitlePrompt = stringResource(Res.string.bookmark_dictation_prompt_title)
          OutlinedTextField(
            value = titleValue,
            onValueChange = { newValue ->
              if (isPlaying && isTrackingLiveAudio && newValue.text != titleValue.text) {
                onPause()
              }
              titleValue = newValue
            },
            label = { Text(stringResource(Res.string.bookmark_new_dialog_label_title)) },
            placeholder = { Text(defaultTitle) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Sentences,
            ),
            trailingIcon = if (titleDictation.isSupported) {
              {
                IconButtonTooltip(text = dictateTitleLabel) {
                  IconButton(
                    onClick = {
                      if (isPlaying) onPause()
                      titleDictation.launch(dictateTitlePrompt)
                    },
                  ) {
                    Icon(
                      CampfireIcons.Rounded.Mic,
                      contentDescription = dictateTitleLabel,
                    )
                  }
                }
              }
            } else {
              null
            },
            modifier = Modifier.fillMaxWidth(),
          )

          Spacer(Modifier.height(12.dp))

          val dictateNotesLabel = stringResource(Res.string.action_dictate_bookmark_notes)
          val dictateNotesPrompt = stringResource(Res.string.bookmark_dictation_prompt_notes)
          OutlinedTextField(
            value = notesValue,
            onValueChange = { newValue ->
              if (isPlaying && isTrackingLiveAudio && newValue.text != notesValue.text) {
                onPause()
              }
              notesValue = newValue
            },
            label = { Text(stringResource(Res.string.bookmark_dialog_label_notes)) },
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Sentences,
            ),
            trailingIcon = if (notesDictation.isSupported) {
              {
                IconButtonTooltip(text = dictateNotesLabel) {
                  IconButton(
                    onClick = {
                      if (isPlaying) onPause()
                      notesDictation.launch(dictateNotesPrompt)
                    },
                  ) {
                    Icon(
                      CampfireIcons.Rounded.Mic,
                      contentDescription = dictateNotesLabel,
                    )
                  }
                }
              }
            } else {
              null
            },
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

@Composable
private fun BookmarkFineNudgeRow(
  onNudge: (Duration) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    FineNudgePills.take(3).forEach { (label, delta) ->
      FineNudgePill(
        label = label,
        onClick = { onNudge(delta) },
        modifier = Modifier.weight(1f),
      )
    }

    Box(
      modifier = Modifier
        .width(1.dp)
        .height(18.dp)
        .background(MaterialTheme.colorScheme.outlineVariant),
    )

    FineNudgePills.drop(3).forEach { (label, delta) ->
      FineNudgePill(
        label = label,
        onClick = { onNudge(delta) },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun FineNudgePill(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .height(36.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Box(
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

private val FineNudgePills = listOf(
  "-10s" to (-10).seconds,
  "-5s" to (-5).seconds,
  "-1s" to (-1).seconds,
  "+1s" to 1.seconds,
  "+5s" to 5.seconds,
  "+10s" to 10.seconds,
)

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
