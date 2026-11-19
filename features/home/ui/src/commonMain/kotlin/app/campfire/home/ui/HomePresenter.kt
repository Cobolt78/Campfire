// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.battery.BatteryOptimization
import app.campfire.audioplayer.battery.NoOpBatteryOptimization
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.UpcomingRelease
import app.campfire.common.compose.lifecycle.RefreshOnForegroundStaleOut
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.AuthorsScreen
import app.campfire.common.screens.ContinueSeriesScreen
import app.campfire.common.screens.DownloadsScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.filter.ContentFilter
import app.campfire.core.lifecycle.ForegroundResumeTracker
import app.campfire.core.lifecycle.NoOpForegroundResumeTracker
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.DownloadsSort
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.sortDownloadedItems
import app.campfire.core.time.FatherTime
import app.campfire.core.time.GrandFatherTime
import app.campfire.discover.api.screen.UpcomingScreen
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.map
import app.campfire.home.api.model.ShelfIds
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.settings.api.LibraryViewSettings
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.api.MediaProgressRepository
import app.campfire.updates.source.AppUpdateSource
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.Inject
import kotlin.time.Duration
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@CircuitInject(HomeScreen::class, UserScope::class)
@Inject
class HomePresenter(
  private val navigator: Navigator,
  private val homeRepository: HomeRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val libraryItemRepository: LibraryItemRepository,
  private val bookInfoRegistry: BookInfoRegistry,
  private val libraryViewSettings: LibraryViewSettings,
  private val analytics: Analytics,
  private val appUpdateSource: AppUpdateSource,
  private val batteryOptimization: BatteryOptimization = NoOpBatteryOptimization(),
  private val foregroundResumeTracker: ForegroundResumeTracker = NoOpForegroundResumeTracker,
  private val fatherTime: FatherTime = GrandFatherTime,
) : NonPausablePresenter<HomeUiState> {

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): HomeUiState {
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var isRefreshingDiscoveries by remember { mutableStateOf(false) }
    var isBatteryWarningDismissed by remember { mutableStateOf(false) }
    var isReorderingShelves by remember { mutableStateOf(false) }

    RefreshOnForegroundStaleOut(
      foregroundResumeTracker = foregroundResumeTracker,
      fatherTime = fatherTime,
      // Staleness is tracked per (user, library) in HomeRepository so socket refreshes
      // and manual refreshes share the same 5-minute SWR window.
      staleOutDuration = Duration.ZERO,
    ) {
      homeRepository.refreshHomeFeedIfStale()
    }

    val showBatteryWarning = if (batteryOptimization.isSupported && !isBatteryWarningDismissed) {
      val isIgnoring by remember(batteryOptimization) {
        batteryOptimization.observeIgnoringBatteryOptimizations()
      }.collectAsState(batteryOptimization.isIgnoringBatteryOptimizations())
      !isIgnoring
    } else {
      false
    }

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

    val upcomingReleases by remember {
      bookInfoRegistry.observeCachedUpcoming()
    }.collectAsState(emptyList())

    val completedDownloadEntries by remember {
      offlineDownloadManager.observeAll()
        .map { downloads ->
          downloads.filter { it.isCompleted }
        }
        .distinctUntilChanged()
        .mapLatest { downloads ->
          downloads.mapNotNull { dl ->
            try {
              val item = libraryItemRepository.getLibraryItem(dl.libraryItemId)
              dl to item
            } catch (e: CancellationException) {
              throw e
            } catch (e: Exception) {
              null
            }
          }
        }
    }.collectAsState(emptyList())

    val completedDownloads = remember(completedDownloadEntries) {
      completedDownloadEntries.map { it.second }
    }

    val downloadsSortModeRaw by remember {
      libraryViewSettings.observeDownloadsSortMode()
    }.collectAsState()
    val downloadsSortAscending by remember {
      libraryViewSettings.observeDownloadsSortAscending()
    }.collectAsState()
    val downloadsCustomOrder by remember {
      libraryViewSettings.observeDownloadsCustomOrder()
    }.collectAsState()
    val homeShelfOrder by remember {
      libraryViewSettings.observeHomeShelfOrder()
    }.collectAsState()

    val userMediaProgress by remember {
      mediaProgressRepository.observeAllProgress()
        .map { allProgress ->
          allProgress
            .associateBy { MediaProgressKey(it) }
            .toPersistentMap()
        }
    }.collectAsState(persistentMapOf())

    val feed by produceState(
      initialValue = domainFeed.map { persistentListOf() },
      domainFeed,
      shelfEntities,
      completedDownloadEntries,
      userMediaProgress,
      upcomingReleases,
      downloadsSortModeRaw,
      downloadsSortAscending,
      downloadsCustomOrder,
      homeShelfOrder,
    ) {
      withContext(Dispatchers.Default) {
        value = domainFeed.map { shelves ->
          val uiShelves = shelves
            .filter { it.id != ShelfIds.NewestAuthors && it.type != ShelfType.AUTHOR }
            .map { shelf ->
              UiShelf(
                shelf,
                shelfEntities[shelf.id]
                  ?: LoadState.Loading as LoadState<List<ShelfEntity>>,
              )
            }

          val withEnhancedContinue = enhanceContinueListeningShelf(uiShelves, completedDownloads, userMediaProgress)
          val withEnhancedListenAgain = enhanceListenAgainShelf(withEnhancedContinue, completedDownloads, userMediaProgress)
          val withUpcoming = insertUpcomingShelf(withEnhancedListenAgain, upcomingReleases).toMutableList()

          if (completedDownloadEntries.isNotEmpty()) {
            val sortMode = DownloadsSort.fromStorageKey(downloadsSortModeRaw)
            val sortedDownloads = sortDownloadedItems(
              items = completedDownloadEntries,
              sortMode = sortMode,
              sortAscending = downloadsSortAscending,
              customOrder = downloadsCustomOrder,
              libraryItem = { (_, item) -> item },
              progress = { (_, item) -> findUserMediaProgress(item, userMediaProgress) },
              downloadUpdateTimeMs = { (dl, _) -> dl.updateTimeMs },
              downloadContentLength = { (dl, _) -> dl.contentLength },
            ).map { (_, item) -> item }

            withUpcoming.add(
              UiShelf(
                id = "downloads",
                label = "Downloads",
                total = sortedDownloads.size,
                entities = LoadState.Loaded(sortedDownloads),
              ),
            )
          }

          val cappedShelves = withUpcoming.map { shelf ->
            if (shelf.id != "downloads" && shelf.entities is LoadState.Loaded) {
              shelf.copy(
                entities = LoadState.Loaded(shelf.entities.data.take(10)),
              )
            } else {
              shelf
            }
          }

          val orderedShelves = if (homeShelfOrder.isEmpty()) {
            cappedShelves
          } else {
            val orderMap = homeShelfOrder.withIndex().associate { (index, key) -> key to index }
            cappedShelves.sortedWith(
              compareBy { shelf -> orderMap[canonicalShelfKey(shelf.id)] ?: Int.MAX_VALUE },
            )
          }

          orderedShelves.toPersistentList()
        }
      }
    }

    val offlineDownloads by remember {
      combine(
        snapshotFlow { shelfEntities.values }
          .map { responses ->
            responses
              .mapNotNull { it.dataOrNull }
              .flatten()
              .filterIsInstance<LibraryItem>()
          },
        snapshotFlow { completedDownloads },
      ) { fromShelves, fromDownloads ->
        (fromShelves + fromDownloads).distinctBy { it.id }
      }
      .flatMapLatest { libraryItems ->
        offlineDownloadManager.observeForItems(libraryItems)
          .map { it.toPersistentMap() }
      }
    }.collectAsState(persistentMapOf())

    return HomeUiState(
      homeFeed = feed,
      offlineStates = offlineDownloads,
      progressStates = userMediaProgress,
      isRefreshing = isRefreshing,
      isRefreshingDiscoveries = isRefreshingDiscoveries,
      showBatteryWarning = showBatteryWarning,
      isReorderingShelves = isReorderingShelves,
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

        is HomeUiEvent.OpenUpcomingBook -> navigator.goTo(UrlScreen(event.url))
        HomeUiEvent.OpenUpcomingScreen -> navigator.goTo(UpcomingScreen)

        is HomeUiEvent.OpenShelf -> {
          when {
            event.shelfId.startsWith(ShelfIds.ContinueListening) -> {
              analytics.send(ActionEvent("shelf_header", "clicked", ShelfIds.ContinueListening))
              navigator.goTo(LibraryScreen(filter = ContentFilter.Progress(ContentFilter.Progress.Type.InProgress)))
            }

            event.shelfId.startsWith(ShelfIds.ListenAgain) -> {
              analytics.send(ActionEvent("shelf_header", "clicked", ShelfIds.ListenAgain))
              navigator.goTo(
                LibraryScreen(
                  filter = ContentFilter.Progress(ContentFilter.Progress.Type.Finished),
                  sortMode = ContentSortMode.LastListened,
                  sortDirection = SortDirection.Descending,
                ),
              )
            }

            event.shelfId.startsWith(ShelfIds.RecentlyAdded) -> {
              analytics.send(ActionEvent("shelf_header", "clicked", ShelfIds.RecentlyAdded))
              navigator.goTo(LibraryScreen(sortMode = ContentSortMode.AddedAt, sortDirection = SortDirection.Descending))
            }

            event.shelfId.startsWith(ShelfIds.RecentSeries) -> {
              analytics.send(ActionEvent("shelf_header", "clicked", ShelfIds.RecentSeries))
              navigator.goTo(SeriesScreen(sortMode = ContentSortMode.AddedAt, sortDirection = SortDirection.Descending))
            }

            event.shelfId.startsWith(ShelfIds.ContinueSeries) -> {
              analytics.send(ActionEvent("shelf_header", "clicked", ShelfIds.ContinueSeries))
              navigator.goTo(ContinueSeriesScreen)
            }

            event.shelfId == "downloads" -> {
              analytics.send(ActionEvent("shelf_header", "clicked", "downloads"))
              navigator.goTo(DownloadsScreen)
            }

            event.shelfId.startsWith(ShelfIds.UpcomingReleases) -> {
              analytics.send(ActionEvent("shelf_header", "clicked", ShelfIds.UpcomingReleases))
              navigator.goTo(UpcomingScreen)
            }

            event.shelfId.startsWith(ShelfIds.NewestAuthors) -> {
              analytics.send(ActionEvent("shelf_header", "clicked", ShelfIds.NewestAuthors))
              navigator.goTo(AuthorsScreen)
            }
          }
        }

        HomeUiEvent.Refresh -> {
          if (!isRefreshing) {
            isRefreshing = true
            coroutineScope.launch {
              try {
                homeRepository.refreshHomeFeed()
              } finally {
                isRefreshing = false
              }
            }
            coroutineScope.launch {
              appUpdateSource.checkForUpdates(force = false)
            }
          }
        }

        HomeUiEvent.RefreshDiscoveries -> {
          if (!isRefreshingDiscoveries) {
            isRefreshingDiscoveries = true
            coroutineScope.launch {
              try {
                homeRepository.refreshHomeFeed()
                delay(400)
              } finally {
                isRefreshingDiscoveries = false
              }
            }
          }
        }

        HomeUiEvent.RequestIgnoreBatteryOptimizations -> {
          batteryOptimization.requestIgnoreBatteryOptimizations()
        }

        HomeUiEvent.DismissBatteryWarning -> {
          isBatteryWarningDismissed = true
        }

        HomeUiEvent.ToggleReorderShelves -> {
          isReorderingShelves = !isReorderingShelves
        }

        is HomeUiEvent.MoveShelf -> {
          val currentShelves = feed.dataOrNull ?: return@HomeUiState
          val keys = currentShelves.map { canonicalShelfKey(it.id) }.toMutableList()
          val targetKey = canonicalShelfKey(event.shelfId)
          val index = keys.indexOf(targetKey)
          if (index != -1) {
            val newIndex = (index + event.delta).coerceIn(0, keys.lastIndex)
            if (newIndex != index) {
              val moved = keys.removeAt(index)
              keys.add(newIndex, moved)
              libraryViewSettings.homeShelfOrder = keys
            }
          }
        }

        HomeUiEvent.ResetShelfOrder -> {
          libraryViewSettings.homeShelfOrder = emptyList()
        }
      }
    }
  }
}

