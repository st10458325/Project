/**
 * HolidayRepository.kt
 *
 * Repository for fetching South African public holidays using NagerDateApi.
 * Used by the booking calendar to highlight public holidays.
 */
package com.maryhairsalon.data.repository

import android.util.Log
import com.maryhairsalon.data.network.NetworkModule
import java.time.LocalDate

/** Free, keyless South African public holiday lookup — replaces the old hardcoded demo unavailable-days list with real data. */
class HolidayRepository {
    suspend fun getHolidays(year: Int): Map<LocalDate, String> = try {
        NetworkModule.nagerDateApi.getPublicHolidays(year)
            .mapNotNull { holiday ->
                runCatching { LocalDate.parse(holiday.date) to holiday.localName }.getOrNull()
            }
            .toMap()
    } catch (e: Exception) {
        Log.e("HolidayRepo", "Failed to fetch public holidays", e)
        emptyMap()
    }
}
