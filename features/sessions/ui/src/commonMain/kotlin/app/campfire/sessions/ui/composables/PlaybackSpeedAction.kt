// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.composables

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Speed
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.extensions.readableHundredths
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_playback_speed
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlaybackSpeedAction(
  playbackSpeed: Float,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val label = stringResource(Res.string.action_playback_speed)
  IconButtonTooltip(
    text = label,
    modifier = modifier,
  ) {
    IconButton(
      onClick = onClick,
    ) {
      if (playbackSpeed == 1f) {
        Icon(CampfireIcons.Rounded.Speed, contentDescription = label)
      } else {
        Text(
          text = "${playbackSpeed.readableHundredths}x",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Visible,
        )
      }
    }
  }
}
