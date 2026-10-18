// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.User
import app.campfire.core.settings.SortDirection
import app.campfire.core.time.FatherTime
import app.campfire.data.LibraryItemPageJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.LibraryItemFilter
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.nextPage
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.withContext

@AssistedFactory
fun interface LibraryItemRemoteMediatorFactory {
  operator fun invoke(
    user: User,
    input: LibraryItemPagingInput,
  ): LibraryItemRemoteMediator
}

@OptIn(ExperimentalPagingApi::class)
@AssistedInject
class LibraryItemRemoteMediator(
  @Assisted private val user: User,
  @Assisted private val input: LibraryItemPagingInput,
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
  private val fatherTime: FatherTime,
  private val urlHydrator: UrlHydrator,
) : RemoteMediator<Int, LibraryItem>(), Cork {

  override val tag: String = "LibraryItemRemoteMediator"

  override suspend fun initialize(): InitializeAction {
    return InitializeAction.LAUNCH_INITIAL_REFRESH
  }

  override suspend fun load(
    loadType: LoadType,
    state: PagingState<Int, LibraryItem>,
  ): MediatorResult {
    ibark { "Mediator::load($loadType, anchor=${state.anchorPosition}, pages=${state.pages.size})" }
    return try {
      val loadKey = when (loadType) {
        LoadType.REFRESH -> null
        LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
        LoadType.APPEND -> {
          val nextPage = withContext(dispatcherProvider.databaseRead) {
            db.libraryItemPageQueries.selectNextPage(
              input = input.databaseKey,
              userId = user.id,
              libraryId = user.selectedLibraryId,
              mapper = { it ?: -1 },
            ).awaitAsOne().takeIf { it != -1 }
          }

          if (nextPage == null) {
            return MediatorResult.Success(
              endOfPaginationReached = true,
            )
          }

          nextPage
        }
      } ?: 0

      ibark { "Mediator::loadKey($loadKey)" }

      // Load the page from the network
      val response = api.getLibraryItemsMinified(
        libraryId = user.selectedLibraryId,
        filter = input.filter?.let {
          LibraryItemFilter(it.group, it.value)
        },
        sortMode = input.sortMode.networkKey.libraryItemSortKey,
        sortDescending = input.sortDirection == SortDirection.Descending,
        page = loadKey,
        limit = state.config.pageSize,
      )

      response.fold(
        onSuccess = { pagedResponse ->
          ibark { "Mediator::response(page=${pagedResponse.page}, nextPage=${pagedResponse.nextPage})" }

          db.transaction {
            if (loadType == LoadType.REFRESH) {
              val existingPage0 = db.libraryItemPageQueries.selectPage(
                input = input.databaseKey,
                page = pagedResponse.page,
                userId = user.id,
                libraryId = user.selectedLibraryId,
              ).awaitAsOneOrNull()

              val isPage0Unchanged = if (existingPage0 != null && existingPage0.total == pagedResponse.total) {
                val existingItems = db.libraryItemPageQueries.selectLibraryItemsForPage(
                  pageId = existingPage0.id,
                  userId = user.id,
                ).awaitAsList()
                val existingIds = existingItems.map { it.id }
                val newIds = pagedResponse.data.map { it.id }
                existingIds == newIds
              } else {
                false
              }

              if (isPage0Unchanged && existingPage0 != null) {
                // Page 0 items and total count are identical; do nothing to preserve cached pages and prevent invalidation
                return@transaction
              }

              // If Page 0 changed:
              // If user is scrolled past page 0, only replace page 0 so subsequent pages currently visible are not wiped out
              val isScrolledPastPage0 = state.anchorPosition != null && state.anchorPosition!! >= state.config.pageSize
              if (isScrolledPastPage0) {
                db.libraryItemPageQueries.deletePage(
                  input = input.databaseKey,
                  page = pagedResponse.page,
                  userId = user.id,
                  libraryId = user.selectedLibraryId,
                )
              } else {
                db.libraryItemPageQueries.deleteByInput(
                  input = input.databaseKey,
                  userId = user.id,
                  libraryId = user.selectedLibraryId,
                )
              }
            }

            pagedResponse.data.forEach { item ->
              when (item) {
                is LibraryItemMinified.Book -> {
                  val libraryItem = item.asDbModel(user.serverUrl)
                  val media = item.media.asDbModel(item.id)
                  db.libraryItemsQueries.insertOrIgnore(libraryItem)
                  db.mediaQueries.insertOrIgnore(media)
                }
                is LibraryItemMinified.Podcast -> {
                  val libraryItem = item.asDbModel(user.serverUrl)
                  val podcastMedia = item.media.asDbModel(item.id, urlHydrator)
                  db.libraryItemsQueries.insertOrIgnore(libraryItem)
                  db.podcastMediaQueries.insertOrIgnore(podcastMedia)
                }
              }
            }

            // Insert the Page + Joins
            db.libraryItemPageQueries.insertPage(
              id = null, // Auto-incrementing
              input = input.databaseKey,
              page = pagedResponse.page,
              nextPage = pagedResponse.nextPage,
              total = pagedResponse.total,
              libraryId = user.selectedLibraryId,
              userId = user.id,
              updatedAt = fatherTime.nowInEpochMillis(),
            )
            val pageId = db.libraryItemPageQueries
              .selectLastPageId()
              .awaitAsOne()

            ibark { "Inserted page ${pagedResponse.page} with rowId[$pageId]" }

            pagedResponse.data.forEachIndexed { index, item ->
              db.libraryItemPageQueries.insertPageJoin(
                LibraryItemPageJoin(
                  pageId = pageId,
                  libraryItemId = item.id,
                  pageIndex = index,
                ),
              )
            }
          }

          MediatorResult.Success(
            endOfPaginationReached = pagedResponse.nextPage == null,
          )
        },
        onFailure = { t ->
          MediatorResult.Error(t)
        },
      )
    } catch (e: Exception) {
      ebark(throwable = e) { "LibraryItem RemoteMediator Exception" }
      MediatorResult.Error(e)
    }
  }
}
