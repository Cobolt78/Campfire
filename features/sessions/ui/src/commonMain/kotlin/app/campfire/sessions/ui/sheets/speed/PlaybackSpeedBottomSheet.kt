// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.speed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.Changed
import app.campfire.analytics.events.PlaybackActionEvent
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.analytics.events.Speed
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Add
import app.campfire.common.compose.icons.rounded.Book
import app.campfire.common.compose.icons.rounded.Globe
import app.campfire.common.compose.icons.rounded.Remove
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.PlaybackSpeedDialog
import app.campfire.common.compose.widgets.PlaybackSpeedRange
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.extensions.readableHundredths
import app.campfire.core.extensions.roundToHundredths
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import app.campfire.settings.api.PlaybackSettings
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.session_scope_global
import campfire.features.sessions.ui.generated.resources.session_scope_per_book
import campfire.features.sessions.ui.generated.resources.speed_bottomsheet_title
import campfire.features.sessions.ui.generated.resources.speed_custom_open
import campfire.features.sessions.ui.generated.resources.speed_decrease
import campfire.features.sessions.ui.generated.resources.speed_increase
import com.slack.circuit.overlay.OverlayHost
import dev.zacsweers.metro.ContributesTo
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.jetbrains.compose.resources.stringResource

data class PlaybackSpeedInput(
  val itemId: LibraryItemId,
  val speed: Float,
)

