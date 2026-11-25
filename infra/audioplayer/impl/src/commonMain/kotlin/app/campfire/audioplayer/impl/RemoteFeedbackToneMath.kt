// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val REMOTE_FEEDBACK_SAMPLE_RATE = 44_100
internal const val REMOTE_FEEDBACK_LEADING_SILENCE_MS = 40

internal enum class RemoteFeedbackTone {
  SKIP_FORWARD,
  SKIP_BACKWARD,
  BOOKMARK_ADDED,
}

internal data class ToneSegment(
  val frequencyHz: Double,
  val durationMs: Int,
  val gapAfterMs: Int = 0,
)

internal fun RemoteFeedbackTone.segments(): List<ToneSegment> = when (this) {
  RemoteFeedbackTone.SKIP_FORWARD -> listOf(
    ToneSegment(frequencyHz = 523.25, durationMs = 55, gapAfterMs = 15), // C5 -> E5 ascending
    ToneSegment(frequencyHz = 659.25, durationMs = 75),
  )

  RemoteFeedbackTone.SKIP_BACKWARD -> listOf(
    ToneSegment(frequencyHz = 659.25, durationMs = 55, gapAfterMs = 15), // E5 -> C5 descending
    ToneSegment(frequencyHz = 523.25, durationMs = 75),
  )

  RemoteFeedbackTone.BOOKMARK_ADDED -> listOf(
    ToneSegment(frequencyHz = 587.33, durationMs = 65, gapAfterMs = 20), // D5 -> A5 ping
    ToneSegment(frequencyHz = 880.0, durationMs = 110),
  )
}

internal fun synthesizeTonePcm(
  tone: RemoteFeedbackTone,
  sampleRate: Int = REMOTE_FEEDBACK_SAMPLE_RATE,
  amplitude: Double = 0.28,
): ShortArray {
  val segments = tone.segments()
  val leadingSilenceSamples = sampleRate * REMOTE_FEEDBACK_LEADING_SILENCE_MS / 1000
  val segmentCounts = segments.map { segment ->
    val noteSamples = sampleRate * segment.durationMs / 1000
    val gapSamples = sampleRate * segment.gapAfterMs / 1000
    noteSamples to gapSamples
  }
  val totalSamples = leadingSilenceSamples + segmentCounts.sumOf { (note, gap) -> note + gap }
  val buffer = ShortArray(totalSamples)

  var offset = leadingSilenceSamples
  val fadeSamplesMax = sampleRate * 5 / 1000

  segments.forEachIndexed { idx, segment ->
    val (noteSamples, gapSamples) = segmentCounts[idx]
    val fadeSamples = fadeSamplesMax.coerceAtMost(noteSamples / 2)
    for (i in 0 until noteSamples) {
      val envelope = when {
        fadeSamples > 0 && i < fadeSamples -> i.toDouble() / fadeSamples.toDouble()
        fadeSamples > 0 && i >= noteSamples - fadeSamples ->
          (noteSamples - 1 - i).toDouble() / fadeSamples.toDouble()
        else -> 1.0
      }
      val phase = 2.0 * PI * segment.frequencyHz * i.toDouble() / sampleRate.toDouble()
      val raw = (sin(phase) * amplitude * envelope * Short.MAX_VALUE)
        .roundToInt()
        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
      buffer[offset + i] = raw.toShort()
    }
    offset += noteSamples + gapSamples
  }

  return buffer
}
