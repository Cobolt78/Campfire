package app.campfire.widgets.callbacks

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import app.campfire.widgets.di.AudioPlayerActionCallback

class SleepTimerActionCallback : AudioPlayerActionCallback() {

  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    if (audioPlayer == null) return
    val minutes = parameters[KEY_MINUTES]
      ?: component.sleepSettings.lastSetSleepTimer.inWholeMinutes.toInt()
    commandSender.setSleepTimer(minutes)
  }

  companion object {
    val KEY_MINUTES = ActionParameters.Key<Int>("minutes")
  }
}
