package dev.manhal.reader.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** The language of the app itself: follow the system, or always English or Arabic. */
enum class AppLanguage(val tag: String?) {
    System(null), English("en"), Arabic("ar");

    /** The context in the chosen language; the system choice leaves it alone (Android 13 and later also honour the per-app language setting). */
    fun wrap(base: Context): Context {
        val tag = tag ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    companion object {
        fun parse(text: String?): AppLanguage = entries.firstOrNull { it.name == text } ?: System
    }
}
