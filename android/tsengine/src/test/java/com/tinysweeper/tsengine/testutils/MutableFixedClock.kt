package com.tinysweeper.tsengine.testutils

import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId

/*
 * Clock.fixed() is immutable. Utility class with the same behavior as Clock.fixed() but with
 * the ability to modify the time.
 */
class MutableFixedClock(
  public var currentTime: Long,
  var zoneId: ZoneId = ZoneId.systemDefault(),
) : Clock() {
  override fun getZone(): ZoneId {
    return zoneId
  }

  override fun withZone(z: ZoneId?): Clock {
    zoneId = z!!
    return this
  }

  override fun instant(): Instant {
    return Instant.ofEpochMilli(currentTime)
  }
}
