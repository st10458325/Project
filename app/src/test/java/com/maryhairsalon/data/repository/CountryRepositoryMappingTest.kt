package com.maryhairsalon.data.repository

import com.maryhairsalon.data.network.CountryDto
import com.maryhairsalon.data.network.CountryName
import com.maryhairsalon.data.network.IddInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class CountryRepositoryMappingTest {

    @Test
    fun `builds dial code from root plus first suffix`() {
        val input = listOf(CountryDto(CountryName("South Africa"), "ZA", IddInfo("+2", listOf("7"))))
        assertEquals("+27", mapCountriesToDialCodes(input).first().dialCode)
    }

    @Test
    fun `countries with no idd root are skipped`() {
        val input = listOf(CountryDto(CountryName("Nowhere"), "NW", idd = null))
        assertEquals(emptyList<DialCodeOption>(), mapCountriesToDialCodes(input))
    }

    @Test
    fun `results are sorted alphabetically by country name`() {
        val input = listOf(
            CountryDto(CountryName("Zimbabwe"), "ZW", IddInfo("+263", null)),
            CountryDto(CountryName("Angola"), "AO", IddInfo("+244", null))
        )
        assertEquals(listOf("Angola", "Zimbabwe"), mapCountriesToDialCodes(input).map { it.countryName })
    }
}
