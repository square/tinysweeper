package com.squareup.tinysweeper.detections

import com.google.protobuf.ByteString
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.threeten.bp.Clock
import java.security.KeyPairGenerator
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.util.stream.Collectors
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class AndroidKeyAttestorTest {
  val dummyCert = "definitely a certificate".toByteArray()
  val wantCertChain = arrayOf(ByteString.copyFrom(dummyCert))
  val mockCert: Certificate =
    mock<Certificate> {
      on { encoded } doReturn (dummyCert)
    }

  @Mock private var mockKeystore: KeyStoreWrapper =
    mock<KeyStoreWrapper> {
      on { load(any()) } doAnswer {}
      on { getCertificateChain(TS_KEYSTORE_ALIAS) } doReturn arrayOf(mockCert)
    }

  @Mock private var mockKeyPairGenerator: KeyPairGenerator = mock<KeyPairGenerator>()

  @Mock private var mockKeyGenParameterSpec: AlgorithmParameterSpec = mock<AlgorithmParameterSpec>()

  @Mock private var mockClock: Clock = mock<Clock>()

  @OptIn(ExperimentalEncodingApi::class)
  @Test
  fun getCertificateChainEncoded() {
    val attestor =
      AndroidKeyAttestor(
        mockKeystore,
        mockKeyPairGenerator,
        mockClock
      )

    val got =
      attestor.getCertificateChainEncoded().stream()
        .map { cert: ByteString ->
          return@map ByteString.copyFrom(Base64.decode(cert.toByteArray()))
        }.collect(Collectors.toList())

    assertEquals(wantCertChain[0], got[0])
  }

  @Test fun generateKeyPair() {
    val attestor =
      AndroidKeyAttestor(
        mockKeystore,
        mockKeyPairGenerator,
        mockClock
      )

    attestor.generateKeyPair(mockKeyGenParameterSpec)

    verify(mockKeyPairGenerator).initialize(mockKeyGenParameterSpec)
    verify(mockKeyPairGenerator).generateKeyPair()
  }
}