internal fun canonicalShelfKey(shelfId: String): String = shelfId.substringBefore('_')

/**
 * Weaves the cached upcoming shelf into the server's feed — after the
 * server's Discover shelf when present, at the end otherwise. Only dated
 * releases make the shelf (undated announcements live on the Upcoming screen,
 * which [UiShelf.total] still counts in full for the view-all card); nothing
 * is inserted when none qualify. The label stays empty here; the UI resolves
 * it from resources by shelf id.
 */
private fun insertUpcomingShelf(
  shelves: List<UiShelf<ShelfEntity>>,
  upcoming: List<UpcomingRelease>,
): List<UiShelf<ShelfEntity>> {
  val dated = upcoming.filter { it.entry.releaseDate != null }
  if (dated.isEmpty()) return shelves

  val upcomingShelf = UiShelf<ShelfEntity>(
    id = ShelfIds.UpcomingReleases,
    label = "",
    total = upcoming.size,
    entities = LoadState.Loaded(
      dated.map { release ->
        ShelfEntity.UpcomingBookShelfEntry(
          id = "${release.providerId.key}:${release.entry.providerBookId}",
          title = release.entry.title,
          seriesName = release.seriesName,
          releaseDate = release.entry.releaseDate,
          coverUrl = release.entry.coverUrl,
          providerUrl = release.entry.providerUrl,
        )
      },
    ),
  )

  val anchor = shelves.indexOfFirst {
    it.id.startsWith(ShelfIds.Discover)
  }
  return if (anchor >= 0) {
    shelves.toMutableList().apply { add(anchor + 1, upcomingShelf) }
  } else {
    shelves + upcomingShelf
  }
}

