package com.squareup.tinysweeper.tsengine

import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface DetectionTimer {
  suspend fun wait()
}

/**
 * Timer with an added random jitter delay
 */
class JitterTimer(
  /**
   * Average attestation period. Our bounds for attestation will be avg ± jitter
   */
  var avgAttestationPeriod: Duration,
  /**
   * Mark the maximum amount of random "noise" added or subtracted to the attestationPeriod to
   * prevent attackers from abusing checking intervals delays. A safe default would be
   * maxJitter == (attestationPeriod - 1).
   *
   * delay(0) has surprising behavior https://github.com/Kotlin/kotlinx.coroutines/issues/3373
   * Lets avoid it if we can
   **/
  var maxJitter: Duration = avgAttestationPeriod.minus(1.toDuration(DurationUnit.MILLISECONDS)),
  var random: Random = Random.Default,
) : DetectionTimer {
  /** Marks the lifespan of a detection in milliseconds **/
  fun attestationPeriod(): Duration {
    return avgAttestationPeriod + maxJitter
  }

  fun calculateDelay(): Duration {
    var randomJitter =
      random.nextLong(
        maxJitter.toLong(DurationUnit.MILLISECONDS) * -1,
        maxJitter.toLong(DurationUnit.MILLISECONDS),
      ).toDuration(DurationUnit.MILLISECONDS)
    return avgAttestationPeriod.plus(randomJitter)
  }

  override suspend fun wait() {
    delay(calculateDelay())
  }
}

fun newDefaultJitterTimer(): JitterTimer {
  return JitterTimer(500.toDuration(DurationUnit.MILLISECONDS))
}
