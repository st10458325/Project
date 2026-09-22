package com.maryhairsalon.locale

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocaleManagerTest {

    @Test
    fun settingAppLocaleUpdatesCurrentLanguage() {
        LocaleManager.setAppLocale(LocaleManager.LANGUAGE_AFRIKAANS)
        assertEquals(LocaleManager.LANGUAGE_AFRIKAANS, LocaleManager.getCurrentLanguage())

        LocaleManager.setAppLocale(LocaleManager.LANGUAGE_ZULU)
        assertEquals(LocaleManager.LANGUAGE_ZULU, LocaleManager.getCurrentLanguage())

        // Reset back to English
        LocaleManager.setAppLocale(LocaleManager.LANGUAGE_ENGLISH)
        assertEquals(LocaleManager.LANGUAGE_ENGLISH, LocaleManager.getCurrentLanguage())
    }
}
