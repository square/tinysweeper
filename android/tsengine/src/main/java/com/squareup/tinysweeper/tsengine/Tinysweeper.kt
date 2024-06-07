package com.squareup.tinysweeper.tsengine

import android.content.Context
import com.squareup.protos.tinysweeper.android.Tinysweeper.Attestation
import com.squareup.tinysweeper.tsengine.detections.DeveloperModeDetection
import com.squareup.tinysweeper.tsengine.detections.PlayIntegrityDetection
import com.squareup.tinysweeper.tsengine.network.ChallengeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.protobuf.ProtoConverterFactory

fun defaultRetrofit(baseURL: String = "https://api.squareup.com"): Retrofit {
  return Retrofit.Builder()
    .baseUrl(baseURL)
    .client(
      OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor())
        .build()
    )
    .addConverterFactory(ProtoConverterFactory.create())
    .build()
}

class Tinysweeper(
  private val ctx: Context,
  private var retrofit: Retrofit = defaultRetrofit(),
  private val atc: AntiTamperClock = AntiTamperClock(System.currentTimeMillis()),
  private val deviceIdProvider: IdProvider = InstallationIdProvider(ctx),
  private val bootIdProvider: IdProvider = RealBootIdProvider(ctx)
) {
  // This should be the only call to System.currentTimeMillis() in Tinysweeper. All other usages
  // of a timestamp should make use of ATC.currentTimeMillis() instead.
  private val playIntegrity: PlayIntegrityDetection
  private val dispatcher: DetectionDispatcher
  private var started: Boolean = false
  private var detectors: List<Detector>
  private var challengeClient: ChallengeClient? = null

  init {
    connectAPIs(retrofit)
    playIntegrity =
      PlayIntegrityDetection(
        ctx,
        challengeClient!!,
        clock = atc,
        idProviders =
          listOf(
            deviceIdProvider,
            bootIdProvider,
          )
      )

    detectors =
      listOf(
        playIntegrity,
        DeveloperModeDetection(ctx, clock = atc)
      )
    dispatcher = DetectionDispatcher(detectors)
  }

  private fun connectAPIs(rf: Retrofit) {
    challengeClient = rf.create(ChallengeClient::class.java)
  }

  fun start(scope: CoroutineScope = GlobalScope) {
    if (!started) {
      scope.launch(Dispatchers.IO) {
        dispatcher.runDetections()
      }
    }
    started = true
  }

  fun stop() {
    started = false
    dispatcher.cancel()
  }

  fun getAttestationResult(): Attestation {
    val attestation =
      Attestation.newBuilder()
        .setClientCurrentTimeS(atc.currentTimeMillis())
    if (playIntegrity.lastAttestation != null) {
      attestation.integrityToken = playIntegrity.lastAttestation
    }

    attestation.addAllDetections(
      detectors
        .filterNot { it is PlayIntegrityDetection }
        .map { detection -> detection.state.build() }.toMutableList(),
    )

    val attestationBuilder = attestation.identifierBuilder

    attestationBuilder
      .setDeviceId(deviceIdProvider.getId())
      .setBootId(bootIdProvider.getId())
      .setApiLevel(android.os.Build.VERSION.SDK_INT.toLong())
      .setOsVersion(android.os.Build.VERSION.BASE_OS)
      .setSecurityPatch(android.os.Build.VERSION.SECURITY_PATCH)
      .setCreationTs(atc.currentTimeMillis())

    return attestation.build()
  }
}
