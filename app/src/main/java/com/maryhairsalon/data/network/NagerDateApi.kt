package com.maryhairsalon.data.network

import retrofit2.http.GET
import retrofit2.http.Path

interface NagerDateApi {
    @GET("api/v3/PublicHolidays/{year}/{countryCode}")
    suspend fun getPublicHolidays(
        @Path("year") year: Int,
        @Path("countryCode") countryCode: String = "ZA"
    ): List<PublicHoliday>
}

data class PublicHoliday(
    val date: String, // "yyyy-MM-dd"
    val localName: String,
    val name: String
)