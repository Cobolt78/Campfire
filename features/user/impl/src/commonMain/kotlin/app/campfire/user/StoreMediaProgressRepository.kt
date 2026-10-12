// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.store.debugLogging
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.api.MediaProgressRepository
import app.campfire.user.mediaprogress.MediaProgressSynchronizer
import app.campfire.user.mediaprogress.store.MediaProgressStore
import app.campfire.user.mediaprogress.store.MediaProgressStore.Operation
import app.campfire.user.mediaprogress.store.MediaProgressStore.Output
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.impl.extensions.fresh
import org.mobilenativefoundation.store.store5.impl.extensions.get

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStoreApi::class)
@ContributesBinding(UserScope::class)
@SingleIn(UserScope::class)
@Inject
class StoreMediaProgressRepository(
  private val userSession: UserSession,
  private val storeFactory: MediaProgressStore.Factory,
  private val db: CampfireDatabase,
  private val api: AudioBookShelfApi,
  private val libraryItemDao: LibraryItemDao,
  private val mediaProgressSynchronizer: MediaProgressSynchronizer,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
) : MediaProgressRepository {

  private val store: Store<Operation, Output> by lazy { storeFactory.create() }

  override fun observeProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
    refresh: Boolean,
  ): Flow<MediaProgress?> {
    val request = StoreReadRequest.cached(
      key = Operation.Query.One(userSession.requiredUserId, libraryItemId, episodeId),
      refresh = refresh,
    )
    return store.stream(request)
      .debugLogging(
        tag = "MediaProgressStore::observeProgress($libraryItemId, $episodeId)",
        enabled = MediaProgressStore.enabled,
      )
      .map { it.dataOrNull() }
      .filterNotNull()
      .map { it.requireSingle() }
  }

  override suspend fun getProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
    fresh: Boolean,
  ): MediaProgress? {
    val userId = userSession.userId ?: return null
    val operation = Operation.Query.One(userId, libraryItemId, episodeId)
    return try {
      if (fresh) {
        store.fresh(operation).requireSingle()
      } else {
        store.get(operation).requireSingle()
      }
    } catch (_: Exception) {
      null
    }
  }

  override fun observeAllProgress(): Flow<List<MediaProgress>> {
    val userId = userSession.userId ?: return emptyFlow()
    val request = StoreReadRequest.cached(Operation.Query.All(userId), false)
    return store.stream(request)
      .debugLogging(
        tag = "MediaProgressStore::observeAllProgress",
        enabled = MediaProgressStore.enabled,
      )
      .map { it.dataOrNull() }
      .filterNotNull()
      .map { it.requireCollection() }
  }

  override suspend fun updateProgress(
    newProgress: MediaProgress,
    force: Boolean,
    skipUpload: Boolean,
    onlyIfFresher: Boolean,
  ) {
    // Update local storage
    val progressId = db.mediaProgressQueries.transactionWithResult<String?> {
      val existing = db.mediaProgressQueries.selectForEpisode(
        userId = newProgress.userId,
        libraryItemId = newProgress.libraryItemId,
        // DB stores book progress under the empty-string sentinel; episodes carry their id.
        episodeId = newProgress.episodeId.orEmpty(),
      ).awaitAsOneOrNull()

      if (onlyIfFresher && existing != null && existing.lastUpdate > newProgress.lastUpdate) {
        // The stored row is fresher (e.g. this device's own live playback writes) —
        // dropping the incoming stale echo is the same last-write-wins rule the fetch
        // path's insertIfFresher applies.
        return@transactionWithResult null
      }

      db.mediaProgressQueries.insert(
        newProgress.asDbModel(existing?.id),
      )

      existing?.id?.takeIf { it != MediaProgress.UNKNOWN_ID } ?: newProgress.id
    } ?: return

    val updatedProgress = newProgress.copy(id = progressId)

    // Kick off potential synchronizer (suppressed when the write originated remotely).
    if (!skipUpload) {
      mediaProgressSynchronizer.sync(updatedProgress, force)
    }
  }

  override suspend fun deleteProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    val currentUserId = userSession.requiredUserId
    val operation = Operation.Query.One(currentUserId, libraryItemId, episodeId)
    val existing = store.get(operation).requireSingle()
    if (existing != null && existing.id != MediaProgress.UNKNOWN_ID) {
      api.deleteMediaProgress(existing.id)
        .onSuccess {
          store.clear(operation)
        }
        .onFailure {
          MediaProgressStore.ebark(throwable = it) { "Failed to delete MediaProgress for $libraryItemId" }
        }
    }
  }

  override suspend fun markFinished(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    val currentUserId = userSession.requiredUserId
    val now = fatherTime.nowInEpochMillis()

    // 1. Update local database immediately so UI updates with zero lag
    withContext(dispatcherProvider.databaseWrite) {
      val operation = Operation.Query.One(currentUserId, libraryItemId, episodeId)
      val existing = store.get(operation).requireSingle()
      if (existing != null) {
        db.mediaProgressQueries.markFinished(
          timestamp = now,
          userId = currentUserId,
          libraryItemId = libraryItemId,
          // Empty string is the DB sentinel for "no episode" (book-level progress).
          episodeId = episodeId.orEmpty(),
        )
      } else {
        val libraryItem = libraryItemDao.hydrateById(libraryItemId)
        if (libraryItem != null) {
          val durationMillis = if (episodeId != null) {
            val episodeRow = withContext(dispatcherProvider.databaseRead) {
              db.podcastEpisodeQueries.selectForId(episodeId).awaitAsOneOrNull()
            }
            episodeRow?.durationInMillis ?: libraryItem.media.durationInMillis
          } else {
            libraryItem.media.durationInMillis
          }

          val newMediaProgress = app.campfire.data.MediaProgress(
            id = MediaProgress.UNKNOWN_ID,
            userId = currentUserId,
            libraryItemId = libraryItemId,
            episodeId = episodeId.orEmpty(),
            mediaItemId = episodeId ?: libraryItem.media.id,
            mediaItemType = libraryItem.mediaType,
            duration = durationMillis.milliseconds.toDouble(DurationUnit.SECONDS),
            progress = 1.0,
            currentTime = 0.0,
            isFinished = true,
            hideFromContinueListening = true,
            ebookLocation = null,
            ebookProgress = null,
            finishedAt = now,
            lastUpdate = now,
            startedAt = now,
            source = MediaProgress.Source.Local,
          )

          db.mediaProgressQueries.insert(
            newMediaProgress,
          )
        }
      }
    }

    // 2. Notify remote server in background
    api.updateMediaProgress(
      libraryItemId = libraryItemId,
      episodeId = episodeId,
      update = MediaProgressUpdatePayload(
        episodeId = episodeId,
        isFinished = true,
        finishedAt = now,
      ),
    ).onFailure {
      MediaProgressStore.ebark(throwable = it) { "Error marking finished for libraryItemId $libraryItemId" }
    }
  }

  override suspend fun markNotFinished(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    val currentUserId = userSession.requiredUserId

    // First we just fetch the existing media progressId for the given (library item, episode)
    val mediaProgressId = db.mediaProgressQueries
      .getMediaProgressId(
        userId = currentUserId,
        libraryItemId = libraryItemId,
        episodeId = episodeId.orEmpty(),
      )
      .awaitAsOneOrNull()

    // Always delete local progress immediately so UI updates instantly
    withContext(dispatcherProvider.databaseWrite) {
      deleteLocalProgress(libraryItemId, episodeId)
    }

    // If it exists and is not an un-synced Id
    if (mediaProgressId != null && mediaProgressId != MediaProgress.UNKNOWN_ID) {
      api.deleteMediaProgress(mediaProgressId).onFailure {
        MediaProgressStore.ebark(throwable = it) {
          "Error deleting progress for libraryItemId $libraryItemId"
        }
      }
    } else if (mediaProgressId == MediaProgress.UNKNOWN_ID) {
      // If we have an unsynced media progress, then lets fallback to the legacy
      // method and just use the update method to mark as not finished
      api.updateMediaProgress(
        libraryItemId = libraryItemId,
        episodeId = episodeId,
        update = MediaProgressUpdatePayload(
          episodeId = episodeId,
          isFinished = false,
          progress = 0f,
          currentTime = 0f,
          hideFromContinueListening = true,
        ),
      ).onFailure {
        MediaProgressStore.ebark(throwable = it) {
          "Error marking not finished for libraryItemId $libraryItemId"
        }
      }
    }
  }

  override suspend fun markAllFinished(keys: List<MediaProgressKey>) {
    if (keys.isEmpty()) return
    val finishedAt = fatherTime.nowInEpochMillis()
    api.batchUpdateMediaProgress(
      keys.map { key ->
        MediaProgressUpdatePayload(
          libraryItemId = key.libraryItemId,
          episodeId = key.episodeId,
          isFinished = true,
          finishedAt = finishedAt,
        )
      },
    ).onSuccess {
      val currentUserId = userSession.requiredUserId
      val untracked = withContext(dispatcherProvider.databaseWrite) {
        db.mediaProgressQueries.transactionWithResult {
          keys.filter { key ->
            val tracked = db.mediaProgressQueries.getMediaProgressId(
              userId = currentUserId,
              libraryItemId = key.libraryItemId,
              episodeId = key.episodeId.orEmpty(),
            ).awaitAsOneOrNull() != null
            if (tracked) {
              db.mediaProgressQueries.markFinished(
                timestamp = finishedAt,
                userId = currentUserId,
                libraryItemId = key.libraryItemId,
                episodeId = key.episodeId.orEmpty(),
              )
            }
            !tracked
          }
        }
      }

      // The server created progress for the items that had none, so pull those rows down
      // rather than inventing local ones that would lack a server id.
      untracked.forEach { key ->
        try {
          store.fresh(Operation.Query.One(currentUserId, key.libraryItemId, key.episodeId))
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          MediaProgressStore.ebark(throwable = e) { "Failed to fetch new progress for ${key.libraryItemId}" }
        }
      }
    }.onFailure {
      MediaProgressStore.ebark(throwable = it) { "Error marking ${keys.size} items finished" }
    }
  }

  override suspend fun markAllNotFinished(keys: List<MediaProgressKey>) {
    // The server has no batch delete, and un-finishing through the batch update leaves a
    // reset row behind instead of removing the progress, so un-finish each item on its own.
    coroutineScope {
      keys
        .map { key -> async { markNotFinished(key.libraryItemId, key.episodeId) } }
        .awaitAll()
    }
  }

  private suspend fun deleteLocalProgress(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId?,
  ) {
    val currentUserId = userSession.requiredUserId
    db.mediaProgressQueries.deleteForEpisode(
      userId = currentUserId,
      libraryItemId = libraryItemId,
      episodeId = episodeId.orEmpty(),
    )
  }
}
