package dev.folio.reader.data

/** How a text file is shown: decides the highlighting and whether it is rendered (Markdown). */
enum class TextLang { Plain, Markdown, Json, Yaml, Docker, Xml, Toml, Ini, Shell, Csv, Code }

object FileTypes {
    private val comic = setOf("cbz", "cbr", "cb7", "cbt")
    private val markdown = setOf("md", "markdown", "mdx", "mkd")
    private val json = setOf("json", "jsonc", "json5", "geojson", "ipynb")
    private val yaml = setOf("yml", "yaml")
    private val xml = setOf("xml", "html", "htm", "xhtml", "svg", "plist", "xsd", "xsl", "rss", "atom")
    private val toml = setOf("toml")
    private val ini = setOf("ini", "cfg", "conf", "config", "properties", "env", "editorconfig", "gitconfig", "desktop", "service")
    private val shell = setOf("sh", "bash", "zsh", "fish", "ps1", "bat", "cmd")
    private val csv = setOf("csv", "tsv")
    private val code = setOf(
        "js", "mjs", "cjs", "ts", "tsx", "jsx", "py", "rb", "php", "java", "kt", "kts", "go", "rs", "c", "h", "cc", "cpp", "hpp", "cs", "swift",
        "sql", "gradle", "groovy", "lua", "pl", "r", "dart", "scala", "css", "scss", "less", "vue", "svelte", "tf", "hcl", "proto", "graphql",
        "gql", "cmake", "mk", "makefile", "tex", "diff", "patch",
    )
    private val plain = setOf("txt", "text", "log", "nfo", "rst", "adoc", "gitignore", "dockerignore", "lock", "srt", "vtt", "license", "readme")
    private val dockerNames = setOf("dockerfile", "containerfile")

    fun extension(name: String): String = name.substringAfterLast('.', "").lowercase()

    private fun stem(name: String) = name.substringBeforeLast('.').lowercase()

    /** The kind of document, from the file name; null when the name says nothing (the caller may sniff the content). */
    fun kindOf(name: String, mime: String? = null): Kind? {
        val ext = extension(name)
        val lower = name.lowercase()
        return when {
            ext == "pdf" || mime == "application/pdf" -> Kind.Pdf
            ext == "epub" || mime == "application/epub+zip" -> Kind.Epub
            ext in comic || mime?.startsWith("application/vnd.comicbook") == true -> Kind.Comic
            textLang(name) != null -> Kind.Text
            mime?.startsWith("text/") == true || mime == "application/json" || mime?.contains("yaml") == true || mime?.contains("xml") == true -> Kind.Text
            lower in dockerNames || stem(name) in dockerNames -> Kind.Text
            else -> null
        }
    }

    /** The language of a text file by name, or null when it is not a known text name. */
    fun textLang(name: String): TextLang? {
        val ext = extension(name)
        val lower = name.lowercase()
        return when {
            lower in dockerNames || stem(name) in dockerNames || lower.startsWith("dockerfile") || lower.endsWith(".dockerfile") -> TextLang.Docker
            lower == "makefile" || lower == "gemfile" || lower == "rakefile" || lower == "vagrantfile" || lower == "jenkinsfile" -> TextLang.Code
            ext in markdown -> TextLang.Markdown
            ext in json -> TextLang.Json
            ext in yaml -> TextLang.Yaml
            ext in xml -> TextLang.Xml
            ext in toml -> TextLang.Toml
            ext in ini -> TextLang.Ini
            ext in shell -> TextLang.Shell
            ext in csv -> TextLang.Csv
            ext in code -> TextLang.Code
            ext in plain -> TextLang.Plain
            else -> null
        }
    }

    /** A short badge for a cover tile: "MD", "JSON", "EPUB". */
    fun badge(name: String, kind: Kind): String = when (kind) {
        Kind.Pdf -> "PDF"
        Kind.Epub -> "EPUB"
        Kind.Comic -> "COMIC"
        Kind.Text -> when (textLang(name)) {
            TextLang.Docker -> "DOCKER"
            null -> extension(name).uppercase().ifEmpty { "TXT" }.take(5)
            else -> extension(name).uppercase().ifEmpty { "TXT" }.take(5)
        }
    }

    /** True when the first bytes look like text (no NUL, mostly printable). */
    fun looksLikeText(sample: ByteArray, length: Int): Boolean {
        if (length == 0) return true
        var odd = 0
        for (i in 0 until length) {
            val b = sample[i].toInt() and 0xFF
            if (b == 0) return false
            if (b < 9 || (b in 14..31)) odd++
        }
        return odd * 20 < length
    }
}
