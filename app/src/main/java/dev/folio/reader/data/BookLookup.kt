package dev.folio.reader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** What an online catalogue knows about a book; any part may be empty. */
data class BookMeta(
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val year: String = "",
    val pages: Int = 0,
    val isbn: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val workKey: String = "",
    val source: String = "",
)

/** Looks books up in Open Library (no account or key needed). Only text you typed is sent. */
object BookLookup {
    private val json = Json { ignoreUnknownKeys = true }
    private const val BASE = "https://openlibrary.org"

    /** Every way of asking, on both catalogues at once, merged into one list without repeats. */
    suspend fun search(query: String): List<BookMeta> = coroutineScope {
        val q = query.trim()
        if (q.isEmpty()) return@coroutineScope emptyList()
        val enc = URLEncoder.encode(q, "UTF-8")
        val digits = q.filter { it.isDigit() || it == 'X' || it == 'x' }
        val isIsbn = digits.length in setOf(10, 13) && q.all { it.isDigit() || it in "-xX " }
        val fields = "key,title,author_name,publisher,first_publish_year,number_of_pages_median,isbn,cover_i"
        val ol = "$BASE/search.json?limit=12&fields=$fields&"
        val gb = "https://www.googleapis.com/books/v1/volumes?maxResults=12&printType=books&q="
        val calls = buildList<Pair<String, (String) -> List<BookMeta>>> {
            if (isIsbn) {
                add("$ol" + "isbn=$digits" to ::parse)
                add(gb + "isbn:$digits" to ::parseGoogle)
            } else {
                add("$ol" + "q=$enc" to ::parse)
                add("$ol" + "title=$enc" to ::parse)
                add("$ol" + "author=$enc" to ::parse)
                add(gb + enc to ::parseGoogle)
                add(gb + "intitle:$enc" to ::parseGoogle)
                add(gb + "inauthor:$enc" to ::parseGoogle)
            }
        }
        val lists = calls.map { (url, read) -> async(Dispatchers.IO) { runCatching { read(get(url)) }.getOrDefault(emptyList()) } }.awaitAll()
        merge(lists)
    }

    /** The long description lives on the work record, so it is fetched only for the result you open. */
    suspend fun describe(meta: BookMeta): BookMeta = withContext(Dispatchers.IO) {
        if (meta.workKey.isEmpty() || meta.description.isNotEmpty()) return@withContext meta
        val text = runCatching { get("$BASE${meta.workKey}.json") }.getOrNull() ?: return@withContext meta
        val d = runCatching { json.parseToJsonElement(text).jsonObject["description"] }.getOrNull()
        val value = when (d) {
            is JsonPrimitive -> d.contentOrNull
            is JsonObject -> d["value"]?.jsonPrimitive?.contentOrNull
            else -> null
        }.orEmpty()
        meta.copy(description = value.trim())
    }

    suspend fun cover(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { open(url).inputStream.use { it.readBytes() }.takeIf { it.size > 1000 && it.size < 8_000_000 } }.getOrNull()
    }

    internal fun parse(body: String): List<BookMeta> {
        val docs = json.parseToJsonElement(body).jsonObject["docs"]?.jsonArray ?: return emptyList()
        return docs.mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            val title = o["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val cover = o["cover_i"]?.jsonPrimitive?.intOrNull
            BookMeta(
                title = title,
                author = o.strings("author_name").take(3).joinToString(", "),
                publisher = o.strings("publisher").firstOrNull().orEmpty(),
                year = o["first_publish_year"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                pages = o["number_of_pages_median"]?.jsonPrimitive?.intOrNull ?: 0,
                isbn = o.strings("isbn").firstOrNull { it.length == 13 } ?: o.strings("isbn").firstOrNull().orEmpty(),
                coverUrl = if (cover != null && cover > 0) "https://covers.openlibrary.org/b/id/$cover-L.jpg" else "",
                workKey = o["key"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                source = "Open Library",
            )
        }
    }

    internal fun parseGoogle(body: String): List<BookMeta> {
        val items = json.parseToJsonElement(body).jsonObject["items"]?.jsonArray ?: return emptyList()
        return items.mapNotNull { e ->
            val v = (e as? JsonObject)?.get("volumeInfo") as? JsonObject ?: return@mapNotNull null
            val title = listOfNotNull(v["title"]?.jsonPrimitive?.contentOrNull, v["subtitle"]?.jsonPrimitive?.contentOrNull).joinToString(": ").ifBlank { return@mapNotNull null }
            val ids = (v["industryIdentifiers"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.get("identifier")?.jsonPrimitive?.contentOrNull }.orEmpty()
            val thumb = ((v["imageLinks"] as? JsonObject)?.let { it["thumbnail"] ?: it["smallThumbnail"] } as? JsonPrimitive)?.contentOrNull
            BookMeta(
                title = title,
                author = v.strings("authors").take(3).joinToString(", "),
                publisher = v["publisher"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                year = v["publishedDate"]?.jsonPrimitive?.contentOrNull.orEmpty().take(4),
                pages = v["pageCount"]?.jsonPrimitive?.intOrNull ?: 0,
                isbn = ids.firstOrNull { it.length == 13 } ?: ids.firstOrNull().orEmpty(),
                description = v["description"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                coverUrl = thumb?.replace("http://", "https://")?.replace("&edge=curl", "")?.replace("zoom=1", "zoom=0").orEmpty(),
                source = "Google Books",
            )
        }
    }

    /** The same book from several answers becomes one entry; blanks are filled from the others. Fuller entries come first. */
    internal fun merge(lists: List<List<BookMeta>>): List<BookMeta> {
        fun key(m: BookMeta) = m.isbn.ifEmpty { (m.title.lowercase().filter { it.isLetterOrDigit() } + "|" + m.author.lowercase().filter { it.isLetterOrDigit() }.take(8)) }
        val out = LinkedHashMap<String, BookMeta>()
        lists.flatten().forEach { m ->
            val k = key(m)
            val old = out[k]
            out[k] = if (old == null) m else old.copy(
                author = old.author.ifEmpty { m.author }, publisher = old.publisher.ifEmpty { m.publisher }, year = old.year.ifEmpty { m.year },
                pages = if (old.pages > 0) old.pages else m.pages, isbn = old.isbn.ifEmpty { m.isbn },
                description = old.description.ifEmpty { m.description }, coverUrl = old.coverUrl.ifEmpty { m.coverUrl },
                workKey = old.workKey.ifEmpty { m.workKey }, source = if (old.source == m.source) old.source else "Open Library + Google Books",
            )
        }
        return out.values.sortedByDescending { listOf(it.author, it.publisher, it.year, it.coverUrl, it.description).count(String::isNotEmpty) + if (it.pages > 0) 1 else 0 }.take(24)
    }

    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonElement)?.jsonPrimitive?.contentOrNull }.orEmpty()

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "Folio (github.com/abdulrahmanelnegery/folio)")
        }

    private fun get(url: String): String = open(url).inputStream.bufferedReader().use { it.readText() }
}
