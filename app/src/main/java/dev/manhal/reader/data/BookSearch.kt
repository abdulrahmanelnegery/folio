package dev.manhal.reader.data

/** One match inside a book: [unit] is the page (PDF), chapter (EPUB) or 0 (a text file); [start] is the offset inside that unit's text. */
data class SearchHit(val unit: Int, val start: Int, val snippet: String)

/** Search results so far, and how far through the book the search has got (0 to 1). [textFound] is false when nothing readable was seen (for example a scanned PDF). */
data class SearchProgress(val hits: List<SearchHit>, val fraction: Float, val textFound: Boolean)

/** Full-text search over a book split into units. The text of each unit comes from the caller, one at a time, so a big book is never held whole. */
object BookSearch {
    /** Plain text of an HTML chapter: tags and scripts removed, common entities decoded, whitespace collapsed. */
    fun htmlToText(html: String): String {
        var t = html.replace(Regex("(?is)<(script|style)\\b.*?</\\1>"), " ")
        t = t.replace(Regex("(?s)<[^>]*>"), " ")
        t = t.replace(Regex("&#x([0-9a-fA-F]+);")) { String(Character.toChars(it.groupValues[1].toInt(16))) }
        t = t.replace(Regex("&#([0-9]+);")) { String(Character.toChars(it.groupValues[1].toInt())) }
        t = t.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
        return t.replace(Regex("\\s+"), " ").trim()
    }

    /** The 1-based line of [offset] in [text]. */
    fun lineOf(text: String, offset: Int): Int = text.take(offset.coerceIn(0, text.length)).count { it == '\n' } + 1

    /**
     * Runs [query] over [units] (index to text, in book order), keeping at most [perUnit] hits from one unit and [limit] in all.
     * [onProgress] is called after each unit with the hits so far; it also lets the caller stop by throwing a cancellation.
     */
    fun search(units: Sequence<Pair<Int, String>>, total: Int, query: String, limit: Int = 300, perUnit: Int = 5, onProgress: (SearchProgress) -> Unit = {}): List<SearchHit> {
        if (query.isBlank() || total <= 0) return emptyList()
        val hits = ArrayList<SearchHit>()
        var seen = 0
        var textFound = false
        for ((unit, text) in units) {
            seen++
            if (text.isNotBlank()) textFound = true
            if (hits.size < limit) TextSearch.find(text, query, limit = minOf(perUnit, limit - hits.size)).forEach { hits += SearchHit(unit, it.start, it.snippet) }
            onProgress(SearchProgress(hits.toList(), seen.toFloat() / total, textFound))
            if (hits.size >= limit) break
        }
        return hits
    }
}
