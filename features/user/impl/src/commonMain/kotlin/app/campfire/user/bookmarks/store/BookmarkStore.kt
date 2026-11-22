// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.bookmarks.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.UserId
import app.campfire.network.AudioBookShelfApi
import dev.zacsweers.metro.Inject
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object BookmarkStore : Cork {

  override val tag: String = "BookmarkStore"
  override val enabled: Boolean = false

  @Inject
  class Factory(
    api: AudioBookShelfApi,
    db: CampfireDatabase,
    dispatcherProvider: DispatcherProvider,
  ) {
    private val fetcherFactory = BookmarkFetcherFactory(api)
    private val sourceOfTruthFactory = BookmarkSourceOfTruthFactory(db, dispatcherProvider)

    fun create(): Store<Operation.Item, List<Bookmark>> {
      return StoreBuilder
        .from(
          fetcher = fetcherFactory.create(),
          sourceOfTruth = sourceOfTruthFactory.create(),
        )
        .cachePolicy(
          MemoryPolicy.builder<Operation.Item, List<Bookmark>>()
            .setMaxSize(0)
            .build(),
        )
        .build()
    }
  }

  sealed interface Operation {
    data class Item(
      val userId: UserId,
      val libraryItemId: LibraryItemId,
    ) : Operation
  }
}
