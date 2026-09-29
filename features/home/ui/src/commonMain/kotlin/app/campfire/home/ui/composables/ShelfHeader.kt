// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ChevronRight
import app.campfire.home.api.model.ShelfIds
import app.campfire.home.ui.UiShelf
import campfire.features.home.ui.generated.resources.Res
import campfire.features.home.ui.generated.resources.upcoming_shelf_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShelfHeader(
  shelf: UiShelf<*>,
  modifier: Modifier = Modifier,
  onHeaderClick: (() -> Unit)? = null,
) {
  val title = when (shelf.id) {
    ShelfIds.UpcomingReleases -> stringResource(Res.string.upcoming_shelf_title)
    "downloads" -> "Downloads"
    else -> shelf.label
  }

  Box(
    modifier = modifier
      .height(48.dp)
      .padding(horizontal = 8.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    if (onHeaderClick != null) {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(
            role = Role.Button,
            onClick = onHeaderClick,
          )
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = CampfireIcons.Rounded.ChevronRight,
          contentDescription = "View all",
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 8.dp),
      )
    }
  }
}
