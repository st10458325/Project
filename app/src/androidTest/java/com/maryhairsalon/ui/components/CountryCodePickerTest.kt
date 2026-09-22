package com.maryhairsalon.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.maryhairsalon.data.repository.DialCodeOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CountryCodePickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val options = listOf(
        DialCodeOption("South Africa", "ZA", "+27"),
        DialCodeOption("United Kingdom", "GB", "+44")
    )

    @Test
    fun selectingACountryUpdatesTheDisplayedCode() {
        var selected: DialCodeOption? = options.first()

        composeRule.setContent {
            CountryCodePicker(selected = selected, options = options, onSelect = { selected = it })
        }

        composeRule.onNodeWithText("+27 ZA").performClick()
        composeRule.onNodeWithText("+44  United Kingdom").performClick()

        assertEquals("GB", selected?.countryCode)
    }
}
