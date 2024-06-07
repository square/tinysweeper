package com.squareup.tinysweeper.tsengine

import android.content.Context
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TinysweeperTest {
  private val mockDeviceId: IdProvider =
    mock<IdProvider> {
      on { getId() } doReturn "test-id"
    }

  private val mockBootId: IdProvider =
    mock<IdProvider> {
      on { getId() } doReturn "test-id"
    }

  private val ctx: Context =
    mock<Context> {
      on { getPackageName() } doReturn "com.tinysweeper.tsengine.testpackage"
    }

  @Test
  fun testStart() {
    var ts =
      Tinysweeper(
        ctx,
        deviceIdProvider = mockDeviceId,
        bootIdProvider = mockBootId,
      )
    var attestation = ts.getAttestationResult()
    ts.start()
  }
}
