// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.search.ui

import androidx.compose.foundation.text.input.TextFieldState
import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.search.api.SearchRepository
import app.campfire.search.api.SearchResult
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class SearchPresenterTest {

  private class FakeSearchRepository : SearchRepository {
    val searchCalls = mutableListOf<String>()
    val cached = mutableMapOf<String, SearchResult.Success>()

    override fun getCachedResult(query: String): SearchResult.Success? {
      return cached[query.trim()]
    }

    override fun searchCurrentLibrary(query: String): Flow<SearchResult> {
      searchCalls += query
      val result = cached[query.trim()] ?: SearchResult.Empty
      return flowOf(result)
    }
  }

  private val searchRepository = FakeSearchRepository()
  private val offlineDownloadManager = FakeOfflineDownloadManager()
  private val analytics = FakeAnalytics()

  private fun createPresenter(textFieldState: TextFieldState) = SearchPresenter(
    searchRepository = searchRepository,
    textFieldState = textFieldState,
    onSearchEvent = {},
    offlineDownloadManager = offlineDownloadManager,
    analytics = analytics,
  )

  @Test
  fun cachedQueryEmitsSuccessImmediatelyWithoutLoadingOrRefetch() = runTest {
    val book = libraryItem(id = "book-1", media = media(metadata = mediaMetadata(title = "Dune")))
    val expected = SearchResult.Empty.copy(books = listOf(book))
    searchRepository.cached["dune"] = expected

    val presenter = createPresenter(TextFieldState(initialText = "dune"))
    presenter.test {
      val initial = awaitItem()
      assertThat(initial.searchResult).isInstanceOf<SearchResult.Success>()
      assertThat((initial.searchResult as SearchResult.Success).books.map { it.id })
        .isEqualTo(listOf("book-1"))
      assertThat(searchRepository.searchCalls).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun uncachedQueryFetchesViaRepository() = runTest {
    val book = libraryItem(id = "book-2", media = media(metadata = mediaMetadata(title = "Foundation")))
    val expected = SearchResult.Empty.copy(books = listOf(book))

    val repo = object : SearchRepository {
      val calls = mutableListOf<String>()
      override fun searchCurrentLibrary(query: String): Flow<SearchResult> {
        calls += query
        return flowOf(expected)
      }
    }

    val presenter = SearchPresenter(
      searchRepository = repo,
      textFieldState = TextFieldState(initialText = "foundation"),
      onSearchEvent = {},
      offlineDownloadManager = offlineDownloadManager,
      analytics = analytics,
    )

    presenter.test {
      var state = awaitItem()
      while (state.searchResult.let { it !is SearchResult.Success || it.books.isEmpty() }) {
        state = awaitItem()
      }
      assertThat((state.searchResult as SearchResult.Success).books.map { it.id })
        .isEqualTo(listOf("book-2"))
      assertThat(repo.calls).isEqualTo(listOf("foundation"))
      cancelAndIgnoreRemainingEvents()
    }
  }
}
