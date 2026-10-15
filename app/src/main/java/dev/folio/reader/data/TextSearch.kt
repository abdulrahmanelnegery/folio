package dev.folio.reader.data

/** Finds words inside a book's text, ignoring case and, for Arabic, diacritics and the spelling variants of alef, ya and ta marbuta. */
object TextSearch {
    data class Hit(val start: Int, val end: Int, val snippet: String)

    /** Folds [text] so that spelling variants compare equal; the result has the same length, so positions map back. */
    fun fold(text: String): String {
        val out = StringBuilder(text.length)
        for (c in text) out.append(
            when (c) {
                'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                'ى' -> 'ي'
                'ة' -> 'ه'
                'ـ', in 'ً'..'ٟ', 'ٰ' -> '\u0000'
                else -> c.lowercaseChar()
            },
        )
        return out.toString()
    }

    /** Folded text with the dropped marks removed, and for each kept character its index in the original. */
    private fun foldedWithMap(text: String): Pair<String, IntArray> {
        val folded = fold(text)
        val sb = StringBuilder()
        val map = ArrayList<Int>(folded.length)
        for (i in folded.indices) if (folded[i] != '\u0000') { sb.append(folded[i]); map += i }
        return sb.toString() to map.toIntArray()
    }

    /** All matches of [query] in [text], in order, at most [limit]; each with about [context] characters around it. */
    fun find(text: String, query: String, limit: Int = 500, context: Int = 40): List<Hit> {
        val (q, _) = foldedWithMap(query.trim())
        if (q.isEmpty()) return emptyList()
        val (hay, map) = foldedWithMap(text)
        val hits = ArrayList<Hit>()
        var from = 0
        while (hits.size < limit) {
            val at = hay.indexOf(q, from)
            if (at < 0) break
            val start = map[at]
            val end = map[at + q.length - 1] + 1
            val a = (start - context).coerceAtLeast(0)
            val b = (end + context).coerceAtMost(text.length)
            hits += Hit(start, end, text.substring(a, b).replace(Regex("\\s+"), " ").trim())
            from = at + q.length
        }
        return hits
    }
}
