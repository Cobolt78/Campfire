// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.animated.AnimatedTimerPainter
import app.campfire.common.compose.icons.rounded.Add
import app.campfire.common.compose.icons.rounded.Remove
import campfire.infra.audioplayer.public_ui.generated.resources.Res
import campfire.infra.audioplayer.public_ui.generated.resources.action_clear_timer
import campfire.infra.audioplayer.public_ui.generated.resources.label_current_timer
import campfire.infra.audioplayer.public_ui.generated.resources.timer_adjust_decrease_1m
import campfire.infra.audioplayer.public_ui.generated.resources.timer_adjust_decrease_5m
import campfire.infra.audioplayer.public_ui.generated.resources.timer_adjust_increase_1m
import campfire.infra.audioplayer.public_ui.generated.resources.timer_adjust_increase_5m
import campfire.infra.audioplayer.public_ui.generated.resources.timer_step_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun RunningTimerCard(
  runningTimer: RunningTimer,
  modifier: Modifier = Modifier,
  onTimerCleared: (() -> Unit)? = null,
  onAdjustTimer: ((deltaMillis: Long) -> Unit)? = null,
  useAlternateStepperView: Boolean = false,
  fineStepEnabled: Boolean = false,
  onFineStepEnabledChange: ((Boolean) -> Unit)? = null,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 16.dp, horizontal = 16.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    tonalElevation = 4.dp,
    shadowElevation = 1.dp,
  ) {
    Column {
      // Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp),
      ) {
        Icon(
          AnimatedTimerPainter,
          contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = stringResource(Res.string.label_current_timer),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }

      RunningTimerText(
        runningTimer = runningTimer,
        style = { timer ->
          when (timer) {
            is PlaybackTimer.EndOfChapter -> MaterialTheme.typography.titleLarge
            is PlaybackTimer.Epoch -> MaterialTheme.typography.displayMedium
          }
        },
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )

      if (runningTimer.timer is PlaybackTimer.Epoch && onAdjustTimer != null) {
        Spacer(Modifier.height(16.dp))
        val dec5Label = stringResource(Res.string.timer_adjust_decrease_5m)
        val dec1Label = stringResource(Res.string.timer_adjust_decrease_1m)
        val inc1Label = stringResource(Res.string.timer_adjust_increase_1m)
        val inc5Label = stringResource(Res.string.timer_adjust_increase_5m)

        if (useAlternateStepperView) {
          val deltaMinutes = if (fineStepEnabled) 1L else 5L
          val activeDecLabel = if (fineStepEnabled) dec1Label else dec5Label
          val activeIncLabel = if (fineStepEnabled) inc1Label else inc5Label

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FilledTonalIconButton(
                onClick = { onAdjustTimer(-deltaMinutes * 60_000L) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                  contentColor = MaterialTheme.colorScheme.onSurface,
                ),
              ) {
                Icon(
                  imageVector = CampfireIcons.Rounded.Remove,
                  contentDescription = activeDecLabel,
                )
              }

              Text(
                text = "±${deltaMinutes}m",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )

              FilledTonalIconButton(
                onClick = { onAdjustTimer(deltaMinutes * 60_000L) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                  contentColor = MaterialTheme.colorScheme.onSurface,
                ),
              ) {
                Icon(
                  imageVector = CampfireIcons.Rounded.Add,
                  contentDescription = activeIncLabel,
                )
              }
            }

            if (onFineStepEnabledChange != null) {
              Spacer(Modifier.height(10.dp))
              TimerStepTogglePill(
                fineStepEnabled = fineStepEnabled,
                onFineStepEnabledChange = onFineStepEnabledChange,
              )
            }
          }
        } else {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            DualStageTimerCapsule(
              leftLabel = "−5m",
              leftDescription = dec5Label,
              onLeftClick = { onAdjustTimer(-5 * 60_000L) },
              rightLabel = "−1m",
              rightDescription = dec1Label,
              onRightClick = { onAdjustTimer(-1 * 60_000L) },
              modifier = Modifier.weight(1f),
            )
            DualStageTimerCapsule(
              leftLabel = "+1m",
              leftDescription = inc1Label,
              onLeftClick = { onAdjustTimer(1 * 60_000L) },
              rightLabel = "+5m",
              rightDescription = inc5Label,
              onRightClick = { onAdjustTimer(5 * 60_000L) },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      if (onTimerCleared != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onTimerCleared,
          ) {
            Text(stringResource(Res.string.action_clear_timer))
          }
        }
      } else {
        Spacer(Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun DualStageTimerCapsule(
  leftLabel: String,
  leftDescription: String,
  onLeftClick: () -> Unit,
  rightLabel: String,
  rightDescription: String,
  onRightClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(50),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier.height(40.dp),
  ) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TimerCapsuleHalfButton(
          label = leftLabel,
          description = leftDescription,
          onClick = onLeftClick,
        )
        Box(
          modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
        )
        TimerCapsuleHalfButton(
          label = rightLabel,
          description = rightDescription,
          onClick = onRightClick,
        )
      }
    }
  }
}

@Composable
private fun RowScope.TimerCapsuleHalfButton(
  label: String,
  description: String,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .weight(1f)
      .fillMaxHeight()
      .clickable(onClick = onClick)
      .semantics {
        role = Role.Button
        contentDescription = description
      },
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      softWrap = false,
    )
  }
}

@Composable
private fun TimerStepTogglePill(
  fineStepEnabled: Boolean,
  onFineStepEnabledChange: (Boolean) -> Unit,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = "${stringResource(Res.string.timer_step_label)}:",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    Surface(
      shape = RoundedCornerShape(50),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Row(
        modifier = Modifier.padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        TimerStepChip(
          label = "5m",
          isSelected = !fineStepEnabled,
          onClick = { onFineStepEnabledChange(false) },
        )
        TimerStepChip(
          label = "1m",
          isSelected = fineStepEnabled,
          onClick = { onFineStepEnabledChange(true) },
        )
      }
    }
  }
}

@Composable
private fun TimerStepChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.surfaceContainerHigh
  }
  val contentColor = if (isSelected) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
  }
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(50))
      .background(containerColor)
      .clickable(onClick = onClick)
      .semantics {
        role = Role.RadioButton
        selected = isSelected
      }
      .padding(horizontal = 12.dp, vertical = 5.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = contentColor,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      softWrap = false,
    )
  }
}
