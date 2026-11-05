/**
 * RestCountriesApi.kt
 *
 * Retrofit API interface for retrieving country details and international calling codes from restcountries.com.
 */
package com.maryhairsalon.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface RestCountriesApi {
    @GET("v3.1/all")
    suspend fun getAllCountries(
        @Query("fields") fields: String = "name,cca2,idd"
    ): List<CountryDto>
}

data class CountryDto(val name: CountryName, val cca2: String, val idd: IddInfo?)
data class CountryName(val common: String)
data class IddInfo(val root: String?, val suffixes: List<String>?)
