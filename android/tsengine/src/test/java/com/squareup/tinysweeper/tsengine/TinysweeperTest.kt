package com.squareup.tinysweeper.tsengine

import android.content.Context
import com.squareup.tinysweeper.tsengine.detections.KeyStoreWrapper
import com.squareup.tinysweeper.tsengine.detections.TS_KEYSTORE_ALIAS
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.security.KeyPairGenerator

@RunWith(RobolectricTestRunner::class)
class TinysweeperTest {
  private val mockDeviceId: IdProvider =
    mock<IdProvider> {
      on { getLabel() } doReturn "test-label"
      on { getId() } doReturn "test-id"
    }

  private val mockBootId: IdProvider =
    mock<IdProvider> {
      on { getLabel() } doReturn "test-label"
      on { getId() } doReturn "test-id"
    }

  private val ctx: Context =
    mock<Context> {
      on { getPackageName() } doReturn "com.tinysweeper.tsengine.testpackage"
    }

  @Test
  fun testStart() {
    var mockKeystore: KeyStoreWrapper =
      mock<KeyStoreWrapper> {
        on { getCertificateChain(TS_KEYSTORE_ALIAS) } doReturn arrayOf()
      }
    var mockKeyPairGenerator: KeyPairGenerator = mock<KeyPairGenerator>()

    var ts =
      Tinysweeper(
        ctx,
        deviceIdProvider = mockDeviceId,
        bootIdProvider = mockBootId,
        keyStoreWrapped = mockKeystore,
        keyPairGenerator = mockKeyPairGenerator
      )

    runBlocking {
      ts.start()
      ts.getAttestationResult()
    }
  }
}
