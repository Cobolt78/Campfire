// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.Bookmark
import app.campfire.core.session.UserSession
import app.campfire.core.time.FatherTime
import app.campfire.data.Bookmarks
import app.campfire.data.mapping.asDatabaseModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.db.test.testDb
import app.campfire.network.models.AudioBookmark
import app.campfire.network.models.User as NetworkUser
import app.campfire.network.models.UserPermissions
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.bookmarks.StoreBookmarkRepository
import app.campfire.user.bookmarks.store.BookmarkStore
import app.cash.sqldelight.async.coroutines.awaitAsList
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.doesNotContain
import assertk.assertions.hasSize
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class StoreBookmarkRepositoryTest {

  private val serverUrl = "https://abs.example.com"
  private val fixedNow = LocalDateTime(2026, 1, 1, 12, 0, 0)

  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )

  private val fatherTime = object : FatherTime {
    override fun now(): LocalDateTime = fixedNow
    override fun today(): LocalDate = fixedNow.date
    override fun nowInEpochMillis(): Long = 1_767_268_800_000L
  }

  private suspend fun <T> withRealTimeout(
    timeoutMillis: Long = 5_000L,
    block: suspend CoroutineScope.() -> T,
  ): T = withContext(Dispatchers.Default) {
    withTimeout(timeoutMillis, block)
  }

  private fun networkUser(
    bookmarks: List<AudioBookmark> = emptyList(),
  ) = NetworkUser(
    id = USER_ID,
    username = "listener",
    type = "user",
    mediaProgress = emptyList(),
    seriesHideFromContinueListening = emptyList(),
    bookmarks = bookmarks,
    isActive = true,
    isLocked = false,
    lastSeen = 0L,
    createdAt = 0L,
    permissions = UserPermissions(
      download = true,
      update = false,
      delete = false,
      upload = false,
      accessAllLibraries = true,
      accessAllTags = true,
      accessExplicitContent = true,
    ),
    librariesAccessible = emptyList(),
  )

  private suspend fun seedUser(db: CampfireDatabase) {
    db.usersQueries.insert(networkUser().asDatabaseModel(serverUrl, "lib-1"))
  }

  private suspend fun insertLocalBookmark(
    db: CampfireDatabase,
    timeInSeconds: Int,
    title: String = "Bookmark @$timeInSeconds",
  ) {
    db.bookmarksQueries.insert(
      Bookmarks(
        userId = USER_ID,
        libraryItemId = BOOK_ID,
        title = title,
        timeInSeconds = timeInSeconds,
        createdAt = fixedNow,
      ),
    )
  }

  private fun createRepository(
    db: CampfireDatabase,
    api: FakeAudioBookShelfApi,
    scope: CoroutineScope,
  ): StoreBookmarkRepository {
    val domainUser = networkUser().asDomainModel(serverUrl, "lib-1")
    val session = UserSession.LoggedIn(domainUser)
    return StoreBookmarkRepository(
      userSession = session,
      storeFactory = BookmarkStore.Factory(
        api = api,
        db = db,
        dispatcherProvider = dispatchers,
      ),
      api = api,
      db = db,
      dispatcherProvider = dispatchers,
      fatherTime = fatherTime,
      coroutineScopeHolder = CoroutineScopeHolder { scope },
    )
  }

  @Test
  fun observeBookmarks_keepsCachedBookmarksWhenRefreshFails() = testDb { db ->
    seedUser(db)
    (1..12).forEach { i ->
      insertLocalBookmark(db, timeInSeconds = i * 60, title = "Bookmark #$i")
    }
    val fetchAttempted = CompletableDeferred<Unit>()
    val api = FakeAudioBookShelfApi().apply {
      currentUserResult = {
        withContext(Dispatchers.Default) { delay(50) }
        fetchAttempted.complete(Unit)
        Result.failure(IllegalStateException("network timeout"))
      }
    }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository = createRepository(db, api, scope)

    val observed = MutableStateFlow<List<List<Bookmark>>>(emptyList())
    val collector = scope.launch {
      repository.observeBookmarks(BOOK_ID).collect { list ->
        observed.value = observed.value + listOf(list)
      }
    }

    withRealTimeout {
      val initial = observed.first { it.isNotEmpty() }.first()
      assertThat(initial).hasSize(12)
      fetchAttempted.await()
    }
    withContext(Dispatchers.Default) { delay(300) }

    // Never wipes cached bookmarks to emptyList when background GET /api/me fails
    assertThat(observed.value.last()).hasSize(12)
    assertThat(observed.value).doesNotContain(emptyList<Bookmark>())

    collector.cancel()
    scope.cancel()
  }

  @Test
  fun offlineCreates_storeAllPendingEntries_doNotCrashAndReplayOnlyUnsynced() = testDb { db ->
    seedUser(db)
    // Existing 12 synced bookmarks on server & DB
    val initialServerBookmarks = (1..12).map { i ->
      AudioBookmark(
        libraryItemId = BOOK_ID,
        title = "Synced #$i",
        time = (i * 10).toFloat(),
        createdAt = 1_700_000_000_000L,
      )
    }
    initialServerBookmarks.forEach { insertLocalBookmark(db, it.time.toInt(), it.title) }

    val offlineFetchAttempted = CompletableDeferred<Unit>()
    val api = FakeAudioBookShelfApi().apply {
      createBookmarkResult = { _, _, _ -> Result.failure(IllegalStateException("offline")) }
      currentUserResult = {
        offlineFetchAttempted.complete(Unit)
        Result.failure(IllegalStateException("offline"))
      }
    }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository = createRepository(db, api, scope)

    // Fail two creates on the same book while offline
    repository.createBookmark(BOOK_ID, 130.seconds, "Offline 130")
    repository.createBookmark(BOOK_ID, 140.seconds, "Offline 140")

    val failedEntries = db.bookmarkBookkeepingQueries.selectFailedCreateEntries(USER_ID, BOOK_ID).awaitAsList()
    assertThat(failedEntries.map { it.timeInSeconds }).containsExactlyInAnyOrder(130, 140)

    // Observing while still offline and while eager conflict resolution fails must not throw or stall
    withRealTimeout {
      val stillOffline = repository.observeBookmarks(BOOK_ID).first { it.size == 14 }
      assertThat(stillOffline.map { it.time.inWholeSeconds.toInt() })
        .containsExactly(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140)
      offlineFetchAttempted.await()
    }
    withContext(Dispatchers.Default) { delay(150) }

    // Come back online: server receives ONLY the 2 unsynced bookmarks (not all 12 already-synced ones)
    api.createdBookmarks.clear()
    val syncedAll14 = initialServerBookmarks + listOf(
      AudioBookmark(BOOK_ID, "Offline 130", 130f, 1_700_000_000_000L),
      AudioBookmark(BOOK_ID, "Offline 140", 140f, 1_700_000_000_000L),
    )
    api.createBookmarkResult = { libraryItemId, timeInSeconds, title ->
      Result.success(AudioBookmark(libraryItemId, title, timeInSeconds.toFloat(), 1_700_000_000_000L))
    }
    api.currentUserResult = { Result.success(networkUser(bookmarks = syncedAll14)) }

    withRealTimeout {
      repository.observeBookmarks(BOOK_ID).first { it.size == 14 }
      while (db.bookmarkBookkeepingQueries.selectFailedCreateEntries(USER_ID, BOOK_ID).awaitAsList().isNotEmpty()) {
        delay(25)
      }
    }

    assertThat(api.createdBookmarks.map { it.second }).containsExactlyInAnyOrder(130, 140)
    scope.cancel()
  }

  @Test
  fun writeAll_preservesPendingOfflineCreates_andDoesNotResurrectPendingOfflineDeletes() = testDb { db ->
    seedUser(db)
    insertLocalBookmark(db, 10, "Bookmark 10")
    insertLocalBookmark(db, 20, "Bookmark 20")

    val api = FakeAudioBookShelfApi().apply {
      createBookmarkResult = { _, _, _ -> Result.failure(IllegalStateException("offline")) }
      removeBookmarkResult = { _, _ -> Result.failure(IllegalStateException("offline")) }
      // Server still has old snapshot [10, 20] plus a new bookmark [99] from another device
      currentUserResult = {
        Result.success(
          networkUser(
            bookmarks = listOf(
              AudioBookmark(BOOK_ID, "Bookmark 10", 10f, 1_700_000_000_000L),
              AudioBookmark(BOOK_ID, "Bookmark 20", 20f, 1_700_000_000_000L),
              AudioBookmark(BOOK_ID, "Remote 99", 99f, 1_700_000_000_000L),
            ),
          ),
        )
      }
    }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository = createRepository(db, api, scope)

    repository.removeBookmark(BOOK_ID, 10.seconds)
    repository.createBookmark(BOOK_ID, 30.seconds, "Bookmark 30")

    // Wait until writeAll commits Remote 99 so we prove writeAll actually executed:
    // Offline delete 10 must NOT resurrect and offline create 30 must NOT be deleted.
    withRealTimeout {
      val synced = repository.observeBookmarks(BOOK_ID).first { list ->
        list.any { it.time.inWholeSeconds == 99L }
      }
      assertThat(synced.map { it.time.inWholeSeconds.toInt() }).containsExactly(20, 30, 99)
    }

    scope.cancel()
  }

  @Test
  fun cancellingObserverDuringConflictSync_doesNotDeadlockStoreLock() = testDb { db ->
    seedUser(db)
    insertLocalBookmark(db, 100, "Pending")
    val syncStarted = CompletableDeferred<Unit>()
    val allowSyncToProceed = CompletableDeferred<Unit>()

    val api = FakeAudioBookShelfApi().apply {
      createBookmarkResult = { _, _, _ -> Result.failure(IllegalStateException("initial offline")) }
      currentUserResult = { Result.failure(IllegalStateException("offline")) }
    }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository = createRepository(db, api, scope)

    // Plant a failed create so next observe triggers tryEagerlyResolveConflicts
    repository.createBookmark(BOOK_ID, 100.seconds, "Pending")

    api.createBookmarkResult = { libraryItemId, timeInSeconds, title ->
      syncStarted.complete(Unit)
      allowSyncToProceed.await()
      Result.success(AudioBookmark(libraryItemId, title, timeInSeconds.toFloat(), 1_700_000_000_000L))
    }
    api.currentUserResult = {
      Result.success(
        networkUser(
          bookmarks = listOf(AudioBookmark(BOOK_ID, "Pending", 100f, 1_700_000_000_000L)),
        ),
      )
    }

    // Start observing in a short-lived sheet scope and cancel it immediately after eager sync begins
    val uiScope = CoroutineScope(Job() + Dispatchers.Default)
    val uiCollectJob = uiScope.launch {
      repository.observeBookmarks(BOOK_ID).collect {}
    }

    withRealTimeout {
      syncStarted.await()
    }
    uiCollectJob.cancel()
    uiScope.cancel()
    allowSyncToProceed.complete(Unit)

    // Verify subsequent mutations & observations do NOT deadlock on storeLock
    withRealTimeout {
      repository.createBookmark(BOOK_ID, 200.seconds, "Created after cancel")
      val latest = repository.observeBookmarks(BOOK_ID).first { list ->
        list.any { it.time.inWholeSeconds == 200L }
      }
      assertThat(latest.map { it.time.inWholeSeconds.toInt() }).containsExactly(100, 200)
    }

    scope.cancel()
  }

  private companion object {
    const val USER_ID = "user-1"
    const val BOOK_ID = "li_inn_side_shop"
  }
}
