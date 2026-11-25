// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays synthesized, low-latency audio chimes (`USAGE_MEDIA`, without requesting AudioFocus) and
 * brief haptic confirmations for remote/headset bookmark and skip actions.
 */
internal class RemoteFeedbackPlayer(
  private val context: Context,
  private val scope: CoroutineScope,
) {
  private val cachedPcm = ConcurrentHashMap<RemoteFeedbackTone, ShortArray>()
  private val lock = Any()
  private var activeJob: Job? = null
  private var activeTrack: AudioTrack? = null

  @Suppress("DEPRECATION")
  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  fun playTone(tone: RemoteFeedbackTone) {
    val previousJob: Job?
    val previousTrack: AudioTrack?
    synchronized(lock) {
      previousJob = activeJob
      previousTrack = activeTrack
      activeJob = null
      activeTrack = null
    }
    previousJob?.cancel()
    releaseTrackQuietly(previousTrack)

    val job = scope.launch(Dispatchers.Default) {
      var track: AudioTrack? = null
      try {
        val pcm = cachedPcm.getOrPut(tone) { synthesizeTonePcm(tone) }
        val bufferSizeBytes = pcm.size * Short.SIZE_BYTES
        val createdTrack = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build(),
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(REMOTE_FEEDBACK_SAMPLE_RATE)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build(),
          )
          .setBufferSizeInBytes(bufferSizeBytes)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
          .build()

        track = createdTrack
        val written = createdTrack.write(pcm, 0, pcm.size)
        if (written <= 0) {
          bark(LogPriority.WARN) { "RemoteFeedbackPlayer failed to write PCM samples ($written)" }
          return@launch
        }

        synchronized(lock) {
          activeTrack = createdTrack
        }
        createdTrack.play()

        val durationMs = (pcm.size * 1000L) / REMOTE_FEEDBACK_SAMPLE_RATE
        delay(durationMs + 40L)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Throwable) {
        bark(LogPriority.WARN, throwable = e) { "RemoteFeedbackPlayer failed playing $tone" }
      } finally {
        synchronized(lock) {
          if (activeTrack === track) {
            activeTrack = null
          }
        }
        releaseTrackQuietly(track)
      }
    }

    synchronized(lock) {
      activeJob = job
    }
  }

  fun vibrateBookmarkConfirmation() {
    try {
      val v = vibrator ?: return
      if (!v.hasVibrator()) return
      v.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 45L, 55L, 45L), -1))
    } catch (e: Throwable) {
      bark(LogPriority.WARN, throwable = e) { "RemoteFeedbackPlayer failed to vibrate" }
    }
  }

  fun release() {
    val jobToCancel: Job?
    val trackToRelease: AudioTrack?
    synchronized(lock) {
      jobToCancel = activeJob
      trackToRelease = activeTrack
      activeJob = null
      activeTrack = null
    }
    jobToCancel?.cancel()
    releaseTrackQuietly(trackToRelease)
  }

  private fun releaseTrackQuietly(track: AudioTrack?) {
    if (track == null) return
    runCatching {
      if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
        track.stop()
      }
    }
    runCatching { track.release() }
  }
}
