// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.BookmarkAdd
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.FastForward
import app.campfire.common.compose.icons.rounded.FastRewind
import app.campfire.common.compose.icons.rounded.SkipNext
import app.campfire.common.compose.icons.rounded.SkipPrevious
import app.campfire.settings.api.RemoteControlAction
import app.campfire.ui.settings.SettingsUiEvent.HeadsetControlsSettingEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.DropdownSetting
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

    Spacer(Modifier.height(8.dp))

    RemoteControlActionDropdownSetting(
      headline = "Double-Press",
      subtitle = "Action triggered on two quick presses",
      value = settings.singleButtonDoublePressAction,
      onValueChange = { eventSink(HeadsetControlsSettingEvent.SingleButtonDoublePress(it)) },
    )

    RemoteControlActionDropdownSetting(
      headline = "Triple-Press",
      subtitle = "Action triggered on three quick presses",
      value = settings.singleButtonTriplePressAction,
      onValueChange = { eventSink(HeadsetControlsSettingEvent.SingleButtonTriplePress(it)) },
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

    Spacer(Modifier.height(8.dp))

    RemoteControlActionDropdownSetting(
      headline = "\"Next Track\" Command",
      subtitle = "Action when a Next signal is received (e.g. double-tapping most Bluetooth earbuds or pressing Next on car audio)",
      value = settings.nextButtonAction,
      onValueChange = { eventSink(HeadsetControlsSettingEvent.NextButton(it)) },
    )

    RemoteControlActionDropdownSetting(
      headline = "\"Previous Track\" Command",
      subtitle = "Action when a Previous signal is received (e.g. triple-tapping most Bluetooth earbuds or pressing Previous on car audio)",
      value = settings.previousButtonAction,
      onValueChange = { eventSink(HeadsetControlsSettingEvent.PreviousButton(it)) },
    )
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
  DropdownSetting(
    value = value,
    values = RemoteControlAction.entries,
    onValueChange = onValueChange,
    headlineContent = { Text(headline) },
    supportingContent = { Text(subtitle) },
    itemIcon = { action ->
      Icon(
        imageVector = when (action) {
          RemoteControlAction.SKIP_FORWARD_TIME -> CampfireIcons.Rounded.FastForward
          RemoteControlAction.SKIP_BACKWARD_TIME -> CampfireIcons.Rounded.FastRewind
          RemoteControlAction.NEXT_CHAPTER -> CampfireIcons.Rounded.SkipNext
          RemoteControlAction.PREVIOUS_CHAPTER -> CampfireIcons.Rounded.SkipPrevious
          RemoteControlAction.ADD_BOOKMARK -> CampfireIcons.Rounded.BookmarkAdd
          RemoteControlAction.NONE -> CampfireIcons.Rounded.Close
        },
        contentDescription = null,
      )
    },
    itemText = { action ->
      Text(action.label)
    },
    modifier = modifier,
  )
}
