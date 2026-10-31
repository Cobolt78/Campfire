// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.settings.EnumSetting
import app.campfire.core.settings.EnumSettingProvider

enum class RemoteControlAction(
  override val storageKey: String,
  val label: String,
) : EnumSetting {
  SKIP_FORWARD_TIME("skip_forward_time", "Skip forward"),
  SKIP_BACKWARD_TIME("skip_backward_time", "Skip backward"),
  NEXT_CHAPTER("next_chapter", "Skip to next chapter"),
  PREVIOUS_CHAPTER("previous_chapter", "Skip to previous chapter"),
  ADD_BOOKMARK("add_bookmark", "Add bookmark"),
  NONE("none", "Do nothing"),
  ;

  companion object : EnumSettingProvider<RemoteControlAction> {
    override fun fromStorageKey(key: String?): RemoteControlAction =
      fromKeyOrDefault(key, SKIP_FORWARD_TIME)

    fun fromKeyOrDefault(key: String?, default: RemoteControlAction): RemoteControlAction =
      entries.find { it.storageKey == key } ?: default

    fun provider(default: RemoteControlAction) = object : EnumSettingProvider<RemoteControlAction> {
      override fun fromStorageKey(key: String?): RemoteControlAction = fromKeyOrDefault(key, default)
    }
  }
}
