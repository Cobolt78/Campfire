// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.sleep

import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object VolumeFadeController {

  fun fade(
    scope: CoroutineScope,
    duration: Duration,
    tickRate: Long,
    getVolume: () -> Float,
    setVolume: (Float) -> Unit,
    onPause: () -> Unit,
    now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
  ): Job {
    return scope.launch {
      val startVolume = getVolume()
      val totalMillis = duration.inWholeMilliseconds.toFloat()
      val delayStep = 1000L / tickRate

      // Grab a timestamp and never let the loop here extend past the [duration]
      try {
        val start = now()
        while (isActive) {
          val elapsed = (now() - start).toFloat()
          if (elapsed >= totalMillis) break

          // Calculate remaining fraction (1.0 at start down to 0.0 at end)
          val remainingFraction = ((totalMillis - elapsed) / totalMillis).coerceIn(0f, 1f)

          // Apply perceptual loudness curve (squared) for human hearing
          val volumeFactor = remainingFraction * remainingFraction
          setVolume((startVolume * volumeFactor).coerceIn(0f, 1f))

          delay(delayStep)
        }

        setVolume(0f)
        onPause()
      } finally {
        // Reset the volume to where it started whether completed or cancelled
        setVolume(startVolume)
      }
    }
  }
}
