// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.common.compose.tracing.TraceEffect
import app.campfire.common.compose.widgets.CampfireLoadingIndicator
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.randomEmptyMessage
import app.campfire.common.screens.HomeScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Series
import app.campfire.core.model.ShelfEntity
import app.campfire.core.offline.OfflineStatus
import app.campfire.home.api.FeedResponse
import app.campfire.home.ui.composables.ShelfListItem
import app.campfire.ui.appbar.CampfireAppBar
import app.campfire.ui.navigation.bar.AttachScrollBehaviorToLocalNavigationBar
import app.campfire.user.api.MediaProgressKey
import app.campfire.updates.AppUpdateWidget
import campfire.features.home.ui.generated.resources.Res
import campfire.features.home.ui.generated.resources.home_feed_load_error
import com.slack.circuit.codegen.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(HomeScreen::class, UserScope::class)
@Composable
fun HomeScreen(
  state: HomeUiState,
  campfireAppbar: CampfireAppBar,
  appUpdateWidget: AppUpdateWidget = object : AppUpdateWidget {
    @Composable
    override fun Content(modifier: Modifier) {}
  },
  modifier: Modifier = Modifier,
) {
  val appBarBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  AttachScrollBehaviorToLocalNavigationBar(appBarBehavior)

  TraceEffect("Home")

  Scaffold(
    topBar = {
      // Injected appbar that injects its own presenter to consistently load its state
      // across multiple services.
      campfireAppbar(
        Modifier,
        appBarBehavior,
      )
    },
    modifier = modifier.nestedScroll(appBarBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
      isRefreshing = state.isRefreshing,
      onRefresh = { state.eventSink(HomeUiEvent.Refresh) },
      state = pullToRefreshState,
      modifier = Modifier.fillMaxSize(),
      indicator = {
        CampfireLoadingIndicator(
          state = pullToRefreshState,
          isRefreshing = state.isRefreshing,
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = paddingValues.calculateTopPadding()),
        )
      },
    ) {
      when (val feed = state.homeFeed) {
        FeedResponse.Loading -> LoadingListState(Modifier.padding(paddingValues))
        is FeedResponse.Error -> {
          val reason = when (feed) {
            is FeedResponse.Error.Exception ->
              feed.error.message
                ?: feed.error::class.simpleName
                ?: "<Unknown error>"

            is FeedResponse.Error.Message -> feed.message
          }
          ErrorListState(
            stringResource(Res.string.home_feed_load_error, reason),
            modifier = Modifier.padding(paddingValues),
          )
        }

        is FeedResponse.Success -> if (state.homeFeed.data.isEmpty()) {
          EmptyState(randomEmptyMessage())
        } else {
          LoadedState(
            shelves = state.homeFeed.data,
            appUpdateWidget = appUpdateWidget,
            showBatteryWarning = state.showBatteryWarning,
            onFixBatteryClick = { state.eventSink(HomeUiEvent.RequestIgnoreBatteryOptimizations) },
            onDismissBatteryClick = { state.eventSink(HomeUiEvent.DismissBatteryWarning) },
            offlineStatus = { libraryItemId ->
              state.offlineStates[libraryItemId].asWidgetStatus()
            },
            progressStatus = { libraryItemId, podcastEpisodeId ->
              state.progressStates[MediaProgressKey(libraryItemId, podcastEpisodeId)]
            },
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize(),
            onViewAllUpcomingClick = { state.eventSink(HomeUiEvent.OpenUpcomingScreen) },
            onHeaderClick = { shelfId -> state.eventSink(HomeUiEvent.OpenShelf(shelfId)) },
            onRefreshDiscoveriesClick = { state.eventSink(HomeUiEvent.RefreshDiscoveries) },
            isRefreshingDiscoveries = state.isRefreshingDiscoveries,
            onItemClick = { shelf, item ->
              when (item) {
                is LibraryItem -> state.eventSink(
                  HomeUiEvent.OpenLibraryItem(item, item.id + shelf.id),
                )

                is Author -> state.eventSink(HomeUiEvent.OpenAuthor(item))
                is Series -> state.eventSink(HomeUiEvent.OpenSeries(item))

                is ShelfEntity.UpcomingBookShelfEntry -> item.providerUrl?.let { url ->
                  state.eventSink(HomeUiEvent.OpenUpcomingBook(url))
                }

                is ShelfEntity.EpisodeShelfEntry -> state.eventSink(
                  HomeUiEvent.OpenLibraryItemWithEpisode(
                    item = item.libraryItem,
                    episodeId = item.recentEpisode.id,
                    sharedTransitionKey = item.transitionKey + shelf.id,
                  ),
                )
                else -> Unit
              }
            },
          )
        }
      }
    }
  }
}

@Composable
private fun LoadedState(
  shelves: List<UiShelf<ShelfEntity>>,
  appUpdateWidget: AppUpdateWidget,
  showBatteryWarning: Boolean,
  onFixBatteryClick: () -> Unit,
  onDismissBatteryClick: () -> Unit,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  progressStatus: (LibraryItemId, PodcastEpisodeId?) -> MediaProgress?,
  onItemClick: (UiShelf<*>, Any) -> Unit,
  onViewAllUpcomingClick: () -> Unit,
  onHeaderClick: (String) -> Unit,
  onRefreshDiscoveriesClick: () -> Unit,
  isRefreshingDiscoveries: Boolean,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyListState = rememberLazyListState(),
) {
  LazyColumn(
    state = state,
    modifier = modifier,
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (showBatteryWarning) {
      item(key = "battery_optimization_warning") {
        ElevatedCard(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
          ),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = CampfireIcons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
              )
              Text(
                text = "Background Playback Limited",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
              )
              IconButton(
                onClick = onDismissBatteryClick,
                modifier = Modifier.size(24.dp),
              ) {
                Icon(
                  imageVector = CampfireIcons.Rounded.Close,
                  contentDescription = "Dismiss",
                  modifier = Modifier.size(16.dp),
                )
              }
            }
            Text(
              text = "Battery usage is set to 'Optimized'. When your screen is locked, Samsung / Android power saving may pause playback after 10 seconds or freeze downloads.",
              style = MaterialTheme.typography.bodyMedium,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              TextButton(onClick = onDismissBatteryClick) {
                Text("Dismiss")
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = onFixBatteryClick,
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError,
                ),
              ) {
                Text("Fix Now (1-Tap)")
              }
            }
          }
        }
      }
    }

    item(key = "app_update_banner") {
      appUpdateWidget.Content(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
      )
    }

    items(
      items = shelves,
      key = { it.id },
    ) { shelf ->
      ShelfListItem(
        shelf = shelf,
        onItemClick = { onItemClick(shelf, it) },
        onViewAllUpcomingClick = onViewAllUpcomingClick,
        onHeaderClick = onHeaderClick,
        onRefreshDiscoveriesClick = onRefreshDiscoveriesClick,
        isRefreshingDiscoveries = isRefreshingDiscoveries,
        offlineStatus = offlineStatus,
        progressStatus = progressStatus,
      )
    }
  }
}
