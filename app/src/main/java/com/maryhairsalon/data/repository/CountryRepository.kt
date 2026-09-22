package com.maryhairsalon.data.repository

import android.util.Log
import com.maryhairsalon.data.network.CountryDto
import com.maryhairsalon.data.network.NetworkModule

data class DialCodeOption(val countryName: String, val countryCode: String, val dialCode: String)

internal fun mapCountriesToDialCodes(countries: List<CountryDto>): List<DialCodeOption> =
    countries.mapNotNull { country ->
        val root = country.idd?.root
        if (root.isNullOrBlank()) return@mapNotNull null
        val suffix = country.idd.suffixes?.firstOrNull().orEmpty()
        DialCodeOption(
            countryName = country.name.common,
            countryCode = country.cca2,
            dialCode = root + suffix
        )
    }.distinctBy { it.dialCode + it.countryCode }
     .sortedBy { it.countryName }

/**
 * Free, keyless country dial-code lookup for the phone number field.
 * Falls back to a small hardcoded list if the network call fails, so the
 * picker is never left empty. Cached in memory after the first successful
 * fetch — no need to refetch every time someone opens Signup.
 */
class CountryRepository {
    companion object {
        private var cached: List<DialCodeOption>? = null

        private val FALLBACK = listOf(
            DialCodeOption("South Africa", "ZA", "+27"),
            DialCodeOption("United Kingdom", "GB", "+44"),
            DialCodeOption("United States", "US", "+1"),
            DialCodeOption("Nigeria", "NG", "+234"),
            DialCodeOption("Zimbabwe", "ZW", "+263"),
            DialCodeOption("Kenya", "KE", "+254"),
            DialCodeOption("India", "IN", "+91"),
            DialCodeOption("Australia", "AU", "+61")
        )
    }

    suspend fun getDialCodes(): List<DialCodeOption> {
        cached?.let { return it }

        return try {
            val options = mapCountriesToDialCodes(NetworkModule.restCountriesApi.getAllCountries())

            if (options.isEmpty()) FALLBACK else options.also { cached = it }
        } catch (e: Exception) {
            Log.e("CountryRepo", "Failed to fetch countries, using fallback", e)
            FALLBACK
        }
    }
}