private fun isMediaProgressCompleted(prog: app.campfire.core.model.MediaProgress?): Boolean {
  if (prog == null) return false
  return prog.isCompleted || prog.hideFromContinueListening
}

private fun findUserMediaProgress(
  entity: ShelfEntity,
  userMediaProgress: Map<MediaProgressKey, app.campfire.core.model.MediaProgress>,
): app.campfire.core.model.MediaProgress? {
  return when (entity) {
    is LibraryItem -> {
      userMediaProgress[MediaProgressKey(entity.id, null)]
        ?: userMediaProgress.entries.firstOrNull { it.key.libraryItemId == entity.id }?.value
        ?: entity.userMediaProgress
    }
    is ShelfEntity.EpisodeShelfEntry -> {
      userMediaProgress[MediaProgressKey(entity.libraryItem.id, entity.recentEpisode.id)]
        ?: userMediaProgress[MediaProgressKey(entity.libraryItem.id, null)]
        ?: userMediaProgress.entries.firstOrNull { it.key.libraryItemId == entity.libraryItem.id }?.value
        ?: entity.libraryItem.userMediaProgress
    }
    else -> null
  }
}

private fun enhanceContinueListeningShelf(
  shelves: List<UiShelf<ShelfEntity>>,
  completedDownloads: List<LibraryItem>,
  userMediaProgress: Map<MediaProgressKey, app.campfire.core.model.MediaProgress>,
): List<UiShelf<ShelfEntity>> {
  fun entityKey(entity: ShelfEntity): String {
    return when (entity) {
      is LibraryItem -> entity.id
      is ShelfEntity.EpisodeShelfEntry -> "${entity.libraryItem.id}_${entity.recentEpisode.id}"
      is ShelfEntity.UpcomingBookShelfEntry -> entity.id
      is Author -> entity.id
      is Series -> entity.id
    }
  }

  fun getEntityLastUpdate(entity: ShelfEntity): Long {
    val prog = findUserMediaProgress(entity, userMediaProgress)
    return prog?.lastUpdate ?: 0L
  }

  fun isEntityActive(entity: ShelfEntity): Boolean {
    val prog = findUserMediaProgress(entity, userMediaProgress)
    if (prog != null) {
      if (isMediaProgressCompleted(prog)) return false
      if (prog.progress <= 0f && prog.currentTime <= 0f) return false
    }
    return true
  }

  val activeDownloadedItems = completedDownloads.filter { item ->
    val prog = findUserMediaProgress(item, userMediaProgress)
    prog != null &&
      !isMediaProgressCompleted(prog) &&
      (prog.progress > 0f || prog.currentTime > 0f)
  }

  val continueListeningIndex = shelves.indexOfFirst {
    it.id.startsWith(ShelfIds.ContinueListening)
  }

  if (continueListeningIndex >= 0) {
    val shelf = shelves[continueListeningIndex]
    val updatedEntitiesState: LoadState<List<ShelfEntity>> = when (val state = shelf.entities) {
      is LoadState.Loaded<List<ShelfEntity>> -> {
        val currentEntities = state.data
        val combined = (activeDownloadedItems + currentEntities)
          .distinctBy { entityKey(it) }
          .filter { isEntityActive(it) }
          .sortedByDescending { getEntityLastUpdate(it) }
        LoadState.Loaded(combined)
      }
      is LoadState.Error -> {
        if (activeDownloadedItems.isNotEmpty()) {
          val sorted = activeDownloadedItems.sortedByDescending { getEntityLastUpdate(it) }
          LoadState.Loaded(sorted)
        } else {
          state
        }
      }
      else -> state
    }

    if (updatedEntitiesState is LoadState.Loaded<List<ShelfEntity>> && updatedEntitiesState.data.isEmpty()) {
      return shelves.toMutableList().apply { removeAt(continueListeningIndex) }
    }

    val updatedShelf = UiShelf<ShelfEntity>(
      id = shelf.id,
      label = shelf.label,
      total = when (updatedEntitiesState) {
        is LoadState.Loaded<List<ShelfEntity>> -> updatedEntitiesState.data.size
        else -> shelf.total
      },
      entities = updatedEntitiesState,
    )

    return shelves.toMutableList().apply {
      set(continueListeningIndex, updatedShelf)
    }
  } else if (activeDownloadedItems.isNotEmpty()) {
    val sortedDownloads = activeDownloadedItems.sortedByDescending { getEntityLastUpdate(it) }
    val synthesizedShelf = UiShelf<ShelfEntity>(
      id = ShelfIds.ContinueListening,
      label = "Continue Listening",
      total = sortedDownloads.size,
      entities = LoadState.Loaded(sortedDownloads),
    )
    return listOf(synthesizedShelf) + shelves
  }

  return shelves
}

