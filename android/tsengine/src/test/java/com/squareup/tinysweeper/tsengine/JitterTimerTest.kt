package com.squareup.tinysweeper.tsengine

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class JitterTimerTest {
  class ControlledRandomLong(
    public var wantValue: Long,
  ) : Random() {
    override fun nextBits(bitCount: Int): Int {
      return wantValue.toInt()
    }

    override fun nextLong(
      from: Long,
      until: Long,
    ): Long {
      return wantValue
    }
  }

  @Test
  fun testUpperBound() {
    val avg = 30.toDuration(DurationUnit.MILLISECONDS)
    val jitter = 29.toDuration(DurationUnit.MILLISECONDS)

    val max = avg.toLong(DurationUnit.MILLISECONDS) + jitter.toLong(DurationUnit.MILLISECONDS)

    var rand = ControlledRandomLong(jitter.toLong(DurationUnit.MILLISECONDS))
    var timer =
      JitterTimer(
        avg,
        jitter,
        rand,
      )

    assertEquals(timer.calculateDelay(), max.toDuration(DurationUnit.MILLISECONDS))
  }
}
