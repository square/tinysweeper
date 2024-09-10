package com.squareup.tinysweeper

import android.content.Context
import android.security.keystore.KeyProperties
import com.squareup.protos.tinysweeper.android.Tinysweeper.Attestation
import com.squareup.tinysweeper.detections.AndroidKeyAttestor
import com.squareup.tinysweeper.detections.DeveloperModeDetection
import com.squareup.tinysweeper.detections.KeyStoreWrapper
import com.squareup.tinysweeper.detections.PlayIntegrityDetection
import com.squareup.tinysweeper.detections.TS_DEFAULT_KEYSTORE
import com.squareup.tinysweeper.network.ChallengeClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.protobuf.ProtoConverterFactory
import java.security.KeyPairGenerator
import java.security.KeyStore

fun defaultRetrofit(baseURL: String = "https://api.squareup.com"): Retrofit =
  Retrofit
    .Builder()
    .baseUrl(baseURL)
    .client(
      OkHttpClient
        .Builder()
        .addInterceptor(HttpLoggingInterceptor())
        .build()
    ).addConverterFactory(ProtoConverterFactory.create())
    .build()

class Tinysweeper
  @JvmOverloads
  constructor(
    private val ctx: Context,
    private var retrofit: Retrofit = defaultRetrofit(),
    private val atc: AntiTamperClock = AntiTamperClock(System.currentTimeMillis()),
    private val deviceIdProvider: IdProvider = InstallationIdProvider(ctx),
    private val bootIdProvider: IdProvider = RealBootIdProvider(ctx),
    private val keyStoreWrapped: KeyStoreWrapper =
      KeyStoreWrapper(KeyStore.getInstance(TS_DEFAULT_KEYSTORE)),
    private val keyPairGenerator: KeyPairGenerator =
      KeyPairGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_EC,
        TS_DEFAULT_KEYSTORE
      ),
  ) {
    // This should be the only call to System.currentTimeMillis() in Tinysweeper. All other usages
    // of a timestamp should make use of ATC.currentTimeMillis() instead.
    private val playIntegrity: PlayIntegrityDetection

    private val keyAttestation: AndroidKeyAttestor

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

      keyAttestation =
        AndroidKeyAttestor(
          keyStoreWrapped = keyStoreWrapped,
          keyPairGenerator = keyPairGenerator,
          clock = atc
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

    suspend fun start() {
      playIntegrity.warmupWithRequest()
      if (!started) {
        dispatcher.runDetections()
      }
      started = true
    }

    fun stop() {
      started = false
      dispatcher.cancel()
    }

    suspend fun getAttestationResult(): Attestation {
      val attestation =
        Attestation
          .newBuilder()
          .setClientCurrentTimeS(atc.currentTimeMillis())

      attestation.integrityToken = playIntegrity.attest()

      attestation.addAllDetections(
        detectors
          .filterNot { it is PlayIntegrityDetection }
          .map { detection -> detection.state.build() }
          .toMutableList(),
      )

      val challenge = "aaaabbbbccccddddeeeeffffgggghhhh".toByteArray()
      val spec = keyAttestation.getKeyPairAlgorithmParameterSpec(challenge)

      keyAttestation.generateKeyPair(spec)

      attestation.addAllKeystoreCertificateChain(keyAttestation.getCertificateChainEncoded())

      val attestationBuilder = attestation.identifierBuilder

      attestationBuilder
        .setDeviceId(deviceIdProvider.getId())
        .setBootId(bootIdProvider.getId())
        .setApiLevel(
          android.os.Build.VERSION.SDK_INT
            .toLong()
        ).setOsVersion(android.os.Build.VERSION.BASE_OS)
        .setSecurityPatch(android.os.Build.VERSION.SECURITY_PATCH)
        .setCreationTs(atc.currentTimeMillis())

      return attestation.build()
    }
  }
