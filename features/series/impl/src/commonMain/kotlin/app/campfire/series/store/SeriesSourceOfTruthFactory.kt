// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.core.util.runIfNotNull
import app.campfire.data.SeriesBookJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.model.mapToLibraryItemWithProgress
import app.campfire.network.models.Series as NetworkSeries
import app.campfire.series.store.SeriesStore.Key
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

internal class SeriesSourceOfTruthFactory(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val urlHydrator: UrlHydrator,
  private val dispatcherProvider: DispatcherProvider,
) {

  @OptIn(ExperimentalCoroutinesApi::class)
  fun create() = SourceOfTruth.of(
    reader = { key: Key ->
      db.seriesQueries.selectByLibraryId(key.libraryId)
        .asFlow()
        .mapToList(dispatcherProvider.databaseRead)
        .mapLatest { series ->
          withContext(dispatcherProvider.databaseRead) {
            val seriesIds = series.map { it.id }
            if (seriesIds.isEmpty()) return@withContext emptyList()

            val allBooksFlat = db.libraryItemsQueries
              .selectForMultipleSeries(
                userId = key.userId,
                seriesIds = seriesIds,
              )
              .awaitAsList()

            val booksBySeriesId = allBooksFlat.groupBy { it.joinSeriesId }

            series.map { dbSeries ->
              val booksForSeries = booksBySeriesId[dbSeries.id] ?: emptyList()
              val books = booksForSeries
                .map { row ->
                  mapToLibraryItemWithProgress(
                    id = row.id, ino = row.ino, libraryId = row.libraryId, oldLibraryItemId = row.oldLibraryItemId,
                    folderId = row.folderId, path = row.path, relPath = row.relPath, isFile = row.isFile,
                    mtimeMs = row.mtimeMs, ctimeMs = row.ctimeMs, birthtimeMs = row.birthtimeMs,
                    addedAt = row.addedAt, updatedAt = row.updatedAt, isMissing = row.isMissing,
                    isInvalid = row.isInvalid, mediaType = row.mediaType, numFiles = row.numFiles,
                    size = row.size, serverUrl = row.serverUrl, mediaId = row.mediaId, coverPath = row.coverPath,
                    tags = row.tags, numTracks = row.numTracks, numAudioFiles = row.numAudioFiles,
                    numChapters = row.numChapters, numMissingParts = row.numMissingParts,
                    numInvalidAudioFiles = row.numInvalidAudioFiles, durationInMillis = row.durationInMillis,
                    sizeInBytes = row.sizeInBytes, propertySize = row.propertySize, ebookFormat = row.ebookFormat,
                    metadata_title = row.metadata_title, metadata_subtitle = row.metadata_subtitle,
                    metadata_genres = row.metadata_genres, metadata_publishedYear = row.metadata_publishedYear,
                    metadata_publishedDate = row.metadata_publishedDate, metadata_publisher = row.metadata_publisher,
                    metadata_description = row.metadata_description, metadata_isbn = row.metadata_isbn,
                    metadata_asin = row.metadata_asin, metadata_language = row.metadata_language,
                    metadata_explicit = row.metadata_explicit, metadata_abridged = row.metadata_abridged,
                    metadata_titleIgnorePrefix = row.metadata_titleIgnorePrefix, metadata_authorName = row.metadata_authorName,
                    metadata_authorNameLF = row.metadata_authorNameLF, metadata_narratorName = row.metadata_narratorName,
                    metadata_seriesName = row.metadata_seriesName, metadata_series_id = row.metadata_series_id,
                    metadata_series_name = row.metadata_series_name, metadata_series_sequence = row.metadata_series_sequence,
                    libraryItemId = row.libraryItemId, metadata_series = row.metadata_series,
                    id_ = row.id_, libraryItemId_ = row.libraryItemId_, userId = row.userId,
                    episodeId = row.episodeId, mediaItemId = row.mediaItemId, mediaItemType = row.mediaItemType,
                    duration = row.duration, progress = row.progress, currentTime = row.currentTime,
                    isFinished = row.isFinished, hideFromContinueListening = row.hideFromContinueListening,
                    ebookLocation = row.ebookLocation, ebookProgress = row.ebookProgress, lastUpdate = row.lastUpdate,
                    startedAt = row.startedAt, finishedAt = row.finishedAt, source = row.source
                  ).asDomainModel(urlHydrator)
                }
                .sortedBy { it.media.metadata.seriesSequence?.sequence }

              dbSeries.asDomainModel(
                books = books,
              )
            }
          }
        }
        .flowOn(dispatcherProvider.databaseRead)
    },
    writer = { key: Key, series: List<NetworkSeries> ->
      withContext(dispatcherProvider.databaseWrite) {
        db.transaction {
          series.forEach { series ->
            // Insert Series
            db.seriesQueries.insertOrIgnore(series.asDbModel(key.userId, key.libraryId))

            // Insert the series books
            series.books?.forEachIndexed { index, book ->
              val libraryItem = book.asDbModel(userSession.serverUrl)
              val media = book.media.asDbModel(book.id, fallbackSeriesSequence = (index + 1).toDouble())

              // If these items exist, lets not overwrite their metadata
              db.libraryItemsQueries.insertOrIgnore(libraryItem)
              db.mediaQueries.insertOrIgnore(media)

              // Make sure we keep our item series sequence up to date
              runIfNotNull(
                media.metadata_series_id,
                media.metadata_series_name,
                media.metadata_series_sequence,
              ) { id, name, sequence ->
                db.mediaQueries.updateSeriesSequence(id, name, sequence, book.id)
              }

              // Insert junction entry
              db.seriesBookJoinQueries.insert(
                SeriesBookJoin(
                  seriesId = series.id,
                  libraryItemId = book.id,
                ),
              )
            }
          }
        }
      }
    },
    delete = { key ->
      withContext(dispatcherProvider.databaseWrite) {
        db.seriesQueries.deleteForLibraryId(key.libraryId)
      }
    },
  )
}
