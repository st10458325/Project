/**
 * NetworkModule.kt
 *
 * Centralized singleton providing OkHttpClient, Moshi JSON converter, and Retrofit API instances.
 * Configures network clients for Open-Meteo, NagerDate, RestCountries, and AdviceSlip services.
 */
package com.maryhairsalon.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * One Retrofit instance per external API, sharing a single OkHttp client and
 * Moshi instance. All four APIs here are free and keyless — no auth needed.
 */
object NetworkModule {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun retrofitFor(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    val openMeteoApi: OpenMeteoApi by lazy {
        retrofitFor("https://api.open-meteo.com/").create(OpenMeteoApi::class.java)
    }

    val nagerDateApi: NagerDateApi by lazy {
        retrofitFor("https://date.nager.at/").create(NagerDateApi::class.java)
    }

    val restCountriesApi: RestCountriesApi by lazy {
        retrofitFor("https://restcountries.com/").create(RestCountriesApi::class.java)
    }

    val adviceSlipApi: AdviceSlipApi by lazy {
        retrofitFor("https://api.adviceslip.com/").create(AdviceSlipApi::class.java)
    }
}
