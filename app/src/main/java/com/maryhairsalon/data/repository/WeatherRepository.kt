package com.maryhairsalon.data.repository

import android.util.Log
import com.maryhairsalon.data.network.NetworkModule
import java.time.LocalDate

data class DayForecast(val description: String, val maxTemp: Double, val minTemp: Double)

/**
 * Free, keyless weather lookup for the salon's location. Update
 * SALON_LATITUDE/LONGITUDE if the salon isn't in Cape Town.
 * Open-Meteo's free forecast only covers ~16 days ahead — bookings further
 * out simply get no forecast, which is expected, not an error.
 */
class WeatherRepository {
    companion object {
        private const val SALON_LATITUDE = -33.9249
        private const val SALON_LONGITUDE = 18.4241
    }

    suspend fun getForecastFor(date: LocalDate): DayForecast? {
        val today = LocalDate.now()
        if (date.isBefore(today) || date.isAfter(today.plusDays(15))) return null

        return try {
            val response = NetworkModule.openMeteoApi.getForecast(
                latitude = SALON_LATITUDE,
                longitude = SALON_LONGITUDE,
                startDate = date.toString(),
                endDate = date.toString()
            )
            val daily = response.daily ?: return null
            val index = daily.time.indexOf(date.toString())
            if (index == -1) return null

            DayForecast(
                description = weatherCodeToDescription(daily.weathercode.getOrNull(index) ?: -1),
                maxTemp = daily.temperature_2m_max.getOrNull(index) ?: return null,
                minTemp = daily.temperature_2m_min.getOrNull(index) ?: return null
            )
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Failed to fetch forecast", e)
            null
        }
    }

    private fun weatherCodeToDescription(code: Int): String = when (code) {
        0 -> "Clear sky"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Rain showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Unknown conditions"
    }
}