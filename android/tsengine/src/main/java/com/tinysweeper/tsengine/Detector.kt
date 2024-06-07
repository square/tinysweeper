package com.tinysweeper.tsengine

import com.squareup.protos.tinysweeper.android.Tinysweeper.Detection
import kotlinx.coroutines.coroutineScope
import org.threeten.bp.Clock

abstract class Detector(
  open var timer: DetectionTimer = newDefaultJitterTimer(),
  open var clock: Clock,
) {
  var enabled = false

  /** starts periodic evaluation via kotlin coroutines, keeps hasTriggered updated **/
  open suspend fun start() {
    enabled = true
    coroutineScope {
      while (enabled) {
        tick()
        timer.wait()
      }
    }
  }

  open fun stop() {
    enabled = false
  }

  suspend fun tick() {
    var currentTime = clock.instant()
    state.setLastEvaluationS(currentTime.toEpochMilli())

    if (evaluate()) {
      state.setTriggeredAtS(currentTime.toEpochMilli())
    }
  }

  abstract suspend fun evaluate(): Boolean

  var state: Detection.Builder = Detection.newBuilder()
}
