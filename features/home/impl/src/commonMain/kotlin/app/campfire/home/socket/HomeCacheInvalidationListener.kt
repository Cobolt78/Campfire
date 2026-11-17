// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.socket

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.home.api.HomeRepository
import app.campfire.socket.events.ItemAdded
import app.campfire.socket.events.ItemRemoved
import app.campfire.socket.events.ItemsAdded
import app.campfire.socket.events.SocketEvent
import app.campfire.socket.events.SocketEventListener
import app.campfire.user.api.UserRepository
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Listens for library-item addition/removal socket events and schedules a debounced refresh of the
 * home feed so shelves like "Recently Added" update live while avoiding request storms during bulk
 * server library scans.
 */
@ContributesIntoSet(UserScope::class, binding = binding<SocketEventListener>())
@Inject
class HomeCacheInvalidationListener(
  private val homeRepository: HomeRepository,
  private val userRepository: UserRepository,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : SocketEventListener {

  internal var debounceDuration: Duration = DEFAULT_DEBOUNCE_DURATION

  @Volatile
  private var pendingRefreshJob: Job? = null

  override suspend fun handle(event: SocketEvent) {
    val selectedLibraryId = runCatching { userRepository.getCurrentUser().selectedLibraryId }.getOrNull()
    val shouldRefresh = when (event) {
      is ItemAdded -> selectedLibraryId == null || event.item.libraryId == selectedLibraryId
      is ItemsAdded -> event.items.isNotEmpty() &&
        (selectedLibraryId == null || event.items.any { it.libraryId == selectedLibraryId })
      is ItemRemoved -> selectedLibraryId == null || event.payload.libraryId == selectedLibraryId
      else -> false
    }
    if (!shouldRefresh) return

    pendingRefreshJob?.cancel()
    pendingRefreshJob = coroutineScopeHolder.get().launch {
      delay(debounceDuration)
      homeRepository.refreshHomeFeed()
    }
  }

  companion object {
    val DEFAULT_DEBOUNCE_DURATION: Duration = 1500.milliseconds
  }
}
