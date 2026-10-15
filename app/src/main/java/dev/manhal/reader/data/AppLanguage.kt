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

/** Plain SharedPreferences again, so the language and the digit style are known before the first frame. */
object AppLanguagePrefs {
    private const val FILE = LegacyNames.PREFS_FILE
    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(context: Context): AppLanguage = AppLanguage.parse(prefs(context).getString("app_language", null))
    fun save(context: Context, language: AppLanguage) { prefs(context).edit().putString("app_language", language.name).apply() }
    fun arabicDigits(context: Context): Boolean = prefs(context).getBoolean("arabic_digits", false)
    fun saveArabicDigits(context: Context, on: Boolean) { prefs(context).edit().putBoolean("arabic_digits", on).apply() }
}
