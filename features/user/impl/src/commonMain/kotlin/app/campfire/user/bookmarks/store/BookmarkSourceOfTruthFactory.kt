// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.bookmarks.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.UserId
import app.campfire.core.model.isContentDifferent
import app.campfire.core.model.isSame
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

class BookmarkSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): SourceOfTruth<BookmarkStore.Operation.Item, List<Bookmark>, List<Bookmark>> {
    return SourceOfTruth.of(
      reader = { operation ->
        BookmarkStore.dbark { "handleRead($operation)" }
        observeForItem(operation.userId, operation.libraryItemId)
      },
      writer = { operation, bookmarks ->
        BookmarkStore.dbark { "handleWrite($operation, $bookmarks)" }
        writeAll(
          userId = operation.userId,
          bookmarks = bookmarks,
        )
      },
      delete = { operation ->
        BookmarkStore.dbark { "handleDelete($operation)" }
        deleteAll(
          userId = operation.userId,
          libraryItemId = operation.libraryItemId,
        )
      },
    )
  }

  private fun observeForItem(userId: UserId, libraryItemId: LibraryItemId): Flow<List<Bookmark>> {
    return db.bookmarksQueries.selectForItem(userId, libraryItemId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { bookmarks ->
        bookmarks.map { it.asDomainModel() }
      }
  }

  private suspend fun writeAll(
    userId: UserId,
    bookmarks: List<Bookmark>,
  ) = withContext(dispatcherProvider.databaseWrite + NonCancellable) {
    db.bookmarksQueries.transaction {
      val currentItems = db.bookmarksQueries.selectForUser(userId)
        .awaitAsList()
        .map { it.asDomainModel() }
        .toMutableList()

      val pendingCreateKeys = db.bookmarkBookkeepingQueries
        .selectAllFailedCreateEntriesForUser(userId)
        .awaitAsList()
        .map { it.libraryItemId to it.timeInSeconds }
        .toSet()

      val pendingDeleteKeys = db.bookmarkBookkeepingQueries
        .selectAllFailedDeleteEntriesForUser(userId)
        .awaitAsList()
        .map { it.libraryItemId to it.timeInSeconds }
        .toSet()

      bookmarks.forEach { bookmark ->
        val bookmarkKey = bookmark.libraryItemId to bookmark.time.inWholeSeconds.toInt()
        if (bookmarkKey in pendingDeleteKeys) {
          return@forEach
        }

        val existing = currentItems.find { it isSame bookmark }
          ?.also { currentItems.remove(it) }

        // If exists and is different update,
        if (existing != null && existing isContentDifferent bookmark) {
          db.bookmarksQueries.insert(bookmark.asDbModel())

          // If doesn't exist, insert
        } else if (existing == null) {
          db.bookmarksQueries.insert(bookmark.asDbModel())
        }
      }

      // Remove local items that are no longer on the server, preserving offline creates
      // that have not yet synced to the server.
      currentItems.forEach { deadBookmark ->
        val deadKey = deadBookmark.libraryItemId to deadBookmark.time.inWholeSeconds.toInt()
        if (deadKey in pendingCreateKeys) {
          return@forEach
        }

        db.bookmarksQueries.delete(
          userId = deadBookmark.userId,
          libraryItemId = deadBookmark.libraryItemId,
          timeInSeconds = deadBookmark.time.inWholeSeconds.toInt(),
        )
      }
    }
  }

  private suspend fun deleteAll(
    userId: UserId,
    libraryItemId: LibraryItemId,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.bookmarksQueries.deleteForItem(userId, libraryItemId)
  }
}
