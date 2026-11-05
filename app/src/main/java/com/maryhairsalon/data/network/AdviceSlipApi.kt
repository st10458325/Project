/**
 * AdviceSlipApi.kt
 *
 * Retrofit API interface for fetching random advice slips from api.adviceslip.com.
 * Uses a cache buster query parameter to prevent aggressive CDN caching.
 */
package com.maryhairsalon.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface AdviceSlipApi {
    // A changing query param busts Advice Slip's aggressive CDN caching,
    // which otherwise tends to return the same advice on every call.
    @GET("advice")
    suspend fun getRandomAdvice(@Query("cb") cacheBuster: Long): AdviceResponse
}

data class AdviceResponse(val slip: AdviceSlip)

// Deliberately only declaring the field we actually use — the API's id
// field has changed shape across versions, and Moshi ignores any JSON
// field not declared here, so leaving it out avoids depending on it.
data class AdviceSlip(val advice: String)
