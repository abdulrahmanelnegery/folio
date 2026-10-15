package dev.manhal.reader.data

import androidx.annotation.StringRes
import dev.manhal.reader.R
import dev.manhal.reader.s

/** The theme of the app itself (not the reading themes of the reader): follow the system, or always light or dark. */
enum class AppTheme(@StringRes val labelRes: Int) {
    System(R.string.system), Light(R.string.light), Dark(R.string.dark);

    fun isDark(systemDark: Boolean): Boolean = when (this) { System -> systemDark; Light -> false; Dark -> true }

    companion object {
        fun parse(text: String?): AppTheme = entries.firstOrNull { it.name == text } ?: System
    }
}

val AppTheme.label: String get() = s(labelRes)
