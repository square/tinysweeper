package com.squareup.tinysweeper.tsengine

import android.os.SystemClock
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import kotlin.math.max

/**
 * This class should act as a sub-in for the normal System clock.
 * Tinysweeper makes use of timestamps to keep track of detections being triggered, but for most
 * android devices the user can adjust the system clock at will without requiring special
 * permissions. To get around this, we make use of System.elapsedRealTime() which gives the number
 * of milliseconds since boot, and is guaranteed to be monotonic. We use this value to calculate how
 * much time has elapsed since a "local epoch" which is recorded once at TinySweeper init in order
 * to calculate a more trustable timestamp.
 *
 * Currently the local epoch is just taken from the local System time, however in the future it
 * could be taken from some server side source instead for more consistent timelines.
 */
class AntiTamperClock(localEpoch: Long) : Clock() {
  private val timeSinceBootAtInit = android.os.SystemClock.elapsedRealtime()

  // if someone set their system clock to some extremely small number,
  // just default to a bootEpoch of 0 to avoid any strange negative epoch edge cases.
  private val bootEpoch = max(localEpoch - timeSinceBootAtInit, 0)

  override fun getZone(): ZoneId {
    TODO("Not yet implemented")
  }

  override fun withZone(p0: ZoneId?): Clock {
    TODO("Not yet implemented")
  }

  override fun instant(): Instant {
    return Instant.ofEpochMilli(currentTimeMillis())
  }

  fun currentTimeMillis(): Long {
    return bootEpoch + android.os.SystemClock.elapsedRealtime()
  }
}
