// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series

import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.MediaType
import app.campfire.core.model.SeriesSequence
import app.campfire.core.model.ShelfType
import app.campfire.core.session.UserSession
import app.campfire.core.time.GrandFatherTime
import app.campfire.data.LibraryItem as DbLibraryItem
import app.campfire.data.Media as DbMedia
import app.campfire.data.Shelf
import app.campfire.data.ShelfJoin
import app.campfire.data.mapping.asDatabaseModel
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.db.test.model.createDbSeries
import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User as NetworkUser
import app.campfire.network.models.UserPermissions
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.series.paging.SeriesPagerFactory
import app.campfire.series.paging.SeriesRemoteMediator
import app.campfire.series.paging.SeriesRemoteMediatorFactory
import app.campfire.series.store.SeriesStore
import app.campfire.user.test.FakeUserRepository
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsList
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class StoreSeriesRepositoryContinueSeriesTest {

  private fun fkEnabledDbTest(block: suspend (CampfireDatabase) -> Unit) = runTest {
    val driver = createDriver()
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    try {
      block(DatabaseFactory(driver).build())
    } finally {
      driver.close()
    }
  }

  @Test
  fun backfillQueriesDoNotThrowForeignKeyViolationOnOrphanedSeriesReferences() = fkEnabledDbTest { db ->
    seedUserAndServer(db)
    seedSeries(db, id = "series-valid", inProgress = true)
    seedBook(db, itemId = "book-valid", seriesId = "series-valid", seriesName = "Valid Series")
    seedBook(db, itemId = "book-orphan", seriesId = "series-missing", seriesName = "Missing Series")
    seedContinueSeriesShelf(db, shelfId = "continue-series_1", seriesEntityIds = listOf("series-missing"))

    db.seriesBookJoinQueries.insertMissingMediaSeriesJoins()
    db.seriesBookJoinQueries.insertMissingShelfJoins()
    db.seriesQueries.insertMissingShelfSeries(userId = "nonexistent-user")

    val validBooks = db.seriesQueries
      .selectContinueSeries(userId = USER_ID, libraryId = LIBRARY_ID)
      .awaitAsList()
    assertThat(validBooks.map { it.id }).containsExactly("series-valid")
  }

  @Test
  fun observeAndRefreshContinueSeriesSucceedEvenWithOrphanedMediaSeriesInDb() = fkEnabledDbTest { db ->
    seedUserAndServer(db)
    seedSeries(db, id = "series-valid", inProgress = true)
    seedBook(db, itemId = "book-valid", seriesId = "series-valid", seriesName = "Valid Series")
    seedBook(db, itemId = "book-orphan", seriesId = "series-missing", seriesName = "Missing Series")
    seedContinueSeriesShelf(db, shelfId = "continue-series_1", seriesEntityIds = listOf("series-missing"))

    val api = FakeAudioBookShelfApi().apply {
      personalizedHomeResult = { Result.success(emptyList()) }
    }
    val repository = createRepository(db, api)

    val series = repository.observeContinueSeries().first { list ->
      list.any { !it.books.isNullOrEmpty() }
    }
    assertThat(series.map { it.id }).containsExactly("series-valid")
    assertThat(series.single().books.orEmpty().map { it.id }).containsExactly("book-valid")

    val refreshResult = repository.refreshContinueSeries()
    assertThat(refreshResult.isSuccess).isTrue()
    assertThat(api.personalizedHomeRequests).containsExactly(LIBRARY_ID)
  }

  private fun createRepository(
    db: CampfireDatabase,
    api: FakeAudioBookShelfApi,
  ): StoreSeriesRepository {
    val dispatchers = DispatcherProvider(
      io = Dispatchers.Default,
      databaseWrite = Dispatchers.Default,
      databaseRead = Dispatchers.Default,
      computation = Dispatchers.Default,
      main = Dispatchers.Default,
    )
    val domainUser = user(USER_ID)
    val session = UserSession.LoggedIn(domainUser)
    val userRepository = FakeUserRepository().apply {
      currentStatefulUserFlow.value = domainUser
      currentUser = domainUser
      currentUserFlow.tryEmit(domainUser)
    }
    val urlHydrator = FakeUrlHydrator()
    return StoreSeriesRepository(
      userSession = session,
      api = api,
      db = db,
      userRepository = userRepository,
      urlHydrator = urlHydrator,
      dispatcherProvider = dispatchers,
      seriesStoreFactory = SeriesStore.Factory(session, api, db, urlHydrator, dispatchers),
      seriesPagerFactory = SeriesPagerFactory(
        remoteMediatorFactory = SeriesRemoteMediatorFactory { u, input ->
          SeriesRemoteMediator(u, input, api, db, dispatchers, GrandFatherTime)
        },
        db = db,
        urlHydrator = urlHydrator,
        dispatcherProvider = dispatchers,
      ),
    )
  }

  private suspend fun seedUserAndServer(db: CampfireDatabase) {
    db.serversQueries.insert(
      ServerSettings(
        id = "server-settings",
        scannerFindCovers = false,
        scannerCoverProvider = "google",
        scannerParseSubtitle = false,
        scannerPreferMatchedMetadata = false,
        scannerDisableWatcher = false,
        storeCoverWithItem = false,
        storeMetadataWithItem = false,
        metadataFileFormat = "json",
        rateLimitLoginRequests = 10,
        rateLimitLoginWindow = 600000L,
        backupSchedule = "30 1 * * *",
        backupsToKeep = 2,
        maxBackupSize = 1,
        loggerDailyLogsToKeep = 7,
        loggerScannerLogsToKeep = 2,
        homeBookshelfView = 1,
        bookshelfView = 1,
        sortingIgnorePrefix = false,
        sortingPrefixes = listOf("the"),
        chromecastEnabled = false,
        dateFormat = "MM/dd/yyyy",
        timeFormat = "HH:mm",
        language = "en-us",
        logLevel = 2,
        version = "2.36.0",
      ).asDatabaseModel(url = SERVER_URL, userId = USER_ID, name = "Home"),
    )
    db.usersQueries.insert(
      NetworkUser(
        id = USER_ID,
        username = "listener",
        type = "user",
        mediaProgress = emptyList(),
        seriesHideFromContinueListening = emptyList(),
        bookmarks = emptyList(),
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
      ).asDatabaseModel(SERVER_URL, LIBRARY_ID),
    )
  }

  private suspend fun seedSeries(
    db: CampfireDatabase,
    id: String,
    inProgress: Boolean,
  ) {
    db.seriesQueries.insertOrIgnore(
      createDbSeries(
        id = id,
        inProgress = inProgress,
        libraryId = LIBRARY_ID,
        userId = USER_ID,
      ),
    )
  }

  private suspend fun seedBook(
    db: CampfireDatabase,
    itemId: String,
    seriesId: String,
    seriesName: String,
  ) {
    db.libraryItemsQueries.insert(
      DbLibraryItem(
        id = itemId,
        ino = "ino-$itemId",
        libraryId = LIBRARY_ID,
        oldLibraryItemId = null,
        folderId = "folder-1",
        path = "/books/$itemId",
        relPath = itemId,
        isFile = false,
        mtimeMs = 0L,
        ctimeMs = 0L,
        birthtimeMs = 0L,
        addedAt = 0L,
        updatedAt = 0L,
        isMissing = false,
        isInvalid = false,
        mediaType = MediaType.Book,
        numFiles = 1,
        size = 1024L,
        serverUrl = SERVER_URL,
      ),
    )
    db.mediaQueries.insert(
      DbMedia(
        mediaId = "media-$itemId",
        coverPath = null,
        tags = emptyList(),
        numTracks = 1,
        numAudioFiles = 1,
        numChapters = 1,
        numMissingParts = 0,
        numInvalidAudioFiles = 0,
        durationInMillis = 60_000L,
        sizeInBytes = 1024L,
        propertySize = null,
        ebookFormat = null,
        metadata_title = "Book $itemId",
        metadata_subtitle = null,
        metadata_genres = emptyList(),
        metadata_publishedYear = null,
        metadata_publishedDate = null,
        metadata_publisher = null,
        metadata_description = null,
        metadata_isbn = null,
        metadata_asin = null,
        metadata_language = null,
        metadata_explicit = false,
        metadata_abridged = false,
        metadata_titleIgnorePrefix = null,
        metadata_authorName = "Author",
        metadata_authorNameLF = null,
        metadata_narratorName = "Narrator",
        metadata_seriesName = seriesName,
        metadata_series_id = seriesId,
        metadata_series_name = seriesName,
        metadata_series_sequence = 1.0,
        libraryItemId = itemId,
        metadata_series = listOf(SeriesSequence(seriesId, seriesName, 1.0)),
      ),
    )
  }

  private suspend fun seedContinueSeriesShelf(
    db: CampfireDatabase,
    shelfId: String,
    seriesEntityIds: List<String>,
  ) {
    db.shelfQueries.insert(
      Shelf(
        id = shelfId,
        libraryId = LIBRARY_ID,
        label = "Continue Series",
        labelStringKey = "LabelContinueSeries",
        total = seriesEntityIds.size,
        type = ShelfType.SERIES,
        homeOrder = 0,
        userId = USER_ID,
      ),
    )
    seriesEntityIds.forEachIndexed { index, entityId ->
      db.shelfQueries.insertJoins(
        ShelfJoin(
          shelfId = shelfId,
          entityId = entityId,
          shelfOrder = index,
          episodeId = "",
        ),
      )
    }
  }

  private companion object {
    const val USER_ID = "user-1"
    const val LIBRARY_ID = "fake_library_id"
    const val SERVER_URL = "fake_server_url"
  }
}
