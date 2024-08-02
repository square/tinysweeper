package com.squareup.tinysweeper.tsengine.detections

import android.app.Activity
import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.protobuf.ByteString
import com.squareup.lando.service.challenge.Challenge.ChallengeRequest
import com.squareup.lando.service.challenge.Challenge.ChallengeResponse
import com.squareup.tinysweeper.tsengine.IdProvider
import com.squareup.tinysweeper.tsengine.network.ChallengeClient
import com.squareup.tinysweeper.tsengine.testutils.MutableFixedClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal class PlayIntegrityDetectionTest {
  private val wantNonce = "a1NHN3YxRnhBYjN6TklacVJmVk00R2ZYbUJDTEtWOGtZbU5xdGltK3J2UT0="
  private val wantToken = "some-token"
  private val wantDeviceId = "my-device-id"
  private val wantBootId = "my-boot-id"
  private val packageName = "com.tinysweeper.tsengine"
  private val wantTimestamp = 1681842240346L
  private val cloudProject = 471440260729

  private val sampleChallenge =
    ChallengeResponse.newBuilder()
      .setPlayIntegrityCloudProject(cloudProject)
      .setNonceCreatedAtMs(wantTimestamp)
      .setNonce(ByteString.copyFromUtf8(wantNonce))
      .build()

  @Mock private var ctx: Context = mock<Context>()

  @Mock private var mockDeviceIdProvider: IdProvider =
    mock<IdProvider> {
      on { getId() } doReturn wantDeviceId
      on { getLabel() } doReturn "DeviceID"
    }

  @Mock private var mockBootIdProvider: IdProvider =
    mock<IdProvider> {
      on { getId() } doReturn wantBootId
      on { getLabel() } doReturn "BootID"
    }

  class TestChallengeClient(
    private var want: Response<ChallengeResponse>,
  ) : ChallengeClient {
    var reqCount = 0

    @POST("/1.0/lando/challenge")
    override suspend fun challenge(
      @Body req: ChallengeRequest?,
    ): Response<ChallengeResponse> {
      reqCount++
      return want
    }
  }

    /*
     * If Play Services is missing from the execution context, PlayIntegrity classes are all
     * null with no class definition and Mockito fails to mock these.
     */
  class TestGoogleApiAvailability(private var want: Int) : GoogleApiAvailability() {
    override fun isGooglePlayServicesAvailable(context: Context): Int {
      return want
    }
  }

  class TestStandardPlayIntegrity(private val want: String) : StandardIntegrityManager {
    var warmupCount = 0

    class TestIntegrityToken(private var want: String) : StandardIntegrityToken() {
      override fun showDialog(
        a: Activity?,
        b: Int,
      ): Task<Int> {
        TODO("Unimplemented!")
      }

      override fun token(): String {
        return want
      }
    }

    class TestStandardTokenProvider(private val want: String) : StandardIntegrityTokenProvider {
      override fun request(req: StandardIntegrityTokenRequest?): Task<StandardIntegrityToken> {
        return Tasks.forResult(TestIntegrityToken(want))
      }
    }

    override fun prepareIntegrityToken(
      req: PrepareIntegrityTokenRequest?
    ): Task<StandardIntegrityTokenProvider> {
      warmupCount++
      return Tasks.forResult(TestStandardTokenProvider(want))
    }
  }

  @Test fun testAttest() {
    val challengeClient = TestChallengeClient(Response.success(sampleChallenge))
    val integrityManager = TestStandardPlayIntegrity(wantToken)

    val detection =
      PlayIntegrityDetection(
        ctx,
        challengeClient,
        packageName,
        idProviders =
          listOf(
            mockBootIdProvider,
            mockDeviceIdProvider,
          ),
        integrityManager = integrityManager,
        googleApiAvailability = TestGoogleApiAvailability(ConnectionResult.SUCCESS),
        clock = MutableFixedClock(wantTimestamp),
      )

    runBlocking {
      detection.warmupWithRequest()
    }

    assertEquals(1, integrityManager.warmupCount)
    assertEquals(1, challengeClient.reqCount)

    runBlocking {
      detection.attest()
    }

    assertEquals(1, integrityManager.warmupCount)
    assertEquals(2, challengeClient.reqCount)

    runBlocking {
      detection.attest()
    }
    assertEquals(1, integrityManager.warmupCount)
    assertEquals(3, challengeClient.reqCount)
  }
}