private fun enhanceListenAgainShelf(
  shelves: List<UiShelf<ShelfEntity>>,
  completedDownloads: List<LibraryItem>,
  userMediaProgress: Map<MediaProgressKey, app.campfire.core.model.MediaProgress>,
): List<UiShelf<ShelfEntity>> {
  fun entityKey(entity: ShelfEntity): String {
    return when (entity) {
      is LibraryItem -> entity.id
      is ShelfEntity.EpisodeShelfEntry -> "${entity.libraryItem.id}_${entity.recentEpisode.id}"
      is ShelfEntity.UpcomingBookShelfEntry -> entity.id
      is Author -> entity.id
      is Series -> entity.id
    }
  }

  fun getEntityLastUpdate(entity: ShelfEntity): Long {
    val prog = findUserMediaProgress(entity, userMediaProgress)
    return prog?.lastUpdate ?: 0L
  }

  val finishedDownloadedItems = completedDownloads.filter { item ->
    val prog = findUserMediaProgress(item, userMediaProgress)
    prog != null && isMediaProgressCompleted(prog)
  }

  val listenAgainIndex = shelves.indexOfFirst {
    it.id.startsWith(ShelfIds.ListenAgain)
  }

  if (listenAgainIndex >= 0) {
    val shelf = shelves[listenAgainIndex]
    val updatedEntitiesState: LoadState<List<ShelfEntity>> = when (val state = shelf.entities) {
      is LoadState.Loaded<List<ShelfEntity>> -> {
        val currentEntities = state.data
        val combined = (finishedDownloadedItems + currentEntities)
          .distinctBy { entityKey(it) }
          .sortedByDescending { getEntityLastUpdate(it) }
        LoadState.Loaded(combined)
      }
      is LoadState.Error -> {
        if (finishedDownloadedItems.isNotEmpty()) {
          val sorted = finishedDownloadedItems.sortedByDescending { getEntityLastUpdate(it) }
          LoadState.Loaded(sorted)
        } else {
          state
        }
      }
      else -> state
    }

    val updatedShelf = UiShelf<ShelfEntity>(
      id = shelf.id,
      label = shelf.label,
      total = when (updatedEntitiesState) {
        is LoadState.Loaded<List<ShelfEntity>> -> updatedEntitiesState.data.size
        else -> shelf.total
      },
      entities = updatedEntitiesState,
    )

    return shelves.toMutableList().apply {
      set(listenAgainIndex, updatedShelf)
    }
  } else if (finishedDownloadedItems.isNotEmpty()) {
    val sortedFinished = finishedDownloadedItems.sortedByDescending { getEntityLastUpdate(it) }
    val synthesizedShelf = UiShelf<ShelfEntity>(
      id = ShelfIds.ListenAgain,
      label = "Listen Again",
      total = sortedFinished.size,
      entities = LoadState.Loaded(sortedFinished),
    )

    val continueListeningIndex = shelves.indexOfFirst {
      it.id.startsWith(ShelfIds.ContinueListening)
    }

    return if (continueListeningIndex >= 0) {
      shelves.toMutableList().apply { add(continueListeningIndex + 1, synthesizedShelf) }
    } else {
      listOf(synthesizedShelf) + shelves
    }
  }

  return shelves
}
