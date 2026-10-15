package dev.manhal.reader

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The English and Arabic strings must never drift apart: same keys, same placeholders, same plural names. */
class TranslationsTest {
    private val res = File("src/main/res")

    private fun names(file: String): Map<String, String> {
        val text = File(res, file).readText()
        return Regex("""<(string|plurals) name="([^"]+)"[^>]*>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text).associate { "${it.groupValues[1]}:${it.groupValues[2]}" to it.groupValues[3] }
    }

    private fun placeholders(text: String) = Regex("""%\d\$[sd]""").findAll(text).map { it.value }.sorted().toList()

    @Test fun everyEnglishKeyHasAnArabicOneAndViceVersa() {
        val en = names("values/strings.xml") + names("values/plurals.xml")
        val ar = names("values-ar/strings.xml") + names("values-ar/plurals.xml")
        assertEquals("missing in Arabic", emptySet<String>(), en.keys - ar.keys)
        assertEquals("only in Arabic", emptySet<String>(), ar.keys - en.keys)
    }

    @Test fun placeholdersMatchInBothLanguages() {
        val en = names("values/strings.xml")
        val ar = names("values-ar/strings.xml")
        for ((key, text) in en) assertEquals("placeholders of $key", placeholders(text), placeholders(ar.getValue(key)))
    }

    @Test fun arabicStringsAreArabic() {
        val ar = names("values-ar/strings.xml")
        assertTrue(ar.getValue("string:about").any { it in '؀'..'ۿ' })
    }

    @Test fun arabicDigitsConvertOnlyDigits() {
        assertEquals("٠١٢٣ abc ٤٥", toArabicDigits("0123 abc 45"))
    }
}
