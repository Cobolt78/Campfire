// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class PlaybackSessionDeserializationTest {

  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
  }

  @Test
  fun deserializesLegacyPlaybackSessionWithNullAndMissingOptionalFields() {
    val payload = """
      {
        "id": "sess_1",
        "userId": "user_1",
        "libraryId": "lib_1",
        "libraryItemId": "item_1",
        "mediaType": "book",
        "mediaMetadata": {
          "title": "Legacy Book"
        },
        "displayTitle": null,
        "displayAuthor": null,
        "coverPath": null,
        "mediaPlayer": null,
        "deviceInfo": null,
        "serverVersion": null,
        "date": null,
        "dayOfWeek": null,
        "timeListening": 120.5
      }
    """.trimIndent()

    val session = json.decodeFromString<PlaybackSession>(payload)
    assertEquals("sess_1", session.id)
    assertEquals(emptyList(), session.chapters)
    assertNull(session.displayTitle)
    assertNull(session.deviceInfo)
    assertEquals(120.5f, session.timeListening)
  }
}
