package com.squareup.tinysweeper.network

import com.squareup.lando.service.challenge.Challenge.ChallengeRequest
import com.squareup.lando.service.challenge.Challenge.ChallengeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ChallengeClient {
  @POST("/1.0/lando/challenge")
  suspend fun challenge(
    @Body req: ChallengeRequest?
  ): Response<ChallengeResponse>
}
