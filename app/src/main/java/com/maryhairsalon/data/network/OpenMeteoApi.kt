package com.maryhairsalon.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto",
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): OpenMeteoResponse
}

data class OpenMeteoResponse(val daily: DailyForecast?)

data class DailyForecast(
    val time: List<String> = emptyList(),
    val weathercode: List<Int> = emptyList(),
    val temperature_2m_max: List<Double> = emptyList(),
    val temperature_2m_min: List<Double> = emptyList()
)