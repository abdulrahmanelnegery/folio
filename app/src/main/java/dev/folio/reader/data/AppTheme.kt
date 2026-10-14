package dev.folio.reader.data

import androidx.annotation.StringRes
import dev.folio.reader.R
import dev.folio.reader.s
import android.content.Context

/** The theme of the app itself (not the reading themes of the reader): follow the system, or always light or dark. */
enum class AppTheme(@StringRes val labelRes: Int) {
    System(R.string.system), Light(R.string.light), Dark(R.string.dark);

    fun isDark(systemDark: Boolean): Boolean = when (this) { System -> systemDark; Light -> false; Dark -> true }

    companion object {
        fun parse(text: String?): AppTheme = entries.firstOrNull { it.name == text } ?: System
    }
}

val AppTheme.label: String get() = s(labelRes)

/** Kept in plain SharedPreferences, which load before the first frame, so the right theme shows with no flash. */
object AppThemePrefs {
    private const val FILE = "rukn-ui"
    private const val KEY = "app_theme"

    fun load(context: Context): AppTheme = AppTheme.parse(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null))

    fun save(context: Context, theme: AppTheme) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, theme.name).apply()
    }
}
