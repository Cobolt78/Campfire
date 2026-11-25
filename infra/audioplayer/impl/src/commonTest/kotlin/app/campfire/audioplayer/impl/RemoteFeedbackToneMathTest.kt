// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThanOrEqualTo
import kotlin.math.abs
import kotlin.test.Test

class RemoteFeedbackToneMathTest {

  @Test
  fun synthesizeTonePcm_hasExpectedSampleCountLeadingSilenceAndBoundedAmplitude() {
    val maxExpected = (Short.MAX_VALUE * 0.35).toInt()
    val leadingSilenceSamples = REMOTE_FEEDBACK_SAMPLE_RATE * REMOTE_FEEDBACK_LEADING_SILENCE_MS / 1000

    for (tone in RemoteFeedbackTone.entries) {
      val pcm = synthesizeTonePcm(tone)
      val segments = tone.segments()
      val expectedNoteSamples = segments.sumOf {
        (REMOTE_FEEDBACK_SAMPLE_RATE * it.durationMs / 1000) +
          (REMOTE_FEEDBACK_SAMPLE_RATE * it.gapAfterMs / 1000)
      }
      assertThat(pcm.size).isEqualTo(leadingSilenceSamples + expectedNoteSamples)

      for (i in 0 until leadingSilenceSamples) {
        assertThat(pcm[i].toInt()).isEqualTo(0)
      }

      val peak = pcm.maxOf { abs(it.toInt()) }
      assertThat(peak).isGreaterThan(0)
      assertThat(peak).isLessThanOrEqualTo(maxExpected)
    }
  }

  @Test
  fun skipForwardAndBackwardWaveformsAreDistinct() {
    val forward = synthesizeTonePcm(RemoteFeedbackTone.SKIP_FORWARD)
    val backward = synthesizeTonePcm(RemoteFeedbackTone.SKIP_BACKWARD)
    val bookmark = synthesizeTonePcm(RemoteFeedbackTone.BOOKMARK_ADDED)

    assertThat(forward.contentEquals(backward)).isFalse()
    assertThat(forward.contentEquals(bookmark)).isFalse()
    assertThat(backward.contentEquals(bookmark)).isFalse()
  }
}
