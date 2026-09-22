package com.maryhairsalon.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Wraps AndroidX's per-app language API. Calling setAppLocale automatically
 * recreates any foreground activity to apply the new locale — no manual
 * activity.recreate() needed. Persistence across restarts is handled by
 * AppCompatDelegate itself (the system's per-app language setting on API 33+,
 * AppCompat's own storage below that — both wired up just by having the
 * appcompat dependency, no manifest service to add by hand).
 */
object LocaleManager {
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_AFRIKAANS = "af"
    const val LANGUAGE_ZULU = "zu"

    val supportedLanguages = listOf(LANGUAGE_ENGLISH, LANGUAGE_AFRIKAANS, LANGUAGE_ZULU)

    fun setAppLocale(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
    }

    fun getCurrentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) LANGUAGE_ENGLISH else (locales[0]?.language ?: LANGUAGE_ENGLISH)
    }
}