package dev.manhal.reader

import java.io.File

/** Gives plain unit tests the English strings: R.string ids are matched to names by reflection and read from the values folder. */
object EnglishStrings {
    private val res = File("src/main/res/values")
    private fun xml(file: String) = File(res, file).readText()

    private val strings: Map<String, String> by lazy {
        Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL).findAll(xml("strings.xml"))
            .associate { it.groupValues[1] to it.groupValues[2].replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n").removeSurrounding("\"") }
    }
    private val plurals: Map<String, Map<String, String>> by lazy {
        Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL).findAll(xml("plurals.xml")).associate { p ->
            p.groupValues[1] to Regex("""<item quantity="([^"]+)">(.*?)</item>""").findAll(p.groupValues[2]).associate { it.groupValues[1] to it.groupValues[2] }
        }
    }
    private fun nameOf(cls: Class<*>, id: Int) = cls.fields.first { it.getInt(null) == id }.name

    fun install() {
        Strings.override = { id, args ->
            val isPlural = runCatching { nameOf(R.plurals::class.java, id) }.getOrNull()
            if (isPlural != null) {
                val count = (args[0] as Number).toInt()
                format(plurals.getValue(isPlural)[if (count == 1) "one" else "other"] ?: plurals.getValue(isPlural).getValue("other"), args)
            } else format(strings.getValue(nameOf(R.string::class.java, id)), args)
        }
    }
    private fun format(text: String, args: Array<out Any?>): String =
        if (args.isEmpty()) text else String.format(text.replace("%%", "%%"), *args)
}
