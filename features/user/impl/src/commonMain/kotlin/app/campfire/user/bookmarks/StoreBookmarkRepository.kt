// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.bookmarks

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import app.campfire.data.BookmarkFailedCreate
import app.campfire.data.BookmarkFailedDelete
import app.campfire.data.Bookmarks
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.store.debugLogging
import app.campfire.network.AudioBookShelfApi
import app.campfire.user.api.BookmarkRepository
import app.campfire.user.bookmarks.store.BookmarkStore
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreBookmarkRepository(
  private val userSession: UserSession,
  private val storeFactory: BookmarkStore.Factory,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
  private val fatherTime: FatherTime,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : BookmarkRepository {

  private val bookmarkStore by lazy { storeFactory.create() }
  private val refreshMutex = Mutex()
  private val syncMutex = Mutex()
  private val activeRefreshes = mutableMapOf<Pair<UserId, LibraryItemId>, Job>()

  override fun observeBookmarks(libraryItemId: LibraryItemId): Flow<List<Bookmark>> {
    val userId = userSession.userId ?: return emptyFlow()

    // Observe SQLite directly so local cached bookmarks emit immediately, never disappear on
    // background refresh errors, and never contend on Store5's broken MutableStore lock.
    return db.bookmarksQueries.selectForItem(userId, libraryItemId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { rows ->
        rows.map { it.asDomainModel() }.sortedBy(Bookmark::time)
      }
      .onStart {
        triggerRefresh(userId, libraryItemId)
      }
      .distinctUntilChanged()
  }

  private suspend fun triggerRefresh(
    userId: UserId,
    libraryItemId: LibraryItemId,
  ) = withContext(NonCancellable) {
    val key = userId to libraryItemId
    refreshMutex.withLock {
      if (activeRefreshes[key]?.isActive == true) return@withContext

      val job = coroutineScopeHolder.get().launch(start = CoroutineStart.LAZY) {
        val self = coroutineContext.job
        try {
          syncItem(userId, libraryItemId)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          BookmarkStore.ebark(throwable = e) { "Failed to refresh bookmarks for $libraryItemId" }
        } finally {
          withContext(NonCancellable) {
            refreshMutex.withLock {
              if (activeRefreshes[key] === self) {
                activeRefreshes.remove(key)
              }
            }
          }
        }
      }
      activeRefreshes[key] = job
      job.start()
    }
  }

  private suspend fun syncItem(
    userId: UserId,
    libraryItemId: LibraryItemId,
  ) = syncMutex.withLock {
    val pendingResolved = syncPendingMutations(userId, libraryItemId)
    if (!pendingResolved) {
      BookmarkStore.dbark { "Pending bookmark sync incomplete for $libraryItemId; keeping queued mutations" }
    }

    val request = StoreReadRequest.fresh(BookmarkStore.Operation.Item(userId, libraryItemId))
    bookmarkStore.stream(request)
      .debugLogging(BookmarkStore.tag)
      .firstOrNull { response ->
        response !is StoreReadResponse.Loading &&
          (response.origin is StoreReadResponseOrigin.Fetcher || response is StoreReadResponse.Error)
      }
  }

  private suspend fun syncPendingMutations(
    userId: UserId,
    libraryItemId: LibraryItemId,
  ): Boolean {
    val failedCreates = withContext(dispatcherProvider.databaseRead + NonCancellable) {
      db.bookmarkBookkeepingQueries
        .selectFailedCreateEntries(userId, libraryItemId)
        .awaitAsList()
    }
    for (failedCreate in failedCreates) {
      if (!syncCreateIfPending(userId, libraryItemId, failedCreate.timeInSeconds)) {
        return false
      }
    }

    val failedDeletes = withContext(dispatcherProvider.databaseRead + NonCancellable) {
      db.bookmarkBookkeepingQueries
        .selectFailedDeleteEntries(userId, libraryItemId)
        .awaitAsList()
    }
    for (failedDelete in failedDeletes) {
      if (!syncDeleteIfPending(userId, libraryItemId, failedDelete.timeInSeconds)) {
        return false
      }
    }

    return true
  }

  private suspend fun syncCreateIfPending(
    userId: UserId,
    libraryItemId: LibraryItemId,
    timeInSeconds: Int,
  ): Boolean {
    val isPending = withContext(dispatcherProvider.databaseRead + NonCancellable) {
      db.bookmarkBookkeepingQueries
        .selectFailedCreateEntries(userId, libraryItemId)
        .awaitAsList()
        .any { it.timeInSeconds == timeInSeconds }
    }
    if (!isPending) return true

    val localBookmark = withContext(dispatcherProvider.databaseRead + NonCancellable) {
      db.bookmarksQueries
        .selectForItem(userId, libraryItemId)
        .awaitAsList()
        .firstOrNull { it.timeInSeconds == timeInSeconds }
    }
    if (localBookmark == null) {
      withContext(dispatcherProvider.databaseWrite + NonCancellable) {
        db.bookmarkBookkeepingQueries.deleteFailedCreate(
          userId = userId,
          libraryItemId = libraryItemId,
          timeInSeconds = timeInSeconds,
        )
      }
      return true
    }

    val result = api.createBookmark(libraryItemId, timeInSeconds, localBookmark.title)
    BookmarkStore.ibark { "create ($timeInSeconds) --> $result" }
    if (result.isFailure) return false

    val latestTitle = withContext(dispatcherProvider.databaseRead + NonCancellable) {
      db.bookmarksQueries
        .selectForItem(userId, libraryItemId)
        .awaitAsList()
        .firstOrNull { it.timeInSeconds == timeInSeconds }
        ?.title
    }
    if (latestTitle == localBookmark.title) {
      withContext(dispatcherProvider.databaseWrite + NonCancellable) {
        db.bookmarkBookkeepingQueries.deleteFailedCreate(
          userId = userId,
          libraryItemId = libraryItemId,
          timeInSeconds = timeInSeconds,
        )
      }
    }
    return true
  }

  private suspend fun syncDeleteIfPending(
    userId: UserId,
    libraryItemId: LibraryItemId,
    timeInSeconds: Int,
  ): Boolean {
    val isPending = withContext(dispatcherProvider.databaseRead + NonCancellable) {
      db.bookmarkBookkeepingQueries
        .selectFailedDeleteEntries(userId, libraryItemId)
        .awaitAsList()
        .any { it.timeInSeconds == timeInSeconds }
    }
    if (!isPending) return true

    val result = api.removeBookmark(libraryItemId, timeInSeconds)
    BookmarkStore.ibark { "delete ($timeInSeconds) --> $result" }
    if (result.isFailure) return false

    withContext(dispatcherProvider.databaseWrite + NonCancellable) {
      db.bookmarkBookkeepingQueries.deleteFailedDelete(
        userId = userId,
        libraryItemId = libraryItemId,
        timeInSeconds = timeInSeconds,
      )
    }
    return true
  }

  override suspend fun createBookmark(libraryItemId: LibraryItemId, timestamp: Duration, title: String) {
    val currentUserId = userSession.userId ?: return
    val timeInSeconds = timestamp.inWholeSeconds.toInt()

    // Persist the bookmark and pending-sync record atomically before network I/O so the UI updates
    // immediately and concurrent GET /api/me refreshes cannot drop unsynced bookmarks.
    withContext(dispatcherProvider.databaseWrite + NonCancellable) {
      db.bookmarksQueries.transaction {
        db.bookmarksQueries.insert(
          Bookmarks(
            userId = currentUserId,
            libraryItemId = libraryItemId,
            title = title,
            timeInSeconds = timeInSeconds,
            createdAt = fatherTime.now(),
          ),
        )
        db.bookmarkBookkeepingQueries.deleteFailedDelete(
          userId = currentUserId,
          libraryItemId = libraryItemId,
          timeInSeconds = timeInSeconds,
        )
        db.bookmarkBookkeepingQueries.insertFailedCreate(
          BookmarkFailedCreate(
            userId = currentUserId,
            libraryItemId = libraryItemId,
            timeInSeconds = timeInSeconds,
            timestamp = fatherTime.nowInEpochMillis(),
          ),
        )
      }
    }

    withContext(NonCancellable) {
      syncMutex.withLock {
        syncCreateIfPending(currentUserId, libraryItemId, timeInSeconds)
      }
    }
  }

  override suspend fun removeBookmark(libraryItemId: LibraryItemId, timestamp: Duration) {
    val currentUserId = userSession.userId ?: return
    val timeInSeconds = timestamp.inWholeSeconds.toInt()

    withContext(dispatcherProvider.databaseWrite + NonCancellable) {
      db.bookmarksQueries.transaction {
        db.bookmarksQueries.delete(
          userId = currentUserId,
          libraryItemId = libraryItemId,
          timeInSeconds = timeInSeconds,
        )
        db.bookmarkBookkeepingQueries.deleteFailedCreate(
          userId = currentUserId,
          libraryItemId = libraryItemId,
          timeInSeconds = timeInSeconds,
        )
        db.bookmarkBookkeepingQueries.insertFailedDelete(
          BookmarkFailedDelete(
            userId = currentUserId,
            libraryItemId = libraryItemId,
            timeInSeconds = timeInSeconds,
            timestamp = fatherTime.nowInEpochMillis(),
          ),
        )
      }
    }

    withContext(NonCancellable) {
      syncMutex.withLock {
        syncDeleteIfPending(currentUserId, libraryItemId, timeInSeconds)
      }
    }
  }
}
