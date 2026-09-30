// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.continueseries

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.OverlappedNavigationBarInsets
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ChevronRight
import app.campfire.common.compose.icons.rounded.SortAsc
import app.campfire.common.compose.icons.rounded.SortDesc
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.common.compose.widgets.adaptiveExitUntilCollapsedScrollBehavior
import app.campfire.common.screens.ContinueSeriesScreen
import app.campfire.core.di.UserScope
import com.r0adkll.kimchi.circuit.annotations.CircuitInject

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(ContinueSeriesScreen::class, UserScope::class)
@Composable
fun ContinueSeries(
  state: ContinueSeriesUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = adaptiveExitUntilCollapsedScrollBehavior()
  var showSortMenu by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text("Continue Series") },
        navigationIcon = {
          NavigationBackButton(onClick = { state.eventSink(ContinueSeriesUiEvent.Back) })
        },
        actions = {
          Box {
            IconButton(onClick = { showSortMenu = true }) {
              Icon(
                imageVector = if (state.sortAscending) CampfireIcons.Rounded.SortAsc else CampfireIcons.Rounded.SortDesc,
                contentDescription = "Sort options",
              )
            }
            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false },
            ) {
              ContinueSeriesSort.entries.forEach { sort ->
                DropdownMenuItem(
                  text = {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Text(
                        text = sort.label,
                        fontWeight = if (state.sortMode == sort) FontWeight.Bold else FontWeight.Normal,
                      )
                      if (state.sortMode == sort) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                          imageVector = if (state.sortAscending) CampfireIcons.Rounded.SortAsc else CampfireIcons.Rounded.SortDesc,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                          tint = MaterialTheme.colorScheme.primary,
                        )
                      }
                    }
                  },
                  onClick = {
                    state.eventSink(ContinueSeriesUiEvent.SelectSort(sort))
                    showSortMenu = false
                  },
                )
              }
            }
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets.exclude(OverlappedNavigationBarInsets),
  ) { paddingValues ->
    when {
      state.isLoading -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }

      state.items.isEmpty() -> {
        EmptyState(
          message = "No series currently in progress",
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        )
      }

      else -> {
        if (state.alternateView) {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
              top = paddingValues.calculateTopPadding() + 8.dp,
              bottom = paddingValues.calculateBottomPadding() + 16.dp,
              start = 16.dp,
              end = 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(
              items = state.items,
              key = { it.series.id },
            ) { item ->
              ContinueSeriesGridCard(
                item = item,
                onBookClick = { state.eventSink(ContinueSeriesUiEvent.OpenBook(it)) },
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
              top = paddingValues.calculateTopPadding() + 8.dp,
              bottom = paddingValues.calculateBottomPadding() + 16.dp,
              start = 16.dp,
              end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(
              items = state.items,
              key = { it.series.id },
            ) { item ->
              ContinueSeriesCard(
                item = item,
                onSeriesClick = { state.eventSink(ContinueSeriesUiEvent.OpenSeries(it)) },
                onBookClick = { state.eventSink(ContinueSeriesUiEvent.OpenBook(it)) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ContinueSeriesCard(
  item: ContinueSeriesItem,
  onSeriesClick: (app.campfire.core.model.Series) -> Unit,
  onBookClick: (app.campfire.core.model.LibraryItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
    ) {
      // Series Header: Name, completed books count, and Chevron button to view whole series
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .clickable { onSeriesClick(item.series) }
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.series.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${item.completedBooks} of ${item.totalBooks} books completed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Icon(
          imageVector = CampfireIcons.Rounded.ChevronRight,
          contentDescription = "View series details",
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Series Progress Bar
      val seriesProgress = (item.completedBooks.toFloat() / item.totalBooks.toFloat()).coerceIn(0f, 1f)
      LinearProgressIndicator(
        progress = { seriesProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Next Up Book Surface
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable { onBookClick(item.nextUpBook) },
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(12.dp),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Book Cover (enlarged left poster)
          Box(
            modifier = Modifier
              .size(width = 80.dp, height = 108.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
          ) {
            item.nextUpBook.media.coverImageUrl.takeIf { it.isNotBlank() }?.let { coverUrl ->
              ItemImage(
                imageUrl = coverUrl,
                contentDescription = item.nextUpBook.media.metadata.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            }
          }

          Spacer(modifier = Modifier.width(14.dp))

          // Next Up Book Details
          Column(
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 108.dp),
            verticalArrangement = Arrangement.Center,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp),
              ) {
                Text(
                  text = "NEXT UP",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
              item.nextUpBook.media.metadata.seriesSequence?.let { seq ->
                if (seq.formattedSequence.isNotBlank()) {
                  Text(
                    text = "Book ${seq.formattedSequence}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = item.nextUpBook.media.metadata.title ?: "Untitled",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )

            item.nextUpBook.media.metadata.authorName?.let { author ->
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ContinueSeriesGridCard(
  item: ContinueSeriesItem,
  onBookClick: (app.campfire.core.model.LibraryItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  app.campfire.common.compose.widgets.ElevatedContentCard(
    modifier = modifier,
    onClick = { onBookClick(item.nextUpBook) },
  ) {
    Column {
      Box(modifier = Modifier.aspectRatio(1f).fillMaxWidth()) {
        ItemImage(
          imageUrl = item.nextUpBook.media.coverImageUrl,
          contentDescription = item.nextUpBook.media.metadata.title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      }
      
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = item.nextUpBook.media.metadata.title ?: "Untitled",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          modifier = Modifier.basicMarquee(),
        )
        
        Spacer(modifier = Modifier.height(2.dp))

        val sequence = item.nextUpBook.media.metadata.seriesSequence?.formattedSequence
        val subtitle = buildString {
          if (!sequence.isNullOrBlank()) {
            append("Book $sequence • ")
          }
          append(item.series.name)
        }
        
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 1,
          modifier = Modifier.basicMarquee(),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

