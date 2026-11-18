// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.shake

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects phone movement/shaking across [ShakeSensitivity] levels by measuring linear
 * acceleration relative to [gravityBaseline] (`|magnitude - gravityBaseline|`) over a sliding
 * time window and scaling both the magnitude threshold and sustained sample requirement per tier.
 */
class SamplingShakeDetector(
  private val gravityBaseline: Double = ShakeSensitivityMagnitudes.gravity,
  private val onShake: () -> Unit,
) {
  /**
   * Controls both the linear acceleration delta threshold and the sustained-motion requirement.
   */
  var sensitivity: ShakeSensitivity = ShakeSensitivity.Default
    set(value) {
      if (field != value) {
        field = value
        queue.clear()
      }
    }

  private val queue = SampleQueue()
  private var lastTriggerTimestampNs: Long = Long.MIN_VALUE

  /**
   * Add an accelerometer event to this detector's queue to sample and
   * detect movement/shaking events.
   */
  fun addAccelerometerEvent(event: AccelerometerEvent) {
    val timestamp = event.timestamp
    if (lastTriggerTimestampNs != Long.MIN_VALUE &&
      timestamp >= lastTriggerTimestampNs &&
      timestamp - lastTriggerTimestampNs < COOLDOWN_NS
    ) {
      return
    }
    val accelerating = isAccelerating(event)
    queue.add(timestamp, accelerating)
    if (queue.isShaking(sensitivity)) {
      queue.clear()
      lastTriggerTimestampNs = timestamp
      onShake()
    }
  }

  fun clear() {
    queue.clear()
  }

  /** Returns true if the device's linear acceleration delta from gravity exceeds [ShakeSensitivity.value]. */
  private fun isAccelerating(event: AccelerometerEvent): Boolean {
    val (ax, ay, az) = event
    val magnitude = sqrt(ax * ax + ay * ay + az * az)
    return abs(magnitude - gravityBaseline) > sensitivity.value
  }

  /** Queue of samples. Keeps a running average.  */
  internal class SampleQueue {
    private val pool = SamplePool()

    private var oldest: Sample? = null
    private var newest: Sample? = null
    private var sampleCount = 0
    private var acceleratingCount = 0

    /**
     * Adds a sample.
     *
     * @param timestamp    in nanoseconds of sample
     * @param accelerating true if > [.accelerationThreshold].
     */
    fun add(timestamp: Long, accelerating: Boolean) {
      if (newest != null && timestamp < newest!!.timestamp) {
        clear()
      }
      // Purge samples that precede the active sliding window (and always expire stale samples > 1s old).
      purge(timestamp - MAX_WINDOW_SIZE, maxAgeCutoff = timestamp - MAX_SAMPLE_AGE_NS)

      // Add the sample to the queue.
      val added = pool.acquire()
      added.timestamp = timestamp
      added.accelerating = accelerating
      added.next = null
      if (newest != null) {
        newest!!.next = added
      }
      newest = added
      if (oldest == null) {
        oldest = added
      }

      // Update running average.
      sampleCount++
      if (accelerating) {
        acceleratingCount++
      }
    }

    /** Removes all samples from this queue.  */
    fun clear() {
      while (oldest != null) {
        val removed: Sample = oldest!!
        oldest = removed.next
        pool.release(removed)
      }
      newest = null
      sampleCount = 0
      acceleratingCount = 0
    }

    /** Purges samples with timestamps older than cutoff.  */
    fun purge(cutoff: Long, maxAgeCutoff: Long = cutoff) {
      while (oldest != null && (oldest!!.timestamp < maxAgeCutoff || (sampleCount >= MIN_QUEUE_SIZE && cutoff - oldest!!.timestamp > 0))) {
        // Remove sample.
        val removed: Sample = oldest!!
        if (removed.accelerating) {
          acceleratingCount--
        }
        sampleCount--

        oldest = removed.next
        if (oldest == null) {
          newest = null
        }
        pool.release(removed)
      }
    }

    /** Copies the samples into a list, with the oldest entry at index 0.  */
    fun asList(): List<Sample> {
      val list: MutableList<Sample> = ArrayList()
      var s = oldest
      while (s != null) {
        list.add(s)
        s = s.next
      }
      return list
    }

    fun isShaking(sensitivity: ShakeSensitivity): Boolean {
      val newestSample = newest ?: return false
      val oldestSample = oldest ?: return false
      if (newestSample.timestamp - oldestSample.timestamp < sensitivity.minWindowNs) return false
      if (acceleratingCount < sensitivity.minAcceleratingSamples) return false
      if (sampleCount == 0) return false
      return acceleratingCount.toDouble() / sampleCount.toDouble() >= sensitivity.minAcceleratingFraction
    }

    companion object {
      /** Window size in ns. Used to compute the running movement ratio. */
      private const val MAX_WINDOW_SIZE: Long = 500_000_000L // 0.5s
      private const val MAX_SAMPLE_AGE_NS: Long = 1_000_000_000L // 1.0s

      /**
       * Ensure the queue size never falls below this size during short windows.
       */
      private const val MIN_QUEUE_SIZE = 4
    }
  }

  companion object {
    private const val COOLDOWN_NS: Long = 500_000_000L // 0.5s
  }

  /** An accelerometer sample.  */
  internal class Sample {
    /** Time sample was taken.  */
    var timestamp: Long = 0

    /** If acceleration > [.accelerationThreshold].  */
    var accelerating: Boolean = false

    /** Next sample in the queue or pool.  */
    var next: Sample? = null
  }

  /** Pools samples. Avoids garbage collection.  */
  internal class SamplePool {
    private var head: Sample? = null

    /** Acquires a sample from the pool.  */
    fun acquire(): Sample {
      var acquired = head
      if (acquired == null) {
        acquired = Sample()
      } else {
        // Remove instance from pool.
        head = acquired.next
      }
      return acquired
    }

    /** Returns a sample to the pool.  */
    fun release(sample: Sample) {
      sample.next = head
      head = sample
    }
  }
}
