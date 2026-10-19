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
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.animated.AnimatedTimerPainter
import app.campfire.common.compose.icons.rounded.Timer
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_sleep_timer
import kotlin.time.Duration
import org.jetbrains.compose.resources.stringResource

@Composable
fun RunningTimerAction(
  runningTimer: RunningTimer?,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val label = stringResource(Res.string.action_sleep_timer)
  IconButtonTooltip(
    text = label,
    modifier = modifier,
  ) {
    IconButton(
      onClick = onClick,
    ) {
      if (runningTimer?.timer !is PlaybackTimer.EndOfChapter) {
        if (runningTimer != null) {
          Icon(
            AnimatedTimerPainter,
            contentDescription = label,
          )
        } else {
          Icon(
            CampfireIcons.Rounded.Timer,
            contentDescription = label,
          )
        }
      } else {
        val remaining = (currentDuration - currentTime).div(playbackSpeed.toDouble())
        Text(
          text = remaining.thresholdReadoutFormat(),
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
