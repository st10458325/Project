/**
 * AdviceRepository.kt
 *
 * Repository for fetching random advice tips using AdviceSlipApi.
 * Catches network exceptions gracefully so UI card components fail silently.
 */
package com.maryhairsalon.data.repository

import android.util.Log
import com.maryhairsalon.data.network.NetworkModule

/** Free, keyless "tip of the day" text — purely decorative, so failures just hide the card rather than showing an error. */
class AdviceRepository {
    suspend fun getRandomAdvice(): String? = try {
        NetworkModule.adviceSlipApi.getRandomAdvice(System.currentTimeMillis()).slip.advice
    } catch (e: Exception) {
        Log.e("AdviceRepo", "Failed to fetch advice", e)
        null
    }
}
