// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.map
import app.campfire.home.api.model.ShelfIds
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.api.MediaProgressRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(HomeScreen::class, UserScope::class)
@Inject
class HomePresenter(
  @Assisted private val navigator: Navigator,
  private val homeRepository: HomeRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val libraryItemRepository: LibraryItemRepository,
  private val analytics: Analytics,
) : NonPausablePresenter<HomeUiState> {

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): HomeUiState {
    // Observe just the shelf information. We will use this to compose the remaining elements
    val domainFeed by remember {
      homeRepository.observeHomeFeed()
    }.collectAsState(FeedResponse.Loading)

    // We want to make sure we are constantly and consistently observing the items for each shelf
    // so we are not constantly restarting the observations unless the root shelves themselves change
    val shelfEntities by remember {
      snapshotFlow { domainFeed.dataOrNull }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { shelves ->
          val shelfFlows = shelves.map { shelf ->
            homeRepository.observeShelf(shelf.id, shelf.type)
              .map { LoadState.Loaded(it) as LoadState<List<ShelfEntity>> }
              .onStart { emit(LoadState.Loading as LoadState<List<ShelfEntity>>) }
              .catch { emit(LoadState.Error as LoadState<List<ShelfEntity>>) }
              .map { entityLoadState ->
                shelf.id to entityLoadState
              }
          }

          combine(
            flows = shelfFlows,
            transform = { shelfFlows ->
              persistentMapOf(*shelfFlows)
            },
          )
        }
    }.collectAsState(persistentMapOf())

    val allDownloads by remember {
      offlineDownloadManager.observeAll()
    }.collectAsState(emptyList())

    val downloadedEntities by remember {
      snapshotFlow { allDownloads }
        .mapLatest { downloads ->
          downloads
            .filter { it.state == OfflineDownload.State.Completed }
            .mapNotNull { download ->
              val libraryItem = try {
                libraryItemRepository.getLibraryItem(download.libraryItemId)
              } catch (e: CancellationException) {
                throw e
              } catch (e: Exception) {
                null
              } ?: return@mapNotNull null

              val episodeId = download.episodeId
              if (episodeId == null) {
                libraryItem
              } else {
                val media = libraryItem.media as? Media.Podcast ?: return@mapNotNull null
                val episode = media.episodes.find { it.id == episodeId } ?: return@mapNotNull null
                ShelfEntity.EpisodeShelfEntry(libraryItem, episode)
              }
            }
        }
    }.collectAsState(emptyList())

    // Now combine both the shelves and entities into the final set of UiShelf to render
    // in the UI, replacing Newest Authors with Downloads.
    val feed by remember {
      derivedStateOf {
        domainFeed.map { shelves ->
          val filteredShelves = shelves.filterNot {
            it.id == ShelfIds.NewestAuthors || it.type == ShelfType.AUTHOR
          }
          val uiShelves = filteredShelves.map { shelf ->
            UiShelf(
              shelf,
              shelfEntities[shelf.id]
                ?: LoadState.Loading as LoadState<List<ShelfEntity>>,
            )
          }.toMutableList()

          if (downloadedEntities.isNotEmpty()) {
            uiShelves.add(
              UiShelf(
                id = "downloads",
                label = "Downloads",
                total = downloadedEntities.size,
                entities = LoadState.Loaded(downloadedEntities),
              ),
            )
          }

          uiShelves.toPersistentList()
        }
      }
    }

    val userMediaProgress by remember {
      mediaProgressRepository.observeAllProgress()
        .map { allProgress ->
          allProgress
            .associateBy { MediaProgressKey(it) }
            .toPersistentMap()
        }
    }.collectAsState(persistentMapOf())

    val offlineDownloads by remember {
      snapshotFlow { allDownloads }
        .map { downloads ->
          downloads.associateBy { it.libraryItemId }.toPersistentMap()
        }
    }.collectAsState(persistentMapOf())

    return HomeUiState(
      homeFeed = feed,
      offlineStates = offlineDownloads,
      progressStates = userMediaProgress,
    ) { event ->
      when (event) {
        is HomeUiEvent.OpenLibraryItem -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.item.id, event.sharedTransitionKey))
        }
        is HomeUiEvent.OpenLibraryItemWithEpisode -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(LibraryItemScreen(event.item.id, event.sharedTransitionKey, event.episodeId))
        }
        is HomeUiEvent.OpenAuthor -> {
          analytics.send(ContentSelected(ContentType.Author))
          navigator.goTo(AuthorDetailScreen(event.author.id, event.author.name))
        }
        is HomeUiEvent.OpenSeries -> {
          analytics.send(ContentSelected(ContentType.Series))
          navigator.goTo(SeriesDetailScreen(event.series.id, event.series.name))
        }
      }
    }
  }
}
