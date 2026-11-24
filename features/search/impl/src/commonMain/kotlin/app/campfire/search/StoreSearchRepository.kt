// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.search

import app.campfire.core.di.UserScope
import app.campfire.core.time.FatherTime
import app.campfire.core.time.GrandFatherTime
import app.campfire.search.api.SearchRepository
import app.campfire.search.api.SearchResult
import app.campfire.search.store.SearchStore
import app.campfire.user.api.UserRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreSearchRepository(
  private val userRepository: UserRepository,
  private val searchStoreFactory: SearchStore.Factory,
  private val fatherTime: FatherTime = GrandFatherTime,
) : SearchRepository {

  private val store by lazy { searchStoreFactory.create() }

  @kotlin.concurrent.Volatile
  private var recentCache: Map<String, CachedSearchEntry> = emptyMap()

  private data class CachedSearchEntry(
    val result: SearchResult.Success,
    val cachedAtMillis: Long,
  )

  private fun cacheKey(userId: String, libraryId: String, query: String): String {
    return "$userId::$libraryId::${query.trim()}"
  }

  override fun getCachedResult(query: String): SearchResult.Success? {
    if (query.isBlank()) return null
    val user = userRepository.userFlow.value
    val key = cacheKey(user.id, user.selectedLibraryId, query)
    val entry = recentCache[key] ?: return null
    if (fatherTime.nowInEpochMillis() - entry.cachedAtMillis > CacheTtlMillis) {
      return null
    }
    return entry.result
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun searchCurrentLibrary(query: String): Flow<SearchResult> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val key = SearchStore.Operation.Query(
          userId = user.id,
          libraryId = user.selectedLibraryId,
          text = query,
        )
        // Must hit the fetcher: SearchSourceOfTruthFactory.handleRead uses mapNotNull, so it never
        // emits null for an unseen query and Store5 would never start the network fetch (endless
        // spinner). Instant back-navigation is served by recentCache / getCachedResult instead.
        val request = StoreReadRequest.freshWithFallBackToSourceOfTruth(key)
        store.stream(request)
          .filterNot { it is StoreReadResponse.NoNewData }
          .mapLatest { response ->
            when (response) {
              is StoreReadResponse.Loading -> SearchResult.Loading
              is StoreReadResponse.Error -> SearchResult.Error
              is StoreReadResponse.Data -> response.requireData()
              else -> SearchResult.Error
            }
          }
          .onEach { mapped ->
            if (mapped is SearchResult.Success) {
              val entryKey = cacheKey(user.id, user.selectedLibraryId, query)
              val updated = LinkedHashMap(recentCache).apply {
                remove(entryKey)
                put(entryKey, CachedSearchEntry(mapped, fatherTime.nowInEpochMillis()))
                while (size > MaxRecentEntries) {
                  val oldestKey = keys.firstOrNull() ?: break
                  remove(oldestKey)
                }
              }
              recentCache = updated
            }
          }
      }
  }

  private companion object {
    const val MaxRecentEntries = 10
    val CacheTtlMillis = 10.minutes.inWholeMilliseconds
  }
}
