package com.squareup.tinysweeper.detections

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.protobuf.ByteString
import com.squareup.lando.service.challenge.Challenge.ChallengeRequest
import com.squareup.lando.service.challenge.Challenge.ChallengeRequest.Platform.ANDROID_PLAYINTEGRITY
import com.squareup.lando.service.challenge.Challenge.ChallengeResponse
import com.squareup.protos.tinysweeper.android.Tinysweeper.PlayIntegrityAttestation
import com.squareup.tinysweeper.Detector
import com.squareup.tinysweeper.IdProvider
import com.squareup.tinysweeper.JitterTimer
import com.squareup.tinysweeper.network.ChallengeClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import org.threeten.bp.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PlayIntegrityDetection(
  private var ctx: Context,
  private var challengeClient: ChallengeClient?,
  private var packageName: String = ctx.getPackageName(),
  private var integrityManager: StandardIntegrityManager? =
    try {
      IntegrityManagerFactory.createStandard(
        ctx
      )
    } catch (e: Exception) {
      null
    },
  private var googleApiAvailability: GoogleApiAvailability = GoogleApiAvailability.getInstance(),
  private val idProviders: List<IdProvider>,
  clock: Clock
) : Detector(
    JitterTimer(1.toDuration(DurationUnit.MINUTES), 30.toDuration(DurationUnit.SECONDS)),
    clock = clock
  ) {
  private var tokenProviderLock = Mutex()
  var tokenProvider: StandardIntegrityTokenProvider? = null
  public var lastAttestation: PlayIntegrityAttestation? = null

  /**
   * Warm up the integrity token provider.
   *
   * See https://developer.android.com/google/play/integrity/standard#prepare-integrity",
   */
  suspend fun warmup(cloudProject: Long) {
    if (!supportedAPI()) {
      return
    }

    tokenProviderLock.lock()
    try {
      tokenProvider =
        integrityManager
          ?.prepareIntegrityToken(
            PrepareIntegrityTokenRequest.builder().setCloudProjectNumber(cloudProject).build()
          )?.await()
    } finally {
      tokenProviderLock.unlock()
    }
  }

  fun supportedAPI(): Boolean {
    val status = googleApiAvailability.isGooglePlayServicesAvailable(ctx)
    if (status != ConnectionResult.SUCCESS) {
      return false
    }
    return true
  }

  // Completes an Interpol Challenge; gets nonce/requesthash.
  suspend fun challenge(): ChallengeResponse? {
    val req =
      ChallengeRequest
        .newBuilder()
        .setPlatform(ANDROID_PLAYINTEGRITY)
        .putAllIdentifiers(
          idProviders.associateBy(
            keySelector = { it.getLabel() },
            valueTransform = { it.getId() }
          )
        ).build()
    val resp = challengeClient?.challenge(req)
    return resp?.body()
  }

  suspend fun warmupWithRequest() {
    var challenge: ChallengeResponse? = challenge()

    if (challenge == null) {
      return
    }

    if (tokenProvider == null) {
      warmup(challenge.playIntegrityCloudProject)
    }
  }

  suspend fun attest(): PlayIntegrityAttestation? {
    if (!supportedAPI()) {
      return null
    }

    if (integrityManager == null) {
      return null
    }

    var challenge: ChallengeResponse? = challenge()

    if (challenge == null) {
      return null
    }

    tokenProviderLock.lock()
    try {
      if (tokenProvider == null) {
        warmup(challenge.playIntegrityCloudProject)
      }
    } finally {
      tokenProviderLock.unlock()
    }

    var gotToken: StandardIntegrityToken?
    val task =
      tokenProvider?.request(
        StandardIntegrityTokenRequest
          .builder()
          .setRequestHash(challenge.nonce?.toString("utf-8"))
          .build(),
      )

    gotToken = task?.await()
    if (gotToken == null) {
      return null
    }

    val attestation =
      PlayIntegrityAttestation
        .newBuilder()
        .setIntegrityNonceCreatedAtMs(challenge.nonceCreatedAtMs)
        .setToken(ByteString.copyFrom(gotToken.token(), "utf-8"))
        .setPackageName(packageName)
        .build()

    lastAttestation = attestation
    return attestation
  }

  override suspend fun evaluate(): Boolean {
    lastAttestation = attest() ?: lastAttestation

    // Due to its nature as a data collection / root of trust for the rest of Tinysweeper,
    // this detection itself should never return `true`.
    return lastAttestation != null
  }

  override fun toString(): String = lastAttestation.toString()
}
