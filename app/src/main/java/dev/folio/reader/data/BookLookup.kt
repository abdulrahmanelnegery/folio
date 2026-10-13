package dev.folio.reader.data

import kotlinx.coroutines.Dispatchers
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
)

/** Looks books up in Open Library (no account or key needed). Only text you typed is sent. */
object BookLookup {
    private val json = Json { ignoreUnknownKeys = true }
    private const val BASE = "https://openlibrary.org"

    suspend fun search(query: String): List<BookMeta> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        val digits = q.filter { it.isDigit() || it == 'X' || it == 'x' }
        val param = if (digits.length in setOf(10, 13) && q.all { it.isDigit() || it in "-xX " }) "isbn=$digits" else "q=${URLEncoder.encode(q, "UTF-8")}"
        val fields = "key,title,author_name,publisher,first_publish_year,number_of_pages_median,isbn,cover_i"
        val body = get("$BASE/search.json?$param&limit=8&fields=$fields")
        parse(body)
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
            )
        }
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
