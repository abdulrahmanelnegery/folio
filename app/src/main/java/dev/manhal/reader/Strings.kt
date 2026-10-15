package dev.manhal.reader

import android.content.res.Resources
import androidx.annotation.StringRes

/** The resources of the language the app is showing; set before the first screen, so every screen and message reads the same language. */
object Strings {
    @Volatile var res: Resources? = null
    @Volatile var arabicDigits: Boolean = false

    /** Lets plain unit tests (which have no Android resources) read the English strings; unused in the app. */
    @Volatile var override: ((Int, Array<out Any?>) -> String)? = null
}

/** A string from the resources, with the Arabic-Indic digits when that choice is on. */
fun s(@StringRes id: Int, vararg args: Any?): String {
    Strings.override?.let { return it(id, args) }
    val r = Strings.res ?: return ""
    val text = if (args.isEmpty()) r.getString(id) else r.getString(id, *args)
    return if (Strings.arabicDigits) toArabicDigits(text) else text
}

fun toArabicDigits(text: String): String {
    val out = StringBuilder(text.length)
    for (c in text) out.append(if (c in '0'..'9') '٠' + (c - '0') else c)
    return out.toString()
}

/** A number or counter as text, in Arabic-Indic digits when that setting is on. */
fun digits(text: String): String = if (Strings.arabicDigits) toArabicDigits(text) else text

/** A plural string; the count is the first format argument. */
fun sp(@androidx.annotation.PluralsRes id: Int, count: Int, vararg args: Any?): String {
    Strings.override?.let { return it(id, arrayOf(count, *args)) }
    val r = Strings.res ?: return ""
    val text = r.getQuantityString(id, count, count, *args)
    return if (Strings.arabicDigits) toArabicDigits(text) else text
}
