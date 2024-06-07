package com.squareup.tinysweeper.tsengine

import com.squareup.tinysweeper.tsengine.testutils.MutableFixedClock
import com.squareup.tinysweeper.tsengine.testutils.TestDetector
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionTest {
  @Test
  fun testNegativeToPositive() =
    runTest {
      val startTime = 1704476478L
      var clock = MutableFixedClock(startTime)

      var evalClosure = false
      val detector =
        TestDetector(
          name = String.format(format = "Detector"),
          clock = clock,
          evaluateCallback = { evalClosure },
        )

      // First time the detection is run
      detector.tick()
      assertFalse(detector.evaluate())

      val initialDetection = detector.state.build()
      assertEquals(initialDetection.lastEvaluationS, startTime)
      assertEquals(initialDetection.triggeredAtS, 0)

      // 1 second later..
      clock.currentTime += 1000
      val detectedTime = clock.currentTime

      evalClosure = true
      detector.tick()
      assertTrue(detector.evaluate())

      val positiveDetection = detector.state.build()
      assertEquals(positiveDetection.lastEvaluationS, detectedTime)
      assertEquals(positiveDetection.triggeredAtS, detectedTime)
    }

  @Test
  fun testPositiveToNegative() =
    runTest {
      val detectedTime = 1704476478L
      var clock = MutableFixedClock(detectedTime)

      var evalClosure = true

      val detector =
        TestDetector(
          name = String.format(format = "Detector"),
          clock = clock,
          evaluateCallback = { evalClosure },
        )

      detector.tick()
      assertTrue(detector.evaluate())
      val positiveDetection = detector.state.build()
      assertEquals(positiveDetection.lastEvaluationS, detectedTime)
      assertEquals(positiveDetection.triggeredAtS, detectedTime)

      // And now the detection is disabled
      evalClosure = false
      clock.currentTime += 1000
      detector.tick()
      assertFalse(detector.evaluate())

      val lastDetection = detector.state.build()
      assertEquals(lastDetection.lastEvaluationS, clock.currentTime)
      assertEquals(lastDetection.triggeredAtS, detectedTime)
      assertNotEquals(clock.currentTime, detectedTime)
    }

  @Test
  fun testNeverTriggered() =
    runTest {
      var clock = MutableFixedClock(1704476478L)

      val detector =
        TestDetector(
          name = String.format(format = "Detector"),
          clock = clock,
          evaluateCallback = { false },
        )

      for (i in 1..10) {
        clock.currentTime += 1000
        detector.tick()
        assertFalse(detector.evaluate())

        val state = detector.state.build()
        assertEquals(state.lastEvaluationS, clock.currentTime)
        assertEquals(state.triggeredAtS, 0)
      }
    }

  @Test
  fun testAlwaysPositive() =
    runTest {
      var clock = MutableFixedClock(1704476478L)

      val detector =
        TestDetector(
          name = String.format(format = "Detector"),
          clock = clock,
          evaluateCallback = { true },
        )

      for (i in 1..10) {
        clock.currentTime += 1000
        detector.tick()
        assertTrue(detector.evaluate())

        val state = detector.state.build()
        assertEquals(state.lastEvaluationS, clock.currentTime)
        assertEquals(state.triggeredAtS, clock.currentTime)
      }
    }
}
