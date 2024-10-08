//
package com.squareup.tinysweeper.testutils

import com.squareup.tinysweeper.Detector
import org.threeten.bp.Clock

/**
 * Simple composable detector implementation for use in tests
 */
class TestDetector(
  public var name: String,
  override var clock: Clock,
  var evaluateCallback: () -> Boolean,
) : Detector(clock = clock) {
  public var evaluateCount = 0

  override suspend fun evaluate(): Boolean {
    evaluateCount++
    return evaluateCallback()
  }
}
