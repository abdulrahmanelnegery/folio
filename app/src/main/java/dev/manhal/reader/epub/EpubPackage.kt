package dev.manhal.reader.epub

/** What an EPUB's package file says: the title, the reading order, and which image is the cover. Paths are relative to the package file's folder. */
data class EpubPackage(val title: String, val author: String, val spine: List<String>, val cover: String?, val folder: String) {
    companion object {
        private val attr = Regex("""([\w:.-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")

        private fun attrs(tag: String): Map<String, String> = attr.findAll(tag).associate { it.groupValues[1].lowercase() to (it.groupValues[2].ifEmpty { it.groupValues[3] }) }

        /** The package file's path inside the zip, from META-INF/container.xml. */
        fun packagePath(containerXml: String): String? =
            Regex("""<rootfile\b[^>]*>""", RegexOption.IGNORE_CASE).find(containerXml)?.let { attrs(it.value)["full-path"] }?.takeIf { it.isNotBlank() }

        fun parse(opfXml: String, opfPath: String): EpubPackage? {
            val items = Regex("""<item\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(opfXml).map { attrs(it.value) }.filter { it["id"] != null && it["href"] != null }.toList()
            val byId = items.associateBy { it.getValue("id") }
            val spine = Regex("""<itemref\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(opfXml)
                .mapNotNull { attrs(it.value)["idref"] }
                .mapNotNull { byId[it] }
                .filter { (it["media-type"] ?: "").let { t -> t.contains("html") || t.contains("xml") } }
                .map { decode(it.getValue("href")) }
                .toList()
            if (spine.isEmpty()) return null
            fun text(tag: String) = Regex("""<(?:dc:)?$tag\b[^>]*>(.*?)</(?:dc:)?$tag>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(opfXml)?.groupValues?.get(1)?.let(::plain)?.trim().orEmpty()
            val coverId = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(opfXml).map { attrs(it.value) }.firstOrNull { it["name"] == "cover" }?.get("content")
            val cover = (items.firstOrNull { (it["properties"] ?: "").split(' ').contains("cover-image") } ?: coverId?.let { byId[it] })?.get("href")?.let(::decode)
            return EpubPackage(text("title"), text("creator"), spine, cover, opfPath.substringBeforeLast('/', ""))
        }

        private fun decode(href: String): String = percentDecode(href.substringBefore('#').replace("&amp;", "&"))

        /** Decodes %XX escapes as UTF-8 (Arabic or accented file names); a stray or malformed % is left as it is, and + stays a plus. */
        internal fun percentDecode(text: String): String {
            if ('%' !in text) return text
            val out = StringBuilder()
            val bytes = java.io.ByteArrayOutputStream()
            fun flush() { if (bytes.size() > 0) { out.append(bytes.toString(Charsets.UTF_8.name())); bytes.reset() } }
            var i = 0
            while (i < text.length) {
                val hex = if (text[i] == '%' && i + 2 < text.length) text.substring(i + 1, i + 3).toIntOrNull(16) else null
                if (hex != null) { bytes.write(hex); i += 3 } else { flush(); out.append(text[i]); i++ }
            }
            flush()
            return out.toString()
        }

        private fun plain(s: String): String = s.replace(Regex("<[^>]*>"), "").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
    }
}
