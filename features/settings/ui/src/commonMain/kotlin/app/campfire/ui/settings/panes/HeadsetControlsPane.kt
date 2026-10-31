// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ArrowDropDown
import app.campfire.common.compose.icons.rounded.BookmarkAdd
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.FastForward
import app.campfire.common.compose.icons.rounded.FastRewind
import app.campfire.common.compose.icons.rounded.SkipNext
import app.campfire.common.compose.icons.rounded.SkipPrevious
import app.campfire.settings.api.RemoteControlAction
import app.campfire.ui.settings.SettingsUiEvent.HeadsetControlsSettingEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.Header

@Composable
internal fun HeadsetControlsPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val settings = state.headsetControlsSettings
  val eventSink = state.eventSink

  SettingPaneLayout(
    title = { Text("Headset & Remote Controls") },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    Header(
      title = { Text("Wired Headset (Single Button)") },
    )

    Text(
      text = "Customize actions for earphones with a single inline physical button. Single-press is reserved for Play/Pause.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 16.dp),
    )

    Spacer(Modifier.height(12.dp))

    RemoteControlActionDropdownSetting(
      headline = "Double-Press",
      subtitle = "Action triggered on two quick presses",
      value = settings.singleButtonDoublePressAction,
      onValueChange = { eventSink(HeadsetControlsSettingEvent.SingleButtonDoublePress(it)) },
    )

    HorizontalDivider(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )

    RemoteControlActionDropdownSetting(
      headline = "Triple-Press",
      subtitle = "Action triggered on three quick presses",
      value = settings.singleButtonTriplePressAction,
      onValueChange = { eventSink(HeadsetControlsSettingEvent.SingleButtonTriplePress(it)) },
    )

    HorizontalDivider(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )

    RemoteControlActionDropdownSetting(
      headline = "Quadruple-Press",
      subtitle = "Action triggered on four quick presses",
      value = settings.singleButtonQuadruplePressAction,
      onValueChange = { eventSink(HeadsetControlsSettingEvent.SingleButtonQuadruplePress(it)) },
    )

    Spacer(Modifier.height(16.dp))

    Header(
      title = { Text("Bluetooth & Dedicated Buttons") },
    )

    Text(
      text = "For car stereos, Bluetooth headphones, and multi-button remotes. If your Bluetooth earbuds support touch gestures, tap commands configured in your headphone companion app will trigger these actions.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 16.dp),
    )

    Spacer(Modifier.height(12.dp))

    RemoteControlActionDropdownSetting(
      headline = "\"Next Track\" Command",
      subtitle = "Action when a Next signal is received (e.g. double-tapping most Bluetooth earbuds or pressing Next on car audio)",
      value = settings.nextButtonAction,
      onValueChange = { eventSink(HeadsetControlsSettingEvent.NextButton(it)) },
    )

    HorizontalDivider(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )

    RemoteControlActionDropdownSetting(
      headline = "\"Previous Track\" Command",
      subtitle = "Action when a Previous signal is received (e.g. triple-tapping most Bluetooth earbuds or pressing Previous on car audio)",
      value = settings.previousButtonAction,
      onValueChange = { eventSink(HeadsetControlsSettingEvent.PreviousButton(it)) },
    )

    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun RemoteControlActionDropdownSetting(
  headline: String,
  subtitle: String,
  value: RemoteControlAction,
  onValueChange: (RemoteControlAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isExpanded by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    Text(
      text = headline,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(Modifier.height(4.dp))

    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(10.dp))

    Box(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(12.dp),
          )
          .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(12.dp),
          )
          .clip(RoundedCornerShape(12.dp))
          .clickable { isExpanded = true }
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val actionIcon = when (value) {
          RemoteControlAction.SKIP_FORWARD_TIME -> CampfireIcons.Rounded.FastForward
          RemoteControlAction.SKIP_BACKWARD_TIME -> CampfireIcons.Rounded.FastRewind
          RemoteControlAction.NEXT_CHAPTER -> CampfireIcons.Rounded.SkipNext
          RemoteControlAction.PREVIOUS_CHAPTER -> CampfireIcons.Rounded.SkipPrevious
          RemoteControlAction.ADD_BOOKMARK -> CampfireIcons.Rounded.BookmarkAdd
          RemoteControlAction.NONE -> CampfireIcons.Rounded.Close
        }
        Icon(
          imageVector = actionIcon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
          text = value.label,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        Icon(
          imageVector = CampfireIcons.Rounded.ArrowDropDown,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { isExpanded = false },
      ) {
        RemoteControlAction.entries.forEach { option ->
          val optionIcon = when (option) {
            RemoteControlAction.SKIP_FORWARD_TIME -> CampfireIcons.Rounded.FastForward
            RemoteControlAction.SKIP_BACKWARD_TIME -> CampfireIcons.Rounded.FastRewind
            RemoteControlAction.NEXT_CHAPTER -> CampfireIcons.Rounded.SkipNext
            RemoteControlAction.PREVIOUS_CHAPTER -> CampfireIcons.Rounded.SkipPrevious
            RemoteControlAction.ADD_BOOKMARK -> CampfireIcons.Rounded.BookmarkAdd
            RemoteControlAction.NONE -> CampfireIcons.Rounded.Close
          }
          DropdownMenuItem(
            leadingIcon = {
              Icon(
                imageVector = optionIcon,
                contentDescription = null,
                tint = if (option == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            text = {
              Text(
                text = option.label,
                fontWeight = if (option == value) FontWeight.SemiBold else FontWeight.Normal,
                color = if (option == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
              )
            },
            onClick = {
              onValueChange(option)
              isExpanded = false
            },
          )
        }
      }
    }
  }
}
