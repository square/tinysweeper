package com.squareup.tinysweeper.tsengine

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.supervisorScope

class DetectionDispatcher(private var detections: List<Detector>) {
  suspend fun runDetections() =
    supervisorScope {
      var detectionCoroutines = mutableListOf<Job>()
      for (detection in detections) {
        detectionCoroutines.add(async { detection.start() })
      }
      detectionCoroutines.joinAll()
    }

  fun cancel() {
    for (detection in detections) {
      detection.stop()
    }
  }
}
