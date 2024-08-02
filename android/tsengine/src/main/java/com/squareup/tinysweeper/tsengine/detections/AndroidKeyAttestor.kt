package com.squareup.tinysweeper.tsengine.detections

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.protobuf.ByteString
import org.threeten.bp.Clock
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.ECGenParameterSpec
import java.util.Arrays
import java.util.Date
import java.util.stream.Collectors
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

val TS_KEYSTORE_ALIAS = "Tinysweeper-cert-key"
val TS_DEFAULT_KEYSTORE = "AndroidKeyStore"
val EC_CURVE = "secp256r1"

// Keystore methods are all final; preventing Mockito from mocking them.
open class KeyStoreWrapper(
  private var keyStore: KeyStore
) {
  open fun load(param: KeyStore.LoadStoreParameter?) {
    keyStore.load(param)
  }

  open fun getCertificateChain(alias: String): Array<Certificate> = keyStore.getCertificateChain(alias)
}

class AndroidKeyAttestor(
  private var keyStoreWrapped: KeyStoreWrapper,
  private var keyPairGenerator: KeyPairGenerator,
  private var clock: Clock
) {
  init {
    keyStoreWrapped.load(null)
  }

  @OptIn(ExperimentalEncodingApi::class)
  fun getCertificateChainEncoded(): MutableList<ByteString> {
    return Arrays
      .stream(keyStoreWrapped.getCertificateChain(TS_KEYSTORE_ALIAS))
      .map { certificate: Certificate ->
        return@map ByteString.copyFrom(Base64.encode(certificate.encoded), "utf-8")
      }.collect(Collectors.toList())
  }

  fun generateKeyPair(spec: AlgorithmParameterSpec) {
    keyPairGenerator.initialize(spec)
    keyPairGenerator.generateKeyPair()
  }

  fun getKeyPairAlgorithmParameterSpec(challenge: ByteArray): KeyGenParameterSpec {
    val builder =
      KeyGenParameterSpec
        // PURPOSE_ATTEST_KEY doesn't exist on older devices
        .Builder(TS_KEYSTORE_ALIAS, KeyProperties.PURPOSE_SIGN)
        .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
        .setDigests(KeyProperties.DIGEST_SHA256)
        .setAttestationChallenge(challenge)
        .setKeyValidityStart(Date(clock.millis()))

    return builder.build()
  }
}