suspend fun OverlayHost.showPlaybackSpeedBottomSheet(
  itemId: LibraryItemId,
  speed: Float,
) {
  show(
    AdaptiveSheetOverlay(
      model = PlaybackSpeedInput(itemId, speed),
      onDismiss = { },
      skipPartiallyExpanded = true,
    ) { input, _ ->
      Impression {
        ScreenViewEvent("PlaybackSpeed", ScreenType.Overlay)
      }

      PlaybackSpeedBottomSheet(
        input = input,
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

@ContributesTo(AppScope::class)
interface PlaybackSpeedBottomSheetComponent {
  val playbackSettings: PlaybackSettings
  val audioPlayerHolder: AudioPlayerHolder
}

@Composable
private fun rememberPlaybackSpeedComponent(): PlaybackSpeedBottomSheetComponent {
  return remember {
    ComponentHolder.component<PlaybackSpeedBottomSheetComponent>()
  }
}

/**
 * State/DI layer for the playback speed sheet: resolves the component, collects the player and
 * settings flows, and handles events so [PlaybackSpeedSheet] stays pure and previewable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun PlaybackSpeedBottomSheet(
  input: PlaybackSpeedInput,
  modifier: Modifier = Modifier,
  component: PlaybackSpeedBottomSheetComponent = rememberPlaybackSpeedComponent(),
) {
  val currentSpeed by remember {
    component.audioPlayerHolder.currentPlayer
      .flatMapLatest {
        it?.playbackSpeed ?: emptyFlow()
      }
  }.collectAsState(input.speed)

  val speedOptions = remember { component.playbackSettings.playbackRates.distinct().sorted() }

  val itemPlaybackSpeeds by remember {
    component.playbackSettings.observeItemPlaybackSpeeds()
  }.collectAsState()

  PlaybackSpeedSheet(
    currentSpeed = currentSpeed,
    speedOptions = speedOptions,
    perBookSpeedEnabled = input.itemId in itemPlaybackSpeeds,
    onSpeedChange = { speed ->
      Analytics.send(PlaybackActionEvent(Speed, Changed, extras = mapOf("speed" to speed)))
      component.audioPlayerHolder.currentPlayer.value
        ?.setPlaybackSpeed(speed)
    },
    onPerBookSpeedChange = { enabled ->
      Analytics.send(PlaybackActionEvent(Speed, Changed, extras = mapOf("perBook" to enabled)))
      if (enabled) {
        component.playbackSettings.itemPlaybackSpeeds += (input.itemId to currentSpeed)
      } else {
        component.playbackSettings.itemPlaybackSpeeds -= input.itemId
        // Snap active playback back to the global speed the item now falls back to
        component.audioPlayerHolder.currentPlayer.value
          ?.setPlaybackSpeed(component.playbackSettings.playbackSpeed)
      }
    },
    modifier = modifier,
  )
}

@Composable
internal fun PlaybackSpeedSheet(
  currentSpeed: Float,
  speedOptions: List<Float>,
  perBookSpeedEnabled: Boolean,
  onSpeedChange: (Float) -> Unit,
  onPerBookSpeedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.speed_bottomsheet_title)) },
  ) {
    Spacer(Modifier.height(8.dp))

    SingleChoiceSegmentedButtonRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      SegmentedButton(
        selected = !perBookSpeedEnabled,
        onClick = { onPerBookSpeedChange(false) },
        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        icon = {
          Icon(
            imageVector = CampfireIcons.Rounded.Globe,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        },
        label = { Text(stringResource(Res.string.session_scope_global)) },
      )
      SegmentedButton(
        selected = perBookSpeedEnabled,
        onClick = { onPerBookSpeedChange(true) },
        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        icon = {
          Icon(
            imageVector = CampfireIcons.Rounded.Book,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        },
        label = { Text(stringResource(Res.string.session_scope_per_book)) },
      )
    }

    Spacer(Modifier.height(16.dp))

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged

    var sliderValue by remember { mutableStateOf(currentSpeed) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(currentSpeed) {
      if (!isInteracting && sliderValue != currentSpeed) {
        sliderValue = currentSpeed
      }
    }

    val commitSpeed: (Float) -> Unit = { newSpeed ->
      val clamped = newSpeed.roundToHundredths().coerceIn(PlaybackSpeedRange)
      sliderValue = clamped
      if (clamped != currentSpeed) {
        onSpeedChange(clamped)
      }
    }

    SingleChoiceSegmentedButtonRow(
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(horizontal = 16.dp),
    ) {
      speedOptions.forEachIndexed { index, defaultSpeed ->
        val isCurrentSpeed = currentSpeed == defaultSpeed
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index, speedOptions.size, RoundedCornerShape(16.dp)),
          selected = isCurrentSpeed,
          label = { Text("${defaultSpeed.readableHundredths}x") },
          onClick = { commitSpeed(defaultSpeed) },
        )
      }
    }

    Spacer(Modifier.height(16.dp))

    var showCustomSpeedDialog by remember { mutableStateOf(false) }
    val customSpeedLabel = stringResource(Res.string.speed_custom_open)
    val decreaseSpeedLabel = stringResource(Res.string.speed_decrease)
    val increaseSpeedLabel = stringResource(Res.string.speed_increase)

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButtonTooltip(text = decreaseSpeedLabel) {
        FilledTonalIconButton(
          enabled = sliderValue > PlaybackSpeedRange.start,
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            commitSpeed(stepSpeed(sliderValue, -1))
          },
        ) {
          Icon(
            imageVector = CampfireIcons.Rounded.Remove,
            contentDescription = decreaseSpeedLabel,
          )
        }
      }

      Spacer(Modifier.width(16.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClickLabel = customSpeedLabel) { showCustomSpeedDialog = true },
      ) {
        Text(
          text = "${sliderValue.readableHundredths}x",
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          modifier = Modifier
            .widthIn(min = 84.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }

      Spacer(Modifier.width(16.dp))

      IconButtonTooltip(text = increaseSpeedLabel) {
        FilledTonalIconButton(
          enabled = sliderValue < PlaybackSpeedRange.endInclusive,
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            commitSpeed(stepSpeed(sliderValue, +1))
          },
        ) {
          Icon(
            imageVector = CampfireIcons.Rounded.Add,
            contentDescription = increaseSpeedLabel,
          )
        }
      }
    }

    Spacer(Modifier.height(12.dp))

    val sliderProgressNormalized = ((sliderValue - PlaybackSpeedRange.start) / PlaybackSpeedRange.length)
      .coerceIn(0f, 1f)
    val waveLength = lerp(WavelengthRange.endInclusive, WavelengthRange.start, sliderProgressNormalized)
    val waveHeight = lerp(WaveHeightRange.start, WaveHeightRange.endInclusive, sliderProgressNormalized)
    val waveVelocity = lerp(WaveVelocityRange.start, WaveVelocityRange.endInclusive, sliderProgressNormalized)
    val waveThickness = lerp(WaveThicknessRange.start, WaveThicknessRange.endInclusive, sliderProgressNormalized)

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
    ) {
      WavySlider(
        value = sliderValue,
        valueRange = PlaybackSpeedRange,
        interactionSource = interactionSource,
        onValueChange = { raw ->
          val snapped = raw.snapToSpeedStep()
          if (snapped != sliderValue) {
            sliderValue = snapped
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
          }
        },
        onValueChangeFinished = {
          if (sliderValue != currentSpeed) {
            onSpeedChange(sliderValue)
          }
        },
        waveLength = waveLength,
        waveHeight = waveHeight,
        waveVelocity = waveVelocity to WaveDirection.TAIL,
        waveThickness = waveThickness,
        incremental = true,
        modifier = Modifier.fillMaxWidth(),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "${PlaybackSpeedRange.start.readableHundredths}x",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "${PlaybackSpeedRange.endInclusive.readableHundredths}x",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (showCustomSpeedDialog) {
      PlaybackSpeedDialog(
        initialSpeed = sliderValue,
        onDismiss = { showCustomSpeedDialog = false },
        onConfirm = { speed ->
          showCustomSpeedDialog = false
          commitSpeed(speed)
        },
      )
    }

    Spacer(Modifier.height(24.dp))
  }
}

@Preview
@Composable
private fun PlaybackSpeedSheetPreview() {
  CampfireTheme {
    var speed by remember { mutableStateOf(1.25f) }
    var perBook by remember { mutableStateOf(false) }

    ModalBottomSheetLayout(
      sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded),
      sheetContent = {
        PlaybackSpeedSheet(
          currentSpeed = speed,
          speedOptions = listOf(1f, 1.1f, 1.25f, 1.5f, 2f),
          perBookSpeedEnabled = perBook,
          onSpeedChange = { speed = it },
          onPerBookSpeedChange = { perBook = it },
        )
      },
    ) {
    }
  }
}

internal fun Float.snapToSpeedStep(): Float {
  val steppedHundredths = (this * 20f).roundToInt() * 5
  return (steppedHundredths / 100f).coerceIn(PlaybackSpeedRange)
}

internal fun stepSpeed(current: Float, stepsDelta: Int): Float {
  val currentSteps = if (stepsDelta > 0) {
    floor(current * 20f + 0.001f).toInt()
  } else {
    ceil(current * 20f - 0.001f).toInt()
  }
  val targetHundredths = (currentSteps + stepsDelta) * 5
  return (targetHundredths / 100f).coerceIn(PlaybackSpeedRange)
}

private val WavelengthRange = 40.dp.rangeTo(135.dp)
private val WaveHeightRange = 0.dp.rangeTo(40.dp)
private val WaveVelocityRange = 50.dp.rangeTo(120.dp)
private val WaveThicknessRange = 16.dp.rangeTo(6.dp)

private val ClosedFloatingPointRange<Float>.length: Float get() = endInclusive - start
