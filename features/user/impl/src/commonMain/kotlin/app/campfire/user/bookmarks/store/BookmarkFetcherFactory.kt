// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.bookmarks.store

import app.campfire.core.model.Bookmark
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import org.mobilenativefoundation.store.store5.Fetcher

class BookmarkFetcherFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Fetcher<BookmarkStore.Operation.Item, List<Bookmark>> {
    return Fetcher.ofResult {
      api.getCurrentUser()
        .map { user -> user.bookmarks.map { it.asDomainModel(user.id) } }
        .asFetcherResult()
    }
  }
}
