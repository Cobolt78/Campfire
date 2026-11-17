// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.socket

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.model.Shelf
import app.campfire.home.api.model.ShelfId
import app.campfire.network.models.ExpandedBookMetadata
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.MediaExpanded
import app.campfire.network.models.MediaType
import app.campfire.socket.events.ItemAdded
import app.campfire.socket.events.ItemRemoved
import app.campfire.socket.events.ItemsAdded
import app.campfire.socket.events.UserSessionClosed
import app.campfire.socket.payloads.ItemRemovedPayload
import app.campfire.user.test.FakeUserRepository
import app.campfire.user.test.fixtures.user
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class HomeCacheInvalidationListenerTest {

  private class RecordingHomeRepository : HomeRepository {
    var refreshCount = 0
    override fun observeHomeFeed(): Flow<FeedResponse<List<Shelf>>> = emptyFlow()
    override suspend fun refreshHomeFeed() {
      refreshCount++
    }
    override suspend fun refreshHomeFeedIfStale(maxAge: Duration) = Unit
    override fun observeMediaProgress(libraryItemIds: Set<LibraryItemId>): Flow<Map<LibraryItemId, MediaProgress>> = emptyFlow()
    override fun observeShelf(shelfId: ShelfId, shelfType: ShelfType): Flow<List<ShelfEntity>> = emptyFlow()
  }

  private fun TestScope.createListener(
    homeRepository: RecordingHomeRepository,
    selectedLibraryId: String = "lib-1",
  ): HomeCacheInvalidationListener {
    val userRepository = FakeUserRepository().apply {
      currentUser = user(id = "user-1").copy(selectedLibraryId = selectedLibraryId)
    }
    return HomeCacheInvalidationListener(
      homeRepository = homeRepository,
      userRepository = userRepository,
      coroutineScopeHolder = CoroutineScopeHolder { backgroundScope },
    ).apply {
      debounceDuration = 1500.milliseconds
    }
  }

  @Test
  fun `debounces burst of ItemAdded and ItemsAdded into single refresh`() = runTest {
    val homeRepository = RecordingHomeRepository()
    val listener = createListener(homeRepository)

    listener.handle(ItemAdded(sampleBook(id = "book-1", libraryId = "lib-1")))
    advanceTimeBy(700)
    runCurrent()
    assertThat(homeRepository.refreshCount).isEqualTo(0)

    listener.handle(ItemAdded(sampleBook(id = "book-2", libraryId = "lib-1")))
    advanceTimeBy(700)
    runCurrent()
    assertThat(homeRepository.refreshCount).isEqualTo(0)

    listener.handle(ItemsAdded(listOf(sampleBook(id = "book-3", libraryId = "lib-1"))))
    advanceTimeBy(1499)
    runCurrent()
    assertThat(homeRepository.refreshCount).isEqualTo(0)

    advanceTimeBy(1)
    runCurrent()
    assertThat(homeRepository.refreshCount).isEqualTo(1)
  }

  @Test
  fun `item removed for selected library refreshes home feed`() = runTest {
    val homeRepository = RecordingHomeRepository()
    val listener = createListener(homeRepository)

    listener.handle(ItemRemoved(ItemRemovedPayload(id = "book-1", libraryId = "lib-1")))
    advanceTimeBy(1500)
    runCurrent()

    assertThat(homeRepository.refreshCount).isEqualTo(1)
  }

  @Test
  fun `ignores events for other libraries and unrelated events`() = runTest {
    val homeRepository = RecordingHomeRepository()
    val listener = createListener(homeRepository, selectedLibraryId = "lib-1")

    listener.handle(ItemAdded(sampleBook(id = "book-1", libraryId = "lib-2")))
    listener.handle(ItemsAdded(listOf(sampleBook(id = "book-2", libraryId = "lib-2"))))
    listener.handle(ItemsAdded(emptyList()))
    listener.handle(ItemRemoved(ItemRemovedPayload(id = "book-3", libraryId = "lib-2")))
    listener.handle(UserSessionClosed(sessionId = "session-1"))
    advanceTimeBy(2000)
    runCurrent()

    assertThat(homeRepository.refreshCount).isEqualTo(0)
  }

  private fun sampleBook(id: String, libraryId: String) = LibraryItemExpanded.Book(
    id = id,
    ino = "ino-$id",
    libraryId = libraryId,
    folderId = "folder-1",
    path = "/books/$id",
    relPath = "$id",
    isFile = false,
    mtimeMs = 0L,
    ctimeMs = 0L,
    birthtimeMs = 0L,
    addedAt = 0L,
    updatedAt = 0L,
    isMissing = false,
    isInvalid = false,
    mediaType = MediaType.Book,
    media = MediaExpanded(
      id = "media-$id",
      coverPath = null,
      metadata = ExpandedBookMetadata(title = "Book $id"),
    ),
  )
}
