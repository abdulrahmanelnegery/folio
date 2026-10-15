package dev.manhal.reader.data

import dev.manhal.reader.R
import dev.manhal.reader.s
import android.content.Context
import android.net.Uri
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Locale

/** What the file itself says about the document, read straight from it (best effort). */
object PdfInfo {
    private const val WHOLE_FILE_LIMIT = 48L * 1024 * 1024
    private const val CHUNK = 8 * 1024 * 1024

    private val infoKeys get() = listOf(
        "Title" to s(R.string.title),
        "Author" to s(R.string.author),
        "Subject" to s(R.string.subject),
        "Keywords" to s(R.string.keywords),
        "Creator" to s(R.string.created_with),
        "Producer" to s(R.string.pdf_producer),
        "CreationDate" to s(R.string.created),
        "ModDate" to s(R.string.modified),
    )

    fun read(context: Context, uri: Uri): List<Pair<String, String>> {
        val text = runCatching { sample(context, uri) }.getOrNull() ?: return emptyList()
        val out = ArrayList<Pair<String, String>>()
        Regex("%PDF-(\\d\\.\\d)").find(text.take(1024))?.let { out += s(R.string.pdf_version) to it.groupValues[1] }
        for ((key, label) in infoKeys) {
            val value = infoValue(text, key) ?: xmpValue(text, key) ?: continue
            out += label to if (key.endsWith("Date")) prettyDate(value) else value
        }
        if (text.contains("/Encrypt")) out += s(R.string.encrypted) to s(R.string.yes)
        if (text.contains("/Linearized")) out += s(R.string.optimised_for_the_web) to s(R.string.yes)
        return out
    }

    /** The head and tail of the file (the whole file when small) as Latin-1, so bytes survive. */
    private fun sample(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver
        val size = resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        resolver.openInputStream(uri)?.use { input ->
            if (size in 0..WHOLE_FILE_LIMIT) return String(input.readBytes(), Charsets.ISO_8859_1)
            val head = ByteArray(CHUNK)
            val n = input.read(head).coerceAtLeast(0)
            val tail = ByteArray(CHUNK)
            if (size > 0) input.skip(size - n - CHUNK)
            val m = input.read(tail).coerceAtLeast(0)
            return String(head, 0, n, Charsets.ISO_8859_1) + String(tail, 0, m, Charsets.ISO_8859_1)
        }
        return null
    }

    private fun infoValue(text: String, key: String): String? {
        val m = Regex("/$key\\s*(\\((?:\\\\.|[^\\\\)])*\\)|<[0-9A-Fa-f\\s]*>)").findAll(text).lastOrNull() ?: return null
        val raw = m.groupValues[1]
        val bytes = if (raw.startsWith("<")) hexBytes(raw.substring(1, raw.length - 1)) else literalBytes(raw.substring(1, raw.length - 1))
        return decode(bytes).trim().ifEmpty { null }
    }

    private fun xmpValue(text: String, key: String): String? {
        val pattern = when (key) {
            "Title" -> "<dc:title>.*?<rdf:li[^>]*>(.*?)</rdf:li>"
            "Author" -> "<dc:creator>.*?<rdf:li[^>]*>(.*?)</rdf:li>"
            "Subject" -> "<dc:description>.*?<rdf:li[^>]*>(.*?)</rdf:li>"
            "Producer" -> "<pdf:Producer>(.*?)</pdf:Producer>"
            "Creator" -> "<xmp:CreatorTool>(.*?)</xmp:CreatorTool>"
            "CreationDate" -> "<xmp:CreateDate>(.*?)</xmp:CreateDate>"
            "ModDate" -> "<xmp:ModifyDate>(.*?)</xmp:ModifyDate>"
            else -> return null
        }
        val raw = Regex(pattern, RegexOption.DOT_MATCHES_ALL).find(text)?.groupValues?.get(1)?.trim().orEmpty()
        if (raw.isEmpty()) return null
        // XMP is UTF-8; the text was read as Latin-1, so restore the bytes.
        return String(raw.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8).ifEmpty { null }
    }

    private fun hexBytes(hex: String): ByteArray {
        val clean = hex.filter { !it.isWhitespace() }.let { if (it.length % 2 == 1) it + "0" else it }
        return ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private fun literalBytes(s: String): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c != '\\' || i + 1 >= s.length) { out.write(c.code and 0xFF); i++; continue }
            val n = s[i + 1]
            when {
                n == 'n' -> { out.write(10); i += 2 }
                n == 'r' -> { out.write(13); i += 2 }
                n == 't' -> { out.write(9); i += 2 }
                n in '0'..'7' -> {
                    var j = i + 1
                    var v = 0
                    while (j < s.length && j < i + 4 && s[j] in '0'..'7') { v = v * 8 + (s[j] - '0'); j++ }
                    out.write(v and 0xFF); i = j
                }
                else -> { out.write(n.code and 0xFF); i += 2 }
            }
        }
        return out.toByteArray()
    }

    private fun decode(b: ByteArray): String = when {
        b.size >= 2 && b[0] == 0xFE.toByte() && b[1] == 0xFF.toByte() -> String(b, 2, b.size - 2, Charsets.UTF_16BE)
        b.size >= 2 && b[0] == 0xFF.toByte() && b[1] == 0xFE.toByte() -> String(b, 2, b.size - 2, Charsets.UTF_16LE)
        b.size >= 3 && b[0] == 0xEF.toByte() && b[1] == 0xBB.toByte() && b[2] == 0xBF.toByte() -> String(b, 3, b.size - 3, Charsets.UTF_8)
        else -> String(b, Charset.forName("windows-1252"))
    }

    /** "D:20240131120000+02'00'" or an ISO date become "31 Jan 2024". */
    private fun prettyDate(raw: String): String {
        val digits = Regex("(\\d{4})-?(\\d{2})-?(\\d{2})").find(raw.removePrefix("D:")) ?: return raw
        val (y, m, d) = digits.destructured
        return runCatching {
            val parsed = SimpleDateFormat("yyyyMMdd", Locale.US).parse("$y$m$d")!!
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(parsed)
        }.getOrDefault(raw)
    }
}
