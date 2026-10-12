// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.forwarding

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import app.campfire.settings.api.PlaybackSettings

/**
 * A [androidx.media3.common.ForwardingPlayer] that intercepts next/previous media item commands from external controllers
 * (e.g., car stereos with their own package) and redirects them based on the
 * [app.campfire.settings.api.PlaybackSettings.remoteNextPrevSkipsChapters] setting.
 *
 * When [remoteNextPrevSkipsChapters] is true (default), external next/prev commands skip chapters.
 * When false, they seek forward/backward by the configured time instead.
 *
 * Note: Bluetooth is handled separately via onMediaButtonEvent in MediaSessionCallback since Media3
 * routes Bluetooth key events through the app's own package. Media notification and Android Auto
 * always use chapter skip because they have dedicated custom seek buttons in their layout.
 */
@UnstableApi
class RemoteControlForwardingPlayer(
  player: Player,
  private val settings: PlaybackSettings,
  private val appPackageName: String,
) : ForwardingPlayer(player) {

  /**
   * Reference to the MediaSession, set after session creation.
   * Used to identify the source of commands via [androidx.media3.session.MediaSession.controllerForCurrentRequest].
   */
  var session: MediaSession? = null

  /**
   * Determines if the current command is from a remote controller.
   *
   * A command is considered "remote" if it comes from a different package (e.g., car stereo).
   *
   * Note: Bluetooth controllers are handled separately via onMediaButtonEvent in
   * MediaSessionCallback, since Media3 routes their events through the app's own package.
   * Media notification and Android Auto are NOT remote because they have custom seek buttons.
   */
  private fun isRemoteController(): Boolean {
    return session.isRemoteControllerRequest(appPackageName)
  }

  /**
   * Applies the setting-based behavior for remote controllers.
   * Returns true if we handled the command (seek forward/back), false if default behavior should be used.
   */
  private inline fun handleRemoteNextPrevCommand(seekAction: () -> Unit): Boolean {
    if (isRemoteController() && !settings.remoteNextPrevSkipsChapters) {
      seekAction()
      return true
    }
    return false
  }

  override fun seekToNextMediaItem() {
    if (!handleRemoteNextPrevCommand { seekForward() }) {
      super.seekToNextMediaItem()
    }
  }

  override fun seekToPreviousMediaItem() {
    if (!handleRemoteNextPrevCommand { seekBack() }) {
      super.seekToPreviousMediaItem()
    }
  }

  override fun seekToNext() {
    if (!handleRemoteNextPrevCommand { seekForward() }) {
      super.seekToNext()
    }
  }

  override fun seekToPrevious() {
    if (!handleRemoteNextPrevCommand { seekBack() }) {
      super.seekToPrevious()
    }
  }
}

@UnstableApi
internal val AUTO_CONTROLLER_PACKAGES = setOf(
  "com.google.android.projection.gearhead",
  "com.google.android.apps.automotive.media",
  "com.android.car.media",
  "com.google.android.carassistant",
)

@UnstableApi
internal fun MediaSession.isAutoController(controller: MediaSession.ControllerInfo): Boolean {
  return isAutoCompanionController(controller) || controller.packageName in AUTO_CONTROLLER_PACKAGES
}

/**
 * Whether the session command currently being dispatched came from a *remote* controller —
 * e.g., Bluetooth device, wired headset, car stereo, or external media button. The media
 * notification and Android Auto are excluded: they have dedicated custom seek buttons in their layout,
 * so their next/prev buttons always perform chapter skips.
 */
@UnstableApi
internal fun MediaSession?.isRemoteControllerRequest(appPackageName: String): Boolean {
  val currentSession = this ?: return false
  val controller = currentSession.controllerForCurrentRequest

  if (controller != null && (
    currentSession.isMediaNotificationController(controller) ||
      currentSession.isAutoController(controller)
    )
  ) {
    return false
  }

  return true
}
