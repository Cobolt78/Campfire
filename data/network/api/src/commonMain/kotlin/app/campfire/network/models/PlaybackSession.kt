// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackSession(
  val id: String,
  val userId: String,
  val libraryId: String,
  val libraryItemId: String,
  val episodeId: String? = null,
  val mediaType: String = "book",
  val mediaMetadata: ExpandedBookMetadata,
  val chapters: List<BookChapter> = emptyList(),
  val displayTitle: String? = null,
  val displayAuthor: String? = null,
  val coverPath: String? = null,
  val duration: Double = 0.0,
  val playMethod: Int = 0,
  val mediaPlayer: String? = null,
  val deviceInfo: DeviceInfo? = null,
  val serverVersion: String? = null,
  val date: String? = null,
  val dayOfWeek: String? = null,
  val timeListening: Float? = null,
  val startTime: Float = 0f,
  val currentTime: Float = 0f,
  val startedAt: Long = 0L,
  val updatedAt: Long = 0L,
)